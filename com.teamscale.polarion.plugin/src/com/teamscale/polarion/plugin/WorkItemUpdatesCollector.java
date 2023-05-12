package com.teamscale.polarion.plugin;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.service.repository.ResourceException;
import com.teamscale.polarion.plugin.model.LinkBundle;
import com.teamscale.polarion.plugin.model.LinkDirection;
import com.teamscale.polarion.plugin.model.LinkFieldDiff;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemFieldDiff;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final Map<String, ILinkRoleOpt> linkNamesMap = new HashMap<String, ILinkRoleOpt>();

  /**
   * This is to generate changes in the json result related to backward links, since Polarion
   * doesn't return a diff/change associated with the opposite end of the link when a link changes
   * (added/removed). The key of this map is the workItemId to receive the change
   */
  private Map<String, List<LinkBundle>> backwardLinksTobeAdded;

  public WorkItemUpdatesCollector(
      int lastUpdate, int endRevision, String[] includeCustomFields, String[] includeLinkRoles) {

    this.lastUpdate = lastUpdate;
    this.endRevision = endRevision;
    this.includeCustomFields = includeCustomFields;
    this.includeLinkRoles = includeLinkRoles;
    backwardLinksTobeAdded = new HashMap<String, List<LinkBundle>>();
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
        if (Integer.valueOf(workItemHistory.get(0).getRevision()) <= endRevision)
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

        if (lastUpdateIndex < 0) return null;

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
    if (lastUpdate <= 0) return 0;

    int lastItemRevision =
        Integer.valueOf(workItemHistory.get(workItemHistory.size() - 1).getRevision());
    // Short circuit: don't need to search if lastUpdate points to the last version of the item
    if (lastItemRevision == lastUpdate) return workItemHistory.size() - 1;

    int firstItemVersion = Integer.valueOf(workItemHistory.get(0).getRevision());
    // Short circuit: don't search if all changes are before lastUpdate or after endRevision
    if (lastItemRevision < lastUpdate || firstItemVersion > endRevision) return -1;

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

    if (fieldDiffs == null || fieldDiffs.length == 0) return null;

    List<WorkItemFieldDiff> fieldChanges = new ArrayList<WorkItemFieldDiff>();

    for (IFieldDiff fieldDiff : fieldDiffs) {
      if (fieldDiff.isCollection()) {
        fieldChanges.addAll(collectFieldDiffAsCollection(workItemId, revision, fieldDiff));
      } else {
        WorkItemFieldDiff fieldChange =
            new WorkItemFieldDiff(
                fieldDiff.getFieldName(),
                Utils.castFieldValueToString(fieldDiff.getBefore()),
                Utils.castFieldValueToString(fieldDiff.getAfter()));
        fieldChanges.add(fieldChange);
      }
    }
    return new WorkItemChange(revision, fieldChanges);
  }

  /**
   * Helper method that will process field changes when the field is a collection type of field.
   * Rather then having a 'before value' replaced by an 'after value' Polarion returns these fields
   * with elements that were added or removed in the revision. *
   */
  private List<WorkItemFieldDiff> collectFieldDiffAsCollection(
      String workItemId, String workItemChangeRevision, IFieldDiff fieldDiff) {

    // Polarion returns unparameterized Collections for these two methods
    Collection added = fieldDiff.getAdded();
    Collection removed = fieldDiff.getRemoved();
    List<WorkItemFieldDiff> fieldChanges = new ArrayList<WorkItemFieldDiff>();
    if (added != null && !added.isEmpty()) {
      fieldChanges.addAll(
          collectFieldDiffAsCollection(added, workItemId, workItemChangeRevision, fieldDiff, true));
    }
    if (removed != null && !removed.isEmpty()) {
      fieldChanges.addAll(
          collectFieldDiffAsCollection(
              removed, workItemId, workItemChangeRevision, fieldDiff, false));
    }
    return fieldChanges;
  }

  /**
   * This is an overload of the helper {@see #collectFieldDiffAsCollection(String, WorkItemChange, IFieldDiff)
   * that applies to either added or removed items from fields that are treated as collections in Polarion
   * */
  private List<WorkItemFieldDiff> collectFieldDiffAsCollection(
      Collection addedOrRemovedItems,
      String workItemId,
      String workItemChangeRevision,
      IFieldDiff fieldDiff,
      boolean isAdded) {

    List<WorkItemFieldDiff> fieldChanges = new ArrayList<WorkItemFieldDiff>();
    // We check if the collection is hyperlink list first since they're not
    // convertible into IPObjectList. So, we treat them separately.
    if (Utils.isCollectionHyperlinkStructList(addedOrRemovedItems)) {
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      if (isAdded) {
        fieldChange.setElementsAdded(Utils.castHyperlinksToStrList(addedOrRemovedItems));
      } else {
        fieldChange.setElementsRemoved(Utils.castHyperlinksToStrList(addedOrRemovedItems));
      }
      fieldChanges.add(fieldChange);
      // Then we check if they're ILiknedWorkItemStruct, because, again,
      // Polarion treats those 'struct' objects differently thank regular
      // IPObjects
    } else if (includeLinkRoles != null
        && Utils.isCollectionLinkedWorkItemStructList(addedOrRemovedItems)) {
      if (fieldDiff.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)) {
        Collection<ILinkedWorkItemStruct> links =
            (Collection<ILinkedWorkItemStruct>) addedOrRemovedItems;
        links.forEach(
            linkStruct -> {
              ILinkRoleOpt linkRole = linkStruct.getLinkRole();
              if (Arrays.stream(includeLinkRoles).anyMatch(linkRole.getId()::equals)) {
                WorkItemFieldDiff fieldChange =
                    new LinkFieldDiff(
                        fieldDiff.getFieldName(),
                        linkRole.getId(),
                        linkRole.getName(),
                        LinkDirection.OUT);
                List<String> single = new ArrayList<String>(1);
                single.add(linkStruct.getLinkedItem().getId());
                if (isAdded) {
                  fieldChange.setElementsAdded(single);
                } else {
                  fieldChange.setElementsRemoved(single);
                }
                fieldChanges.add(fieldChange);
                updateOppositeLinksMap(workItemId, workItemChangeRevision, linkStruct, isAdded);
              }
            });
      }
    } else if (Utils.isCollectionApprovalStructList(addedOrRemovedItems)) {
      // If the collection is a list of ApprovalStruct, we also treat them specifically
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      if (isAdded) {
        fieldChange.setElementsAdded(Utils.castApprovalsToStrList(addedOrRemovedItems));
      } else {
        fieldChange.setElementsRemoved(Utils.castApprovalsToStrList(addedOrRemovedItems));
      }
      fieldChanges.add(fieldChange);
    } else if (!Utils.isCollectionLinkedWorkItemStructList(addedOrRemovedItems)) {
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      try {
        if (isAdded) {
          fieldChange.setElementsAdded(
              Utils.castCollectionToStrList((List<IPObject>) addedOrRemovedItems));
        } else {
          fieldChange.setElementsRemoved(
              Utils.castCollectionToStrList((List<IPObject>) addedOrRemovedItems));
        }
      } catch (ClassCastException ex) {
        // For now, when an added/removed element/value is not among the ones we're supposed
        // to support in Teamscale, we simply add as an empty string array.
        // Alternatively, we could ignore the field as a change
        // (skip the field from the json output)
        if (isAdded) {
          fieldChange.setElementsAdded(new ArrayList<String>());
        } else {
          fieldChange.setElementsRemoved(new ArrayList<String>());
        }
      }
      fieldChanges.add(fieldChange);
    }

    return fieldChanges;
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
   * @param isAdded is true if this a list of added links, otherwise these are removed links *
   */
  private void updateOppositeLinksMap(
      String workItemId, String revision, ILinkedWorkItemStruct link, boolean isAdded) {

    // For each link struct, get the WI id, check if there's an entry in the map
    // if there is, check if there's a link of same type, action (added/removed), and revision
    // if there isn't, add an entry to the map fliping the ids (reverse)

    List<LinkBundle> linkBundles = backwardLinksTobeAdded.get(link.getLinkedItem().getId());
    LinkBundle reverse = null;
    if (linkBundles == null) {
      reverse =
          new LinkBundle(
              isAdded,
              new LinkedWorkItem(
                  workItemId,
                  link.getLinkRole().getId(),
                  link.getLinkRole().getOppositeName(),
                  LinkDirection.IN),
              revision);
      List<LinkBundle> newLinkBundles = new ArrayList<LinkBundle>();
      newLinkBundles.add(reverse);
      backwardLinksTobeAdded.put(link.getLinkedItem().getId(), newLinkBundles);
    } else {
      if (!alreadyHasLinkBundle(linkBundles, link, revision, isAdded)) {
        reverse =
            new LinkBundle(
                isAdded,
                new LinkedWorkItem(
                    workItemId,
                    link.getLinkRole().getId(),
                    link.getLinkRole().getOppositeName(),
                    LinkDirection.IN),
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
          && linkBundle.getLinkedWorkItem().getLinkDirection().equals(LinkDirection.IN)
          && linkBundle.getLinkedWorkItem().getId().equals(linkStruct.getLinkedItem().getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * This creates work item opposite links for every existing direct link since Polarion does not
   * provide a clear API for that. The method getLinkedWorkItemsStructsBack from Polarion API
   * returns a collection of ILinkedWorkItemStruct with links roles as "triggered_by" which is not
   * helpful for us to select specific requested links.
   */
  public void createOppositeLinkEntries(final Map<String, WorkItemForJson> allItemsToSend) {
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
                      if (linkedWorkItem.getLinkDirection().equals(LinkDirection.OUT)
                          && allItemsToSend.get(linkedWorkItem.getId()) != null) {

                        ILinkRoleOpt linkRole = linkNamesMap.get(linkedWorkItem.getLinkRoleId());
                        LinkedWorkItem newEntry =
                            new LinkedWorkItem(
                                workItemId,
                                linkRole.getId(),
                                linkRole.getOppositeName(),
                                LinkDirection.IN);
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
  public void createLinkChangesOppositeEntries(final Map<String, WorkItemForJson> allItemsToSend) {
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
      final Collection<WorkItemChange> workItemChanges, LinkBundle linkBundle) {

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
      final WorkItemChange workItemChange, LinkBundle linkBundle) {
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
}
