package com.teamscale.polarion.plugin;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumFactory;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.TypedEnumerationFactory;
import com.polarion.platform.security.PermissionDeniedException;
import com.polarion.platform.service.repository.AccessDeniedException;
import com.polarion.platform.service.repository.ResourceException;
import com.polarion.subterra.base.data.model.TypeFactory;
import com.teamscale.polarion.plugin.model.Response;
import com.teamscale.polarion.plugin.model.ResponseType;
import com.teamscale.polarion.plugin.model.UpdateType;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.PluginLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

  private ResponseType responseType;

  private final PluginLogger logger = new PluginLogger();

  private final ITrackerService trackerService =
      (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);

  private IModule module;

  /**
   * If empty, no work item links should be included. For the values, we expect role names since 
   * this is the format utilized in the Teamscale configuration
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
   * This provides O(1) access when, at the end, when we need to send a list of processed item IDs
   */
  private Map<String, WorkItemForJson> allItemsToSend;

  /** Time limit to stop analyzing new items - triggers a partial response */
  private static final int TIME_THRESHOLD =
      Integer.getInteger("com.teamscale.polarion.plugin.request-time-threshold", 15)
          * 1000; // milliseconds

  /**
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
      throws ServletException, IOException {

    String[] clientKnownIds = readRequestBody(req);
    if (clientKnownIds == null) {
      logger.error("Error attempting to read request body.");
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    // We assume a complete response unless it's close to timeout then we turn it into partial.
    responseType = ResponseType.COMPLETE;

    String lastUpdateStr = req.getParameter("lastUpdate");
    String endRevisionStr = req.getParameter("endRevision");

    if (!processRevisionNumbers(lastUpdateStr, endRevisionStr)) {
      String msg = "Invalid revision numbers. Review lastUpdate and" + " endRevision parameters.";
      logger.error(msg);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      return;
    }

    workItemTypes = req.getParameterValues("includedWorkItemTypes");
    includeCustomFields = req.getParameterValues("includedWorkItemCustomFields");
    includeLinkRoles = req.getParameterValues("includedWorkItemLinkRoles");

    String projId = (String) req.getAttribute("project");
    String spaceId = (String) req.getAttribute("space");
    String docId = (String) req.getAttribute("document");

    try {
      allItemsToSend = new HashMap<>();

      Collection<String> allValidItemsLatest =
          retrieveChanges(projId, spaceId, docId, clientKnownIds);

      if (allValidItemsLatest == null) {
        logger.error("Invalid combination of projectId/folderId/documentId");
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
      } else {
        sendResponse(res, allValidItemsLatest);
        logger.info("Successful response sent");
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

  /** Returns null if an error occurs, otherwise returns a populated or empty string array */
  private String[] readRequestBody(final HttpServletRequest request) throws IOException {
    String[] knownIds = null;
    StringBuilder jsonBody = new StringBuilder();
    String line;
    try {
      BufferedReader reader = request.getReader();
      line = reader.readLine();
      while (line != null) {
        jsonBody.append(line);
        line = reader.readLine();
      }
    } catch (IOException e) {
      return knownIds;
    }
    if (jsonBody.length() == 0) {
      return new String[] {};
    }

    Gson gson = new Gson();
    knownIds = gson.fromJson(jsonBody.toString(), String[].class);
    return knownIds;
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

    int latestRevInPolarion =
        Integer.valueOf(trackerService.getDataService().getLastStorageRevision().getName());
    if (endRevisionStr == null) {
      // process all the way to the latest by default if endRevision is not passed in
      endRevision = latestRevInPolarion;
    } else if (!validateRevisionNumberString(endRevisionStr)) {
      return false;
    } else {
      // We don't take an endRevision that's greater than the latest in Polarion at the moment
      endRevision = Math.min(Integer.parseInt(endRevisionStr), latestRevInPolarion);
    }

    return (lastUpdate < endRevision);
  }

  private void sendResponse(HttpServletResponse resp, Collection<String> allValidItems)
      throws ServletException, IOException {

    final long timeBefore = System.currentTimeMillis();

    Gson gson = new Gson();

    String endRevisionStr;
    if (endRevision == Integer.MAX_VALUE) {
      endRevisionStr = "HEAD";
    } else {
      endRevisionStr = String.valueOf(endRevision);
    }
    if (responseType.equals(ResponseType.PARTIAL)) {
      // If it's a PARTIAL response then it doesn't make sense to send all item ids since the only
      // purpose for it is the diff check on the client. Client should only do this check after a
      // complete
      allValidItems = null;
    }
    Response response =
        new Response(
            allValidItems,
            allItemsToSend.keySet(),
            allItemsToSend.values(),
            responseType,
            String.valueOf(lastUpdate + 1),
            endRevisionStr);

    String jsonResult = gson.toJson(response);
    resp.setContentType("application/json");
    PrintWriter out = resp.getWriter();
    out.print(jsonResult);

    long timeAfter = System.currentTimeMillis();
    logger.debug(
        " Json serialization and response sent. Execution time (ms): " + (timeAfter - timeBefore));
  }

  /**
   * Polarion does not provide a prepared statement or SQL query builder API. Therefore, to prevent
   * SQL injection issues or any unexpected behavior, we check if the variables passed to this query
   * are valid. See what we do in the following method: {@link
   * WorkItemUpdatesServlet#validateParameters(String, String, String)}
   *
   * <p>Besides, Polarion internally maintains a configurable set of invalid SQL commands for
   * security reasons. See the following answer posted in the community forum: Since Polarion 22R2,
   * there is the system configuration property com.polarion.platform.sql.invalidCommands If you
   * don't configure it, the SQL query is not executed if the query contains one of the following
   * default commands: "ABORT_SESSION", "ARRAY_GET", "CARDINALITY", "ARRAY_CONTAINS", "ARRAY_CAT",
   * "ARRAY_APPEND", "ARRAY_MAX_CARDINALITY", "TRIM_ARRAY", "ARRAY_SLICE", "AUTOCOMMIT",
   * "CANCEL_SESSION", "CASEWHEN", "COALESCE", "CONVERT", "CURRVAL", "CSVWRITE", "CURRENT_SCHEMA",
   * "CURRENT_CATALOG", "DATABASE_PATH", "DATA_TYPE_SQL", "DB_OBJECT_ID", "DB_OBJECT_SQL", "DECODE",
   * "DISK_SPACE_USED", "SIGNAL", "ESTIMATED_ENVELOPE", "FILE_READ", "FILE_WRITE", "GREATEST",
   * "LEAST", "LOCK_MODE", "LOCK_TIMEOUT", "MEMORY_FREE", "MEMORY_USED", "NEXTVAL", "NULLIF",
   * "NVL2", "READONLY", "ROWNUM", "SESSION_ID", "SET", "TRANSACTION_ID", "TRUNCATE_VALUE",
   * "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_USER", "H2VERSION"
   *
   * <p>Link to the <a
   * href="https://community.sw.siemens.com/s/question/0D54O000087hf0wSAA/validate-sql-queries-before-running-them">thread</a>
   */
  private String buildSqlQuery(String projId, String spaceId, String docId) {

    if (!validateParameters(projId, spaceId, docId)) {
      return null;
    }

    StringBuilder sqlQuery = new StringBuilder("select * from WORKITEM WI ");
    sqlQuery.append("inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI ");
    sqlQuery.append("inner join MODULE M on WI.FK_URI_MODULE = M.C_URI ");
    sqlQuery.append("where P.C_ID = '" + projId + "'");
    sqlQuery.append(" and M.C_ID = '" + docId + "'");
    sqlQuery.append(" and M.C_MODULEFOLDER = '" + spaceId + "'");
    sqlQuery.append(generateWorkItemTypesAndClause());
    // Note: We do not add a clause to select WIs after a given revision as we still
    // want to return a list of all WIs for the client to do delete control logic.
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
  private Collection<String> retrieveChanges(
      String projId, String spaceId, String docId, String[] clientKnownIds)
      throws ResourceException {

    final long timeBefore = System.currentTimeMillis();

    String sqlQuery = buildSqlQuery(projId, spaceId, docId);

    if (sqlQuery == null || sqlQuery.isEmpty()) {
      return null;
    }

    IDataService dataService = trackerService.getDataService();

    final List<String> allValidItemIdsLatest = new ArrayList<>();

    IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);

    long timeAfter = System.currentTimeMillis();
    logger.debug("Finished sql query. Execution time (ms): " + (timeAfter - timeBefore));

    WorkItemUpdatesCollector workItemUpdatesCollector =
        new WorkItemUpdatesCollector(
            lastUpdate, endRevision, includeCustomFields, includeLinkRoles);

    boolean closing = false;

    for (int i = 0; i < workItems.size() && !closing; i++) {
      IWorkItem workItem = workItems.get(i);

      timeAfter = System.currentTimeMillis();
      if ((timeAfter - timeBefore) >= TIME_THRESHOLD) {
        closing = true;
        responseType = ResponseType.PARTIAL;
      }

      // Only check history if workItem is not in client's known list and
      // if there were changes after lastUpdate and if response is not closing
      if (!closing
          && Arrays.stream(clientKnownIds).noneMatch(workItem.getId()::equals)
          && Integer.valueOf(workItem.getLastRevision()) > lastUpdate) {

        WorkItemForJson workItemForJson;

        // This is because WIs moved to the recycle bin are still in the Polarion WI table we query
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
      allValidItemIdsLatest.add(workItem.getId());
    }

    timeAfter = System.currentTimeMillis();
    logger.debug("Ended history processing. Execution time (ms): " + (timeAfter - timeBefore));

    return allValidItemIdsLatest;
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
    WorkItemForJson item =
        new WorkItemForJson(workItem.getId(), workItem.getUri().toString(), UpdateType.DELETED);
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

      logger.debug("Attempting to read projectID: " + projObj.getId());

      return true;

    } catch (UnresolvableObjectException exception) {
      logger.error("Not possible to resolve project with id: " + projectId, exception);
      return false;
    }
  }

  private boolean validateSpaceId(String projId, String spaceId) {
    if (trackerService.getFolderManager().existFolder(projId, spaceId)) {
      logger.debug("Attempting to read folder: " + spaceId);
      return true;
    }
    logger.error("Not possible to find folder with id: " + spaceId);
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
        logger.debug("Attempting to read document: " + docId);
        return true;
      }
    }
    logger.error("Not possible to find document with id: " + docId);
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

    IEnumeration linkRolesEnum;
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
    if (allLinkRoles == null || allLinkRoles.isEmpty()) {
    	// if there aren't link roles set up then we cannot validate the requested linkRoles
        return false;
    }
    Set<String> allLinkRolesStrSet = new HashSet<String>();
    allLinkRoles.stream().forEach(linkRole -> { 
    		 allLinkRolesStrSet.add(linkRole.getName());
    		 String oppositeName = linkRole.getProperty("oppositeName");
    		 if (oppositeName != null && !oppositeName.isEmpty()) {
    				 allLinkRolesStrSet.add(oppositeName);
    		 }
    });
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
}
