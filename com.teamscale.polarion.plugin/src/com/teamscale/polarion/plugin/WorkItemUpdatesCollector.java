package com.teamscale.polarion.plugin;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.service.repository.ResourceException;
import com.teamscale.polarion.plugin.model.LinkDirection;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.CastUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This is a helper class for the main servlet class. This class provides a series of
 * functionalities to process work item history and data generation for the response.
 */
public class WorkItemUpdatesCollector {

  /** Base revision # for the request */
  private final int lastUpdate;

  /** End revision # to indicate the final revision (included) the request is looking for */
  private final int endRevision;

  /**
   * List of work item custom fields that should be included in the result. If empty, no custom
   * fields should be present.
   */
  private final String[] includeCustomFields;

  /**
   * If empty, no work item links should be included. We expect role IDs (not role names). Invalid
   * (not recognized) link role IDs will be ignored. If all link roles are invalid, the request will
   * be processed as if no linkRoles were requested (as if this field was empty).
   */
  private final String[] includeLinkRoles;

  /** This is used to keep a map of linkRoleIds to its in/out link names */
  private final Map<String, ILinkRoleOpt> linkNamesMap = new HashMap<>();

  /** This is a helper obj to help processing field updates including link changes */
  private final FieldUpdatesCollector fieldUpdatesCollector;

  public WorkItemUpdatesCollector(
      int lastUpdate, int endRevision, String[] includeCustomFields, String[] includeLinkRoles) {

    this.lastUpdate = lastUpdate;
    this.endRevision = endRevision;
    this.includeCustomFields = includeCustomFields;
    this.includeLinkRoles = includeLinkRoles;
    fieldUpdatesCollector = new FieldUpdatesCollector(includeLinkRoles);
  }

