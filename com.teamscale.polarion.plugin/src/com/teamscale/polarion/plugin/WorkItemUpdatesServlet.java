package com.teamscale.polarion.plugin;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.PermissionDeniedException;
import com.polarion.platform.service.repository.AccessDeniedException;
import com.polarion.platform.service.repository.ResourceException;
import com.polarion.subterra.base.data.model.TypeFactory;
import com.teamscale.polarion.plugin.model.Response;
import com.teamscale.polarion.plugin.model.UpdateType;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.PluginLogger;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is the servlet that represents the endpoint for the Teamscale Polarion plugin. Its main job
 * is to return a json object representing updates on work items of a particular document, in a
 * given folder (space) and project.
 *
 * @author Bruno da Silva
 */
public class WorkItemUpdatesServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final PluginLogger logger = new PluginLogger();

  private final ITrackerService trackerService =
      (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);

  private IModule module;

  /**
   * If empty, no work item links should be included. For the values, we expect role IDs (not role
   * names)
   */
  private String[] includeLinkRoles;

  /** Base revision # for the request */
  private int lastUpdate;

  /** End revision # to indicate the final revision (included) the request is looking for */
  private int endRevision;

  /**
   * List of possible types the result can have. If empty, items of all types should be included.
   */
  private String[] workItemTypes;

  /**
   * List of work item custom fields that should be included in the result. If empty, no custom
   * fields should be present.
   */
  private String[] includeCustomFields;

  /**
   * This is to keep in memory all result objects (type WorkItemsForJson) indexed by WorkItem ID
   * This provides O(1) access when, at the end, we need to go back and feed them with the work
   * items opposite link changes.
   */
  private Map<String, WorkItemForJson> allItemsToSend;

  /**
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
      throws ServletException, IOException {

    String projId = (String) req.getAttribute("project");
    String spaceId = (String) req.getAttribute("space");
    String docId = (String) req.getAttribute("document");

    String lastUpdateStr = req.getParameter("lastUpdate");
    String endRevisionStr = req.getParameter("endRevision");

    workItemTypes = req.getParameterValues("includedWorkItemTypes");

    includeCustomFields = req.getParameterValues("includedWorkItemCustomFields");

    includeLinkRoles = req.getParameterValues("includedWorkItemLinkRoles");

    if (!processRevisionNumbers(lastUpdateStr, endRevisionStr)) {
      String msg = "Invalid revision numbers. Review lastUpdate and" + " endRevision parameters.";
      logger.info(msg);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      return;
    }

    try {
      // To prevent SQL injection issues
      // Check if the request params are valid IDs before putting them into the SQL query
      if (validateParameters(projId, spaceId, docId)) {

        allItemsToSend = new HashMap<>();

        Collection<String> allValidItemsLatest = retrieveChanges(projId, spaceId, docId);
        sendResponse(res, allValidItemsLatest);

        logger.info("Successful response sent");
      } else {
        logger.info("Invalid conbination of projectId/folderId/documentId");
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
      }
    } catch (PermissionDeniedException permissionDenied) {
      logger.error("Permission denied raised by Polarion", permissionDenied);
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (AccessDeniedException accessDenied) {
      logger.error("Access denied raised by Polarion", accessDenied);
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (ResourceException resourceException) {
      logger.error(
          "Cannot fulfill request. Failed to process histoy for WorkItem "
              + resourceException.getResource(),
          resourceException);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * This method is necessary since Polarion redirects a POST request when the original GET request
   * to this servlet is attempted without being authenticated. Polarion redirects the client to a
   * login form. Once the client sends an auth request (which is a POST) and is successfully
   * authenticated, Polarion then redirects the auth post request to this servlet (which we then
   * call the method doGet).
   *
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  private boolean processRevisionNumbers(String lastUpdateStr, String endRevisionStr) {
    if (lastUpdateStr == null) {
      lastUpdateStr = "0"; // process from beginning
    } else if (!validateRevisionNumberString(lastUpdateStr)) {
      return false;
    }
    lastUpdate = Integer.parseInt(lastUpdateStr);

    if (endRevisionStr == null) {
      // process all the way to HEAD
      endRevision = Integer.MAX_VALUE;
    } else if (!validateRevisionNumberString(endRevisionStr)) {
      return false;
    } else {
      endRevision = Integer.parseInt(endRevisionStr);
    }

    return (lastUpdate < endRevision);
  }

  private void sendResponse(HttpServletResponse resp, Collection<String> allValidItems)
      throws ServletException, IOException {
    Gson gson = new Gson();

    Response response = new Response(allValidItems, allItemsToSend.values());

    String jsonResult = gson.toJson(response);
    resp.setContentType("application/json");
    PrintWriter out = resp.getWriter();
    out.print(jsonResult);
  }

  /**
   * To avoid SQL injection issues or any unexpected behavior, check if the variable pieces injected
   * in this query are valid. See what we do in the following method: {@link
   * WorkItemUpdatesServlet#validateParameters(String, String, String)}
   */
  private String buildSqlQuery(String projId, String spaceId, String docId) {

    StringBuilder sqlQuery = new StringBuilder("select * from WORKITEM WI ");
    sqlQuery.append("inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI ");
    sqlQuery.append("inner join MODULE M on WI.FK_URI_MODULE = M.C_URI ");
    sqlQuery.append("where P.C_ID = '" + projId + "'");
    sqlQuery.append(" and M.C_ID = '" + docId + "'");
    sqlQuery.append(" and M.C_MODULEFOLDER = '" + spaceId + "'");
    sqlQuery.append(generateWorkItemTypesAndClause());

    return sqlQuery.toString();
  }

  /** If the return string is blank work items of all types will be included in the query. */
  private String generateWorkItemTypesAndClause() {
    StringBuilder andClause = new StringBuilder("");
    if (workItemTypes != null && workItemTypes.length > 0) {
      andClause.append(" and WI.C_TYPE in (");
      for (int i = 0; i < workItemTypes.length; i++) {
        if (workItemTypes[i] != null && !workItemTypes[i].isBlank()) {
          andClause.append("'" + workItemTypes[i] + "',");
        }
      }
      if (andClause.toString().endsWith(",")) {
        andClause.deleteCharAt(andClause.length() - 1);
      }
      andClause.append(")");
    }
    return andClause.toString();
  }

  /**
   * Based on Polarion documentation, the revision column is INTEGER. In Postgresql, the max integer
   * is the same as the max Java integer, which is the maximum revision number a project can have in
   * Polarion/SVN. For reference:
   * https://almdemo.polarion.com/polarion/sdk/doc/database/FullDBSchema.pdf
   */
  private boolean validateRevisionNumberString(String revision) {
    if (revision != null) {
      try {
        int n = Integer.parseInt(revision);
        return n >= 0;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return false;
  }

  /**
   * This method runs the SQL query and starts processing all the work items returned from the
   * query. Additionally, it returns all work item Ids that are valid in the database at the moment
   * (after lastUpdate). That return will be used to pass this list of Ids to the response. *
   */
  private Collection<String> retrieveChanges(String projId, String spaceId, String docId)
      throws ResourceException {

    String sqlQuery = buildSqlQuery(projId, spaceId, docId);

    IDataService dataService = trackerService.getDataService();

    Collection<String> allValidsItemsLatest = new ArrayList<>();

    long timeBefore = System.currentTimeMillis();

    IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);

    long timeAfter = System.currentTimeMillis();
    logger.info("Finished sql query. Execution time in ms: " + (timeAfter - timeBefore));

    timeBefore = System.currentTimeMillis();

    WorkItemUpdatesCollector workItemUpdatesCollector =
        new WorkItemUpdatesCollector(
            lastUpdate, endRevision, includeCustomFields, includeLinkRoles);

    for (IWorkItem workItem : workItems) {

      // Only check history if there were changes after lastUpdate
      if (Integer.valueOf(workItem.getLastRevision()) > lastUpdate) {

        WorkItemForJson workItemForJson;

        // This is because WIs moved to the recyble bin are still in the Polarion WI table we query
        if (wasMovedToRecycleBin(workItem) && shouldIncludeItemFromRecybleBin(workItem)) {
          workItemForJson = buildDeletedWorkItemForJson(workItem);
        } else {
          workItemForJson = workItemUpdatesCollector.processHistory(workItem, dataService);
        }

        if (workItemForJson != null) {
          allItemsToSend.put(workItem.getId(), workItemForJson);
        }
      }
      // Regardless, add item to the response so the client can do the diff to check for deletions
      allValidsItemsLatest.add(workItem.getId());
    }

    workItemUpdatesCollector.createOppositeLinkEntries(allItemsToSend);
    workItemUpdatesCollector.createLinkChangesOppositeEntries(allItemsToSend);

    timeAfter = System.currentTimeMillis();
    logger.info(
        "Finished processing request. " + "Execution time (ms): " + (timeAfter - timeBefore));

    return allValidsItemsLatest;
  }

  /**
   * In Polarion, WIs in the recycle bin will still come in the SQL query, as in the database level
   * they're still related to the module. However, the following API method excludes them and
   * consider them as items NOT contained in the module. *
   */
  private boolean wasMovedToRecycleBin(IWorkItem workItem) {
    return !module.containsWorkItem(workItem);
  }

  /**
   * Not all items in the recycle bin are supposed to be included in the response. It'll depend on
   * the request lastUpdate revision and endRevision parameters. *
   */
  private boolean shouldIncludeItemFromRecybleBin(IWorkItem workItem) {
    Integer workItemLastRevision = Integer.parseInt(workItem.getLastRevision());

    return (workItemLastRevision > lastUpdate && workItemLastRevision <= endRevision);
  }

  /** Create the work item object as DELETED */
  private WorkItemForJson buildDeletedWorkItemForJson(IWorkItem workItem) {
    WorkItemForJson item = new WorkItemForJson(workItem.getId(), UpdateType.DELETED);
    // Note: Items in the recycle bin can still undergo changes. For instance, if
    // any of their field values change, or their links, or even if their module changes id,
    // it'll generate a new revision and changes will be tracked by Polarion.
    // Therefore, the following revision will either be the revision when item was deleted or the
    // the revision when item was lastly changed while in the recycle bin.
    item.setRevision(workItem.getLastRevision());
    return item;
  }

  /**
   * This method validates the required request path attributes plus the optional includedLinkRoles
   * query parameter.
   */
  private boolean validateParameters(String projectId, String space, String doc) {
    // Needs to be executed in this order. Space validation only runs after projectId is validated.
    // DocId is validated only if projectId and SpaceId are validated.
    // And linkRoles are validated only after document is valid (module is defined).
    return validateProjectId(projectId)
        && validateSpaceId(projectId, space)
        && validateDocumentId(projectId, space, doc)
        && validateLinkRoles();
  }

  private boolean validateProjectId(String projectId) {
    try {
      IProject projObj = trackerService.getProjectsService().getProject(projectId);

      logger.info("Attempting to read projectID: " + projObj.getId());

      return true;

    } catch (UnresolvableObjectException exception) {
      logger.error("Not possible to resolve project with id: " + projectId, exception);
      return false;
    }
  }

  private boolean validateSpaceId(String projId, String spaceId) {
    if (trackerService.getFolderManager().existFolder(projId, spaceId)) {
      logger.info("Attempting to read folder: " + spaceId);
      return true;
    }
    logger.info("Not possible to find folder with id: " + spaceId);
    return false;
  }

  /** This helper method should be called after validating the space (aka folder) */
  private boolean validateDocumentId(String projId, String space, String docId) {
    // Haven't found in the Polarion Java API a straightforward way to validate a docId.
    // That's why the current solution has to select all documents in the given
    // valid space (folder) then loops through the documents to validate the given docId
    IDataService dataService = trackerService.getDataService();
    String query =
        "select * from MODULE M "
            + "inner join PROJECT P on M.FK_URI_PROJECT = P.C_URI "
            + "and P.C_ID = '"
            + projId
            + "' "
            + "and M.C_MODULEFOLDER = '"
            + space
            + "'";
    IPObjectList<IModule> modules = dataService.sqlSearch(query);
    for (IModule module : modules) {
      if (module.getId().equals(docId)) {
        this.module = module;
        logger.info("Attempting to read document: " + docId);
        return true;
      }
    }
    logger.info("Not possible to find document with id: " + docId);
    return false;
  }

  private boolean validateLinkRoles() {
    if (includeLinkRoles == null) {
      // an empty list of linkRoles is valid.
      return true;
    }

    if (module == null) {
      logger.error(
          "Unable to retrieve list of workitem link roles because module is still undefined");
      return false;
    }

    IEnumeration<IEnumOption> linkRolesEnum;
    try {
      // This Polarion method getEnumerationForEnumId returns an unparameterized IEnumeration
      // which we parameterize to IEnumeration<IEnumOption>, that's why we add the try/catch
      linkRolesEnum =
          module
              .getDataSvc()
              .getEnumerationForEnumId(
                  TypeFactory.getInstance().getEnumType("workitem-link-role"),
                  module.getContextId());
    } catch (ClassCastException classCastException) {
      logger.error("Unable to retrieve list of workitem link roles", classCastException);
      return false;
    }

    if (linkRolesEnum == null) {
      logger.error("Unable to retrieve list of workitem link roles");
      return false;
    }

    List<IEnumOption> allLinkRoles = linkRolesEnum.getAllOptions();
    if (allLinkRoles != null && !allLinkRoles.isEmpty()) {
      Set<String> allLinkRolesStrSet =
          allLinkRoles.stream().map(linkRole -> linkRole.getId()).collect(Collectors.toSet());
      String[] newLinkRolesList =
          Arrays.asList(includeLinkRoles).stream()
              .filter(linkRole -> allLinkRolesStrSet.contains(linkRole))
              .toArray(String[]::new);
      if (newLinkRolesList.length > 0) {
        includeLinkRoles = newLinkRolesList;
      } else {
        includeLinkRoles = null;
      }
      return true;
    }
    // if there are not link roles set up then we cannot validate the requested linkRoles
    return false;
  }
}
