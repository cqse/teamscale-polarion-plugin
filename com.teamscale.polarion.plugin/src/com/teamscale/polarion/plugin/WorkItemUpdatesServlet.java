package com.teamscale.polarion.plugin;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.PermissionDeniedException;
import com.polarion.platform.service.repository.AccessDeniedException;
import com.teamscale.polarion.plugin.model.LinkBundle;
import com.teamscale.polarion.plugin.model.LinkFieldDiff;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.Response;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemFieldDiff;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.PluginLogger;
import com.teamscale.polarion.plugin.utils.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  private PluginLogger logger = new PluginLogger();

  private ITrackerService trackerService =
      (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);

  private IModule module;

  /** If empty, no work item links should be included. We expect role IDs (not role names) */
  private String[] includeLinkRoles;

  /** Base revision # for the request */
  private String lastUpdate;

  /** End revision # to indicate the final revision (included) the request is looking for */
  private String endRevision;

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
   * This is to generate changes in the json result related to backward links, since Polarion
   * doesn't return a diff/change associated with the opposite end of the link when a link changes
   * (added/removed). The key of this map is the workItemId to receive the change
   */
  private Map<String, List<LinkBundle>> backwardLinksTobeAdded;

  /**
   * This is to keep in memory all result objects (type WorkItemsForJson) indexed by WorkItem ID
   * This provides O(1) access when, at the end, we need to go back and feed them with the work
   * items opposite link changes.
   */
  private Map<String, WorkItemForJson> allItemsToSend;

  /** This is used to keep a map of linkRoleIds to its in/out link names */
  private Map<String, ILinkRoleOpt> linkNamesMap = new HashMap<String, ILinkRoleOpt>();

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

    lastUpdate = req.getParameter("lastUpdate");
    endRevision = req.getParameter("endRevision");

    workItemTypes = req.getParameterValues("includedWorkItemTypes");

    includeCustomFields = req.getParameterValues("includedWorkItemCustomFields");

    includeLinkRoles = req.getParameterValues("includedWorkItemLinkRoles");

    if (!processRevisionNumbers()) {
      logger.info("Invalid revision numbers. Review the lastUpdate and" + " endRevision strings.");
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
      return;
    }

    try {
      // To prevent SQL injection issues
      // Check if the request params are valid IDs before putting them into the SQL query
      if (validateParameters(projId, spaceId, docId)) {

        // Resetting these Servlet global maps.
        backwardLinksTobeAdded = new HashMap<String, List<LinkBundle>>();
        allItemsToSend = new HashMap<String, WorkItemForJson>();

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

  private boolean processRevisionNumbers() {
    if (lastUpdate == null) {
      lastUpdate = "0"; // process from beginning
    } else if (!validateRevisionNumberString(lastUpdate)) {
      return false;
    }

    if (endRevision == null) {
      // process all the way to HEAD
      endRevision = String.valueOf(Integer.MAX_VALUE);
    } else if (!validateRevisionNumberString(endRevision)) {
      return false;
    }

    return (Integer.valueOf(lastUpdate) < Integer.valueOf(endRevision));
  }

  private void sendResponse(HttpServletResponse resp, Collection<String> allValidItems)
      throws ServletException, IOException {
    Gson gson = new Gson();

    Response response = new Response();
    response.setAllWorkItemsForJson(allItemsToSend.values());
    response.setAllItemsIds(allValidItems);

    String jsonResult = gson.toJson(response);
    resp.setContentType("application/json");
    PrintWriter out = resp.getWriter();
    out.print(jsonResult);
  }

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
  private Collection<String> retrieveChanges(String projId, String spaceId, String docId) {

    String sqlQuery = buildSqlQuery(projId, spaceId, docId);

    IDataService dataService = trackerService.getDataService();

    Collection<String> allValidsItemsLatest = new ArrayList<String>();

    long timeBefore = System.currentTimeMillis();

    IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);

    long timeAfter = System.currentTimeMillis();
    logger.info("Finished sql query. Execution time in ms: " + (timeAfter - timeBefore));

    timeBefore = System.currentTimeMillis();

    for (IWorkItem workItem : workItems) {

      // Only check history if there were changes after lastUpdate
      if (Integer.valueOf(workItem.getLastRevision()) > Integer.valueOf(lastUpdate)) {

        WorkItemForJson workItemForJson;

        // This is because WIs moved to the recyble bin are still in the Polarion WI table we query
        if (wasMovedToRecycleBin(workItem) && shouldIncludeItemFromRecybleBin(workItem)) {
          workItemForJson = buildDeletedWorkItemForJson(workItem);
        } else {
          workItemForJson = processHistory(workItem, dataService);
        }

        if (workItemForJson != null) {
          allItemsToSend.put(workItem.getId(), workItemForJson);
        }
      }
      // Regardless, add item to the response so the client can do the diff to check for deletions
      allValidsItemsLatest.add(workItem.getId());
    }

    createOppositeLinkEntries();
    createLinkChangesOppositeEntries();

    timeAfter = System.currentTimeMillis();
    logger.info(
        "Finished processing request. " + "Execution time (ms): " + (timeAfter - timeBefore));

    return allValidsItemsLatest;
  }

  /**
   * This creates work item opposite links for every existing direct link since Polarion does not
   * provide a clear API for that. The method getLinkedWorkItemsStructsBack from Polarion API
   * returns a collection of ILinkedWorkItemStruct with links roles as "triggered_by" which is not
   * helpful for us to select specific requested links.
   */
  private void createOppositeLinkEntries() {
    Map<String, List<LinkedWorkItem>> oppositeLinksMap =
        new HashMap<String, List<LinkedWorkItem>>();

    allItemsToSend.forEach(
        (workItemId, workItemForJson) -> {
          // for each entry of the map allItemsToSend, get the list of linkedWorkItems
          // for each linked WI, if the link is OUT and if the linkedWorkItem id is in allItemsId
          // then, add the linkedWorkItem to the oppositeLinksMap mapped to a LinkedWorkItem
          if (workItemForJson != null && workItemForJson.getLinkedWorkItems() != null) {
            workItemForJson
                .getLinkedWorkItems()
                .forEach(
                    linkedWorkItem -> {
                      if (linkedWorkItem.getLinkDirection().equals(Utils.LinkDirection.OUT)
                          && allItemsToSend.get(linkedWorkItem.getId()) != null) {

                        ILinkRoleOpt linkRole = linkNamesMap.get(linkedWorkItem.getLinkRoleId());
                        LinkedWorkItem newEntry =
                            new LinkedWorkItem(
                                workItemId,
                                linkRole.getId(),
                                linkRole.getOppositeName(),
                                Utils.LinkDirection.IN);
                        List<LinkedWorkItem> oppositeEntries =
                            oppositeLinksMap.get(linkedWorkItem.getId());
                        if (oppositeEntries != null) {
                          oppositeEntries.add(newEntry);
                        } else {
                          List<LinkedWorkItem> singleEntryList = new ArrayList<LinkedWorkItem>();
                          singleEntryList.add(newEntry);
                          oppositeLinksMap.put(linkedWorkItem.getId(), singleEntryList);
                        }
                      }
                    });
          }
        });

    // run the oppositeLinksMap and, for each id/key get the value, access the allItemsToSend
    // map, get the value from the id/key and add to the linkedWorkItems list the value
    // from the oppositeLinksMap
    oppositeLinksMap.forEach(
        (workItemId, linkedWorkItems) -> {
          WorkItemForJson workItemForJson = allItemsToSend.get(workItemId);
          if (workItemForJson != null) {
            workItemForJson.addAllLinkedWorkItems(linkedWorkItems);
          }
        });
  }

  /**
   * This method will create the WI changes on the opposite side of the link changes. Note this
   * method is different from {@link #createOppositeLinkEntries()} which creates opposite link
   * entries for each direct link the current version of the work item has, while the following
   * method creates opposite link entries as link changes for the work item changes section.
   */
  private void createLinkChangesOppositeEntries() {
    backwardLinksTobeAdded.forEach(
        (workItemId, linkBundles) -> {
          WorkItemForJson workItemForJson = allItemsToSend.get(workItemId);
          if (workItemForJson != null) {
            Collection<WorkItemChange> workItemChanges = workItemForJson.getWorkItemChanges();
            linkBundles.forEach(
                linkBundle -> {
                  WorkItemChange workItemChange = findRevisionEntry(workItemChanges, linkBundle);
                  if (workItemChange == null) {
                    workItemChange = new WorkItemChange(linkBundle.getRevision());
                    workItemForJson.addWorkItemChange(workItemChange);
                  }
                  WorkItemFieldDiff fieldChangeEntry =
                      findFieldChangeEntry(workItemChange, linkBundle);
                  if (fieldChangeEntry == null) {
                    fieldChangeEntry =
                        new LinkFieldDiff(
                            Utils.LINKED_WORK_ITEMS_FIELD_NAME,
                            linkBundle.getLinkedWorkItem().getLinkRoleId(),
                            linkBundle.getLinkedWorkItem().getLinkRoleName(),
                            linkBundle.getLinkedWorkItem().getLinkDirection());
                    workItemChange.addFieldChange(fieldChangeEntry);
                  }
                  if (linkBundle.isAdded()) {
                    fieldChangeEntry.addElementAdded(linkBundle.getLinkedWorkItem().getId());
                  } else {
                    fieldChangeEntry.addElementRemoved(linkBundle.getLinkedWorkItem().getId());
                  }
                });
          }
        });
  }

  /** Helper method for {@link #createLinkChangesOppositeEntries}. */
  private WorkItemChange findRevisionEntry(
      Collection<WorkItemChange> workItemChanges, LinkBundle linkBundle) {

    if (workItemChanges == null) return null;

    for (WorkItemChange workItemChange : workItemChanges) {
      if (workItemChange.getRevision().equals(linkBundle.getRevision())) {
        return workItemChange;
      }
    }
    return null;
  }

  /** Helper method for {@link #createLinkChangesOppositeEntries}. */
  private WorkItemFieldDiff findFieldChangeEntry(
      WorkItemChange workItemChange, LinkBundle linkBundle) {
    for (WorkItemFieldDiff fieldChangeEntry : workItemChange.getFieldChanges()) {
      if (fieldChangeEntry.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)
          && fieldChangeEntry instanceof LinkFieldDiff
          && ((LinkFieldDiff) fieldChangeEntry)
              .getLinkRoleId()
              .equals(linkBundle.getLinkedWorkItem().getLinkRoleId())) {
        return fieldChangeEntry;
      }
    }
    return null;
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

    return (workItemLastRevision > Integer.parseInt(lastUpdate)
        && workItemLastRevision <= Integer.parseInt(endRevision));
  }

  /** Create the work item object as DELETED */
  private WorkItemForJson buildDeletedWorkItemForJson(IWorkItem workItem) {
    WorkItemForJson item = new WorkItemForJson(workItem.getId(), Utils.UpdateType.DELETED);
    // Note: Items in the recycle bin can still undergo changes. For instance, if
    // any of their field values change, or their links, or even if their module changes id,
    // it'll generate a new revision and changes will be tracked by Polarion.
    // Therefore, the following revision will either be the revision when item was deleted or the
    // the revision when item was lastly changed while in the recycle bin.
    item.setRevision(workItem.getLastRevision());
    return item;
  }

  /** Main method that will process the work item history based on the parameters in the request */
  private WorkItemForJson processHistory(IWorkItem workItem, IDataService dataService) {

    // Short circuit for performance reasons, don't need to make Polarion fetch history
    if (Integer.valueOf(workItem.getLastRevision()) <= Integer.valueOf(lastUpdate)) {
      return null;
    }

    WorkItemForJson workItemForJson = null;

    IPObjectList<IWorkItem> workItemHistory = dataService.getObjectHistory(workItem);
    if (workItemHistory != null) {
      if (workItemHistory.size() == 1 && workItemHistory.get(0) != null) {
        // No changes in history when size == 1 (the WI remains as created)
        // We then return only if the item was created within the revision boundaries of the request
        if (Integer.valueOf(workItemHistory.get(0).getRevision()) <= Integer.valueOf(endRevision))
          workItemForJson =
              Utils.castWorkItem(
                  workItemHistory.get(0), includeCustomFields, includeLinkRoles, linkNamesMap);
      } else if (workItemHistory.size() > 1) {
        /**
         * From Polarion JavaDoc: "The history list is sorted from the oldest (first) to the newest
         * (last)."
         * https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)
         * Then, we get the last one from the history as the current revision
         */
        int lastUpdateIndex = searchIndexWorkItemHistory(workItemHistory);
        IDiffManager diffManager = dataService.getDiffManager();
        Collection<WorkItemChange> workItemChanges = new ArrayList<WorkItemChange>();
        int endIndex =
            collectWorkItemChanges(
                workItemChanges, workItem.getId(), workItemHistory, diffManager, lastUpdateIndex);
        // Using the endIndex to return the workItem as in the endRevision # (not necessarily the
        // latest version of the item)
        if (endIndex >= 0) {
          workItemForJson =
              Utils.castWorkItem(
                  workItemHistory.get(endIndex),
                  includeCustomFields,
                  includeLinkRoles,
                  linkNamesMap);
          workItemForJson.setWorkItemChanges(workItemChanges);
        }
      } else {
        /**
         * No history. Empty list. From Polarion JavaDoc: "An empty list is returned if the object
         * does not support history retrieval."
         * "https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)"
         */
        // Since we're grabbing WIs (and they support history retrieval), as far as we can tell from
        // Polarion docs, this else will never execute.
      }
    }
    return workItemForJson;
  }

  /**
   * Helper method that will collect work item changes (WorkItemChange) based on the diff of each
   * pair of work item versions in a given work item history and then returns the index of the last
   * workItem in the history (considering the endRevision request parameter)
   */
  private int collectWorkItemChanges(
      Collection<WorkItemChange> workItemChanges,
      String workItemId,
      List<IWorkItem> workItemHistory,
      IDiffManager diffManager,
      int lastUpdateIndex) {

    /**
     * Short circuit: no changes to look for. Defensive code, just in case, since we're already
     * checking requested revision numbers here @{link #processRevisionNumbers()} *
     */
    if (lastUpdateIndex < 0) return -1;

    Integer lastUpdateInt = Integer.valueOf(lastUpdate);
    Integer endRevisionInt = Integer.valueOf(endRevision);

    int index = lastUpdateIndex;
    int next = index + 1;
    while (next < workItemHistory.size()
        && Integer.valueOf(workItemHistory.get(next).getRevision()) <= endRevisionInt) {

      if (Integer.valueOf(workItemHistory.get(next).getRevision()) > lastUpdateInt) {
        IFieldDiff[] fieldDiffs =
            diffManager.generateDiff(
                workItemHistory.get(index), workItemHistory.get(next), new HashSet<String>());
        WorkItemChange fieldChangesToAdd =
            collectFieldChanges(workItemId, fieldDiffs, workItemHistory.get(next).getRevision());
        if (fieldChangesToAdd != null) {
          workItemChanges.add(fieldChangesToAdd);
        }
      }
      index++;
      next++;
    }
    return index;
  }

  /**
   * Helper method that will collect field changes (to be included in a WorkItemChange object) based
   * on the diff created at {@link #collectWorkItemChanges(String, List, IDiffManager, int)}
   */
  private WorkItemChange collectFieldChanges(
      String workItemId, IFieldDiff[] fieldDiffs, String revision) {
    WorkItemChange workItemChange = new WorkItemChange(revision);

    if (fieldDiffs == null) return null;

    for (IFieldDiff fieldDiff : fieldDiffs) {
      if (fieldDiff.isCollection()) {
        collectFieldDiffAsCollection(workItemId, workItemChange, fieldDiff);
      } else {
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setFieldValueBefore(Utils.castFieldValueToString(fieldDiff.getBefore()));
        fieldChange.setFieldValueAfter(Utils.castFieldValueToString(fieldDiff.getAfter()));
        workItemChange.addFieldChange(fieldChange);
      }
    }
    return workItemChange;
  }

  /**
   * Helper method that will process field changes when the field is a collection type of field.
   * Rather then having a 'before value' replaced by an 'after value' Polarion returns these fields
   * with elements that were added or removed in the revision. *
   */
  private void collectFieldDiffAsCollection(
      String workItemId, WorkItemChange workItemChange, IFieldDiff fieldDiff) {

    // Polarion returns unparameterized Collections for these two methods
    Collection added = fieldDiff.getAdded();
    Collection removed = fieldDiff.getRemoved();
    if (added != null && !added.isEmpty()) {
      List<WorkItemFieldDiff> fieldChanges = new ArrayList<WorkItemFieldDiff>();
      // We check if the collection is hyperlink list first since they're not
      // convertible into IPObjectList. So, we treat them separately.
      if (Utils.isCollectionHyperlinkStructList(added)) {
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsAdded(Utils.castHyperlinksToStrList(added));
        fieldChanges.add(fieldChange);
        // Then we check if they're ILiknedWorkItemStruc, because, again,
        // Polarion treats those 'struct' objects differently thank regular
        // IPObjects
      } else if (includeLinkRoles != null && Utils.isCollectionLinkedWorkItemStructList(added)) {
        // If the collection is a list of LinkedWorkItemStruct, we also treat them specifically
        if (fieldDiff.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)) {
          Collection<ILinkedWorkItemStruct> links = (Collection<ILinkedWorkItemStruct>) added;
          links.forEach(
              linkStruct -> {
                ILinkRoleOpt linkRole = linkStruct.getLinkRole();
                if (Arrays.stream(includeLinkRoles).anyMatch(linkRole.getId()::equals)) {
                  WorkItemFieldDiff fieldChange =
                      new LinkFieldDiff(
                          fieldDiff.getFieldName(),
                          linkRole.getId(),
                          linkRole.getName(),
                          Utils.LinkDirection.OUT);
                  List<String> singleAdded = new ArrayList<String>(1);
                  singleAdded.add(linkStruct.getLinkedItem().getId());
                  fieldChange.setElementsAdded(singleAdded);
                  fieldChanges.add(fieldChange);
                  updateOppositeLinksMap(
                      workItemId, workItemChange.getRevision(), linkStruct, true);
                }
              });
        }
      } else if (Utils.isCollectionApprovalStructList(added)) {
        // If the collection is a list of ApprovalStruct, we also treat them specifically
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsAdded(Utils.castApprovalsToStrList(added));
        fieldChanges.add(fieldChange);
      } else if (!Utils.isCollectionLinkedWorkItemStructList(added)) {
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        try {
          fieldChange.setElementsAdded(Utils.castCollectionToStrList((List<IPObject>) added));
        } catch (ClassCastException ex) {
          // For now, when an added element/value is not among the ones we're supposed
          // to support in Teamscale, we simply add as an empty string array.
          // Alternatively, we could ignore the field as a change
          // (skip the field from the json output)
          fieldChange.setElementsAdded(new ArrayList<String>());
        }
        fieldChanges.add(fieldChange);
      }
      if (!fieldChanges.isEmpty()) {
        workItemChange.addFieldChanges(fieldChanges);
      }
    }
    if (removed != null && !removed.isEmpty()) {
      List<WorkItemFieldDiff> fieldChanges = new ArrayList<WorkItemFieldDiff>();
      if (Utils.isCollectionHyperlinkStructList(removed)) {
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsRemoved(Utils.castHyperlinksToStrList(removed));
        fieldChanges.add(fieldChange);
      } else if (includeLinkRoles != null && Utils.isCollectionLinkedWorkItemStructList(removed)) {
        if (fieldDiff.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)) {
          Collection<ILinkedWorkItemStruct> links = (Collection<ILinkedWorkItemStruct>) removed;
          links.forEach(
              linkStruct -> {
                ILinkRoleOpt linkRole = linkStruct.getLinkRole();
                if (Arrays.stream(includeLinkRoles).anyMatch(linkRole.getId()::equals)) {
                  WorkItemFieldDiff fieldChange =
                      new LinkFieldDiff(
                          fieldDiff.getFieldName(),
                          linkRole.getId(),
                          linkRole.getName(),
                          Utils.LinkDirection.OUT);
                  List<String> singleAdded = new ArrayList<String>(1);
                  singleAdded.add(linkStruct.getLinkedItem().getId());
                  fieldChange.setElementsRemoved(singleAdded);
                  fieldChanges.add(fieldChange);
                  updateOppositeLinksMap(
                      workItemId, workItemChange.getRevision(), linkStruct, false);
                }
              });
        }
      } else if (Utils.isCollectionApprovalStructList(removed)) {
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsRemoved(Utils.castApprovalsToStrList(removed));
        fieldChanges.add(fieldChange);
      } else if (!Utils.isCollectionLinkedWorkItemStructList(removed)) {
        WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
        try {
          fieldChange.setElementsRemoved(Utils.castCollectionToStrList((List<IPObject>) removed));
        } catch (ClassCastException ex) {
          // For now, when a removed element/value is not among the ones we're supposed
          // to support in Teamscale, we simply add as an empty string array.
          // Alternatively, we could ignore the field as a change
          // (skip the field from the output)
          fieldChange.setElementsRemoved(new ArrayList<String>());
        }
        fieldChanges.add(fieldChange);
      }
      if (!fieldChanges.isEmpty()) {
        workItemChange.addFieldChanges(fieldChanges);
      }
    }
  }

  /**
   * This maintains a map of opposite work item links. This is necessary since Polarion doesn't
   * generate a change/field diff for the opposite end of the link. So, we need to generate those
   * link changes manually for receiving end.
   *
   * @param workItemId represents the link origin. On the map, this id will be the receiver
   *     (reverse)
   * @param revision represents the revision number when the link changed (added/removed)
   * @param link that represent a ILinkWorkItemStruct given by Polarion containing the added/removed
   *     link
   * @param added is true if this a list of added links, otherwise these are removed links *
   */
  private void updateOppositeLinksMap(
      String workItemId, String revision, ILinkedWorkItemStruct link, boolean added) {

    // For each link struct, get the WI id, check if there's an entry in the map
    // if there is, check if there's a link of same type, action (added/removed), and revision
    // if there isn't, add an entry to the map fliping the ids (reverse)

    List<LinkBundle> linkBundles = backwardLinksTobeAdded.get(link.getLinkedItem().getId());
    LinkBundle reverse = null;
    if (linkBundles == null) {
      reverse =
          new LinkBundle(
              added,
              new LinkedWorkItem(
                  workItemId,
                  link.getLinkRole().getId(),
                  link.getLinkRole().getOppositeName(),
                  Utils.LinkDirection.IN),
              revision);
      List<LinkBundle> newLinkBundles = new ArrayList<LinkBundle>();
      newLinkBundles.add(reverse);
      backwardLinksTobeAdded.put(link.getLinkedItem().getId(), newLinkBundles);
    } else {
      if (!alreadyHasLinkBundle(linkBundles, link, revision, added)) {
        reverse =
            new LinkBundle(
                added,
                new LinkedWorkItem(
                    workItemId,
                    link.getLinkRole().getId(),
                    link.getLinkRole().getOppositeName(),
                    Utils.LinkDirection.IN),
                revision);
        linkBundles.add(reverse);
      }
    }
  }

  /**
   * Helper method called by {@link #updateOppositeLinksMap()} to check if a LinkBundle was already
   * created in the map *
   */
  private boolean alreadyHasLinkBundle(
      final List<LinkBundle> linkBundles,
      ILinkedWorkItemStruct linkStruct,
      String revision,
      boolean added) {
    if (linkBundles == null) return false;
    for (LinkBundle linkBundle : linkBundles) {
      if (linkBundle.getRevision().equals(revision)
          && linkBundle.isAdded() == added
          && linkBundle.getLinkedWorkItem().getLinkRoleId().equals(linkStruct.getLinkRole().getId())
          && linkBundle.getLinkedWorkItem().getLinkDirection().equals(Utils.LinkDirection.IN)
          && linkBundle.getLinkedWorkItem().getId().equals(linkStruct.getLinkedItem().getId())) {
        return true;
      }
    }
    return false;
  }

  /** Binary search to cut down the search space since the list is ordered in ascending order* */
  private int searchIndexWorkItemHistory(IPObjectList<IWorkItem> workItemHistory) {

    if (workItemHistory == null) return -1;

    Integer lastUpdateInt = Integer.valueOf(lastUpdate);
    Integer endRevisionInt = Integer.valueOf(endRevision);
    Integer lastItemRevision =
        Integer.valueOf(workItemHistory.get(workItemHistory.size() - 1).getRevision());
    Integer firstItemVersion = Integer.valueOf(workItemHistory.get(0).getRevision());

    // Short circuit: don't need to binarysearch if you're looking for all history
    if (lastUpdateInt <= 0) return 0;

    // Short circuit: don't need to search if lastUpdate points to the last version of the item
    if (lastItemRevision == lastUpdateInt) return workItemHistory.size() - 1;

    // Short circuit: don't search if all changes are before lastUpdate or after endRevision
    if (lastItemRevision < lastUpdateInt || firstItemVersion > endRevisionInt) return -1;

    int left = 0;
    int right = workItemHistory.size() - 1;
    int index = -1;

    while (left <= right) {
      int mid = (left + right) / 2;
      if (Integer.valueOf(workItemHistory.get(mid).getRevision()) < lastUpdateInt) {
        left = mid + 1;
      } else if (Integer.valueOf(workItemHistory.get(mid).getRevision()) == lastUpdateInt) {
        // When all changes came after the lastUpdate and endRevision
        if (firstItemVersion == Integer.valueOf(workItemHistory.get(mid).getRevision())) {
          return -1;
        }
        return mid;
      } else {
        index = mid;
        right = mid - 1;
      }
    }

    // When the item changed before lastUpdate and afterEndRevision
    if (Integer.valueOf(workItemHistory.get(index).getRevision()) < lastUpdateInt
        || Integer.valueOf(workItemHistory.get(index).getRevision()) > endRevisionInt) {
      return -1;
    }

    return (index == 0 ? index : index - 1);
  }

  /**
   * This method validates these required request attributes. Split into three separate helper
   * methods, one for each param.
   */
  private boolean validateParameters(String projectId, String space, String doc) {
    // Needs to be executed in this order. Space validation only runs after projectId is validated.
    // DocId is validated only if projectId and SpaceId are validated.
    return validateProjectId(projectId)
        && validateSpaceId(projectId, space)
        && validateDocumentId(projectId, space, doc);
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
}