  /** Main method that will process the work item history based on the parameters in the request */
  public WorkItemForJson processHistory(IWorkItem workItem, IDataService dataService)
      throws ResourceException {

    // Short circuit for performance reasons, don't need to make Polarion fetch history
    if (Integer.valueOf(workItem.getLastRevision()) <= lastUpdate) {
      return null;
    }

    WorkItemForJson workItemForJson = null;

    IPObjectList<IWorkItem> workItemHistory = dataService.getObjectHistory(workItem);
    if (workItemHistory != null) {
      if (workItemHistory.size() == 1 && workItemHistory.get(0) != null) {
        // No changes in history when size == 1 (the WI remains as created)
        // We then return only if the item was created within the revision boundaries of the request
        if (Integer.valueOf(workItemHistory.get(0).getRevision()) <= endRevision) {
          workItemForJson =
              CastUtils.castWorkItem(
                  workItemHistory.get(0), includeCustomFields, includeLinkRoles, linkNamesMap);
        }
      } else if (workItemHistory.size() > 1) {
        /**
         * From Polarion JavaDoc: "The history list is sorted from the oldest (first) to the newest
         * (last)."
         * https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)
         * Then, we get the last one from the history as the current revision
         */
        int lastUpdateIndex = searchIndexWorkItemHistory(workItemHistory);

        if (lastUpdateIndex < 0) {
          return null;
        }

        IDiffManager diffManager = dataService.getDiffManager();
        Collection<WorkItemChange> workItemChanges = new ArrayList<>();
        int endIndex =
            collectWorkItemChanges(
                workItemChanges, workItem.getId(), workItemHistory, diffManager, lastUpdateIndex);
        // Using the endIndex to return the workItem as in the endRevision # (not necessarily the
        // latest version of the item)
        if (endIndex >= 0) {
          workItemForJson =
              CastUtils.castWorkItem(
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
         *
         * <p>Since we're grabbing WIs (and they support history retrieval), as far as we can tell
         * from Polarion docs, this else will never execute. If for some reason it does happen, we
         * throw an a runtime exception. The rationale is: if we cannot return the history of an
         * item, it's better to not fulfill the request and return a server error rather than
         * skipping the item and returning a state that does not necessarily reflect the work item
         * history which can potentially lead to inconsistencies on the client side.
         */
        throw new ResourceException(workItem.getLocation());
      }
    }
    return workItemForJson;
  }

  /** Binary search to cut down the search space since the list is ordered in ascending order */
  private int searchIndexWorkItemHistory(IPObjectList<IWorkItem> workItemHistory) {

    // Short circuit: don't need to binarysearch if you're looking for all history
    if (lastUpdate <= 0) {
      return 0;
    }

    int lastItemRevision =
        Integer.valueOf(workItemHistory.get(workItemHistory.size() - 1).getRevision());
    // Short circuit: don't need to search if lastUpdate points to the last version of the item
    if (lastItemRevision == lastUpdate) {
      return workItemHistory.size() - 1;
    }

    int firstItemVersion = Integer.valueOf(workItemHistory.get(0).getRevision());
    // Short circuit: don't search if all changes are before lastUpdate or after endRevision
    if (lastItemRevision < lastUpdate || firstItemVersion > endRevision) {
      return -1;
    }

    int left = 0;
    int right = workItemHistory.size() - 1;
    int index = -1;

    while (left <= right) {
      int mid = (left + right) / 2;
      if (Integer.valueOf(workItemHistory.get(mid).getRevision()) < lastUpdate) {
        left = mid + 1;
      } else if (Integer.valueOf(workItemHistory.get(mid).getRevision()) == lastUpdate) {
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

    // When the item changed before lastUpdate and after endRevision
    if (Integer.valueOf(workItemHistory.get(index).getRevision()) < lastUpdate
        || Integer.valueOf(workItemHistory.get(index).getRevision()) > endRevision) {
      return -1;
    }

    return (index == 0 ? index : index - 1);
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

    int index = lastUpdateIndex;
    int next = index + 1;
    while (next < workItemHistory.size()
        && Integer.valueOf(workItemHistory.get(next).getRevision()) <= endRevision) {

      if (Integer.valueOf(workItemHistory.get(next).getRevision()) > lastUpdate) {
        IFieldDiff[] fieldDiffs =
            diffManager.generateDiff(
                workItemHistory.get(index), workItemHistory.get(next), new HashSet<>());
        WorkItemChange fieldChangesToAdd =
            fieldUpdatesCollector.collectFieldChanges(
                workItemId, fieldDiffs, workItemHistory.get(next).getRevision());
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
   * This creates work item opposite links for every existing direct link since Polarion does not
   * provide a clear API for that. The method getLinkedWorkItemsStructsBack from Polarion API
   * returns a collection of ILinkedWorkItemStruct with links roles as "triggered_by" which is not
   * helpful for us to select specific requested links.
   */
  public void createOppositeLinkEntries(final Map<String, WorkItemForJson> allItemsToSend) {
    Map<String, List<LinkedWorkItem>> oppositeLinksMap = new HashMap<>();

    /*
     * Problem: if there's a link from item A to item B and item A is not in allItemsToSend
     * due to not being processing (time threshold hit) or being already processed (in the known items list)
     * then the backward link IN type will not be created in B.
     * The link would show when A is processed (OUT) normally. The current solution works when both
     * items are processed in the same request.
     *
     * Fix idea: instead of using the allItemsToSend map, let's use allItemsLatest list.
     * For each workItemId from that list, use the id to query the work item at the request endRevision date.
     * 	Another for loop to go over the item links. For each link:
     * 		If there's an OUT link on that item, get the item linked to that, and check if that item is part of the
     * 			response (if item is in allItemsToSend). If yes, than create the opposite entry on that item.
     * 		Else, go to the next link.
     * */

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
                      if (linkedWorkItem.getLinkDirection().equals(LinkDirection.OUT)
                          && allItemsToSend.get(linkedWorkItem.getId()) != null) {

                        ILinkRoleOpt linkRole = linkNamesMap.get(linkedWorkItem.getLinkRoleId());
                        LinkedWorkItem newEntry =
                            new LinkedWorkItem(
                                workItemId,
                                linkRole.getId(),
                                linkRole.getOppositeName(),
                                LinkDirection.IN);
                        addNewOppositeLinkEntry(oppositeLinksMap, newEntry, linkedWorkItem.getId());
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
   * Helper method for {@link #createOppositeLinkEntries(Map)}. It'll help building the map by
   * creating a opposite link entry (LinkedWorkItem) on an existing workItemId already in the map
   * (adding a new entry to its existing list) or starting a new list with a single element
   */
  private void addNewOppositeLinkEntry(
      Map<String, List<LinkedWorkItem>> oppositeLinksMap,
      LinkedWorkItem newEntry,
      String workItemId) {

    List<LinkedWorkItem> oppositeEntries = oppositeLinksMap.get(workItemId);
    if (oppositeEntries != null) {
      oppositeEntries.add(newEntry);
    } else {
      List<LinkedWorkItem> singleEntryList = new ArrayList<>();
      singleEntryList.add(newEntry);
      oppositeLinksMap.put(workItemId, singleEntryList);
    }
  }

  /**
   * Delegate to the helper {@link FieldUpdatesCollector} since that class is handling the backward
   * links
   */
  public void createLinkChangesOppositeEntries(final Map<String, WorkItemForJson> allItemsToSend) {
    fieldUpdatesCollector.createLinkChangesOppositeEntries(allItemsToSend);
  }
}
