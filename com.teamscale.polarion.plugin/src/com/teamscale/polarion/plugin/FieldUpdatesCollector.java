package com.teamscale.polarion.plugin;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObject;
import com.teamscale.polarion.plugin.model.LinkBundle;
import com.teamscale.polarion.plugin.model.LinkDirection;
import com.teamscale.polarion.plugin.model.LinkFieldDiff;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemFieldDiff;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.CastUtils;
import com.teamscale.polarion.plugin.utils.CollectionsAndEnumsUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is dedicated to process field changes and deal with backward links. Used by {@link
 * WorkItemUpdatesCollector}
 */
public class FieldUpdatesCollector {

  /**
   * This is to generate changes in the json result related to backward links, since Polarion
   * doesn't return a diff/change associated with the opposite end of the link when a link changes
   * (added/removed). The key of this map is the workItemId to receive the change
   */
  private Map<String, List<LinkBundle>> backwardLinksTobeAdded;

  /**
   * If empty, no work item links should be included. We expect role IDs (not role names). Invalid
   * (not recognized) link role IDs will be ignored. If all link roles are invalid, the request will
   * be processed as if no linkRoles were requested (as if this field was empty).
   */
  private final String[] includeLinkRoles;

  public FieldUpdatesCollector(String[] includeLinkRoles) {
    backwardLinksTobeAdded = new HashMap<String, List<LinkBundle>>();
    this.includeLinkRoles = includeLinkRoles;
  }

  /**
   * Helper method that will collect field changes (to be included in a WorkItemChange object) based
   * on the diff created at {@link #collectWorkItemChanges(String, List, IDiffManager, int)}
   */
  public WorkItemChange collectFieldChanges(
      String workItemId, IFieldDiff[] fieldDiffs, String revision) {

    if (fieldDiffs == null || fieldDiffs.length == 0) {
      return null;
    }

    List<WorkItemFieldDiff> fieldChanges = new ArrayList<>();

    for (IFieldDiff fieldDiff : fieldDiffs) {
      if (fieldDiff.isCollection()) {
        fieldChanges.addAll(collectFieldDiffAsCollection(workItemId, revision, fieldDiff));
      } else {
        WorkItemFieldDiff fieldChange =
            new WorkItemFieldDiff(
                fieldDiff.getFieldName(),
                CastUtils.castFieldValueToString(fieldDiff.getBefore()),
                CastUtils.castFieldValueToString(fieldDiff.getAfter()));
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
    List<WorkItemFieldDiff> fieldChanges = new ArrayList<>();
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
   * Note: for custom fields, Polarion API behaves differently. The returned collection is not convertible to
   * a list of IPObjects. The collection comes as untyped List (ArrayList) and typically the internal elements are
   * of type EnumOption (as custom field types are typically defined as enumerations on Polarion admin settings).
   * */
  private List<WorkItemFieldDiff> collectFieldDiffAsCollection(
      Collection addedOrRemovedItems,
      String workItemId,
      String workItemChangeRevision,
      IFieldDiff fieldDiff,
      boolean isAdded) {

    List<WorkItemFieldDiff> fieldChanges = new ArrayList<>();
    // We check if the collection is hyperlink list first since they're not
    // convertible into IPObjectList. So, we treat them separately.
    if (CollectionsAndEnumsUtils.isCollectionHyperlinkStructList(addedOrRemovedItems)) {
      // Polarion API does not offer fieldDiff.getId() that's why we get getFieldName()
      // Upon some testing, getFieldName actually returns the field id.
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      if (isAdded) {
        fieldChange.setElementsAdded(
            CollectionsAndEnumsUtils.castHyperlinksToStrList(addedOrRemovedItems));
      } else {
        fieldChange.setElementsRemoved(
            CollectionsAndEnumsUtils.castHyperlinksToStrList(addedOrRemovedItems));
      }
      fieldChanges.add(fieldChange);
      // Then we check if they're ILiknedWorkItemStruct, because, again,
      // Polarion treats those 'struct' objects differently thank regular
      // IPObjects
    } else if (includeLinkRoles != null
        && CollectionsAndEnumsUtils.isCollectionLinkedWorkItemStructList(addedOrRemovedItems)) {
      if (fieldDiff.getFieldName().equals(CastUtils.LINKED_WORK_ITEMS_FIELD_NAME)) {
        Collection<ILinkedWorkItemStruct> links =
            (Collection<ILinkedWorkItemStruct>) addedOrRemovedItems;
        links.forEach(
            linkStruct -> {
              ILinkRoleOpt linkRole = linkStruct.getLinkRole();
              createLinkFieldChange(
                  fieldChanges,
                  workItemId,
                  workItemChangeRevision,
                  fieldDiff,
                  linkRole,
                  linkStruct,
                  isAdded);
            });
      }
    } else if (CollectionsAndEnumsUtils.isCollectionApprovalStructList(addedOrRemovedItems)) {
      // If the collection is a list of ApprovalStruct, we also treat them specifically
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      if (isAdded) {
        fieldChange.setElementsAdded(
            CollectionsAndEnumsUtils.castApprovalsToStrList(addedOrRemovedItems));
      } else {
        fieldChange.setElementsRemoved(
            CollectionsAndEnumsUtils.castApprovalsToStrList(addedOrRemovedItems));
      }
      fieldChanges.add(fieldChange);
    } else if (CastUtils.isUntypedListWithEnumOptions(addedOrRemovedItems)) {
      // This if block catches the case when changes are made on custom field values.
      // For custom fields Polarion returns an untyped List (ArrayList) and typically the internal
      // elements are of type EnumOption (as custom field types are typically defined as
      // enumerations on Polarion admin settings).
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      if (isAdded) {
        fieldChange.setElementsAdded(
            CastUtils.castUntypedListToStrList((List<?>) addedOrRemovedItems));
      } else {
        fieldChange.setElementsRemoved(
            CastUtils.castUntypedListToStrList((List<?>) addedOrRemovedItems));
      }
      fieldChanges.add(fieldChange);
    } else if (!CollectionsAndEnumsUtils.isCollectionLinkedWorkItemStructList(
        addedOrRemovedItems)) {
      WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName());
      try {
        if (isAdded) {
          fieldChange.setElementsAdded(
              CastUtils.castCollectionToStrList((List<IPObject>) addedOrRemovedItems));
        } else {
          fieldChange.setElementsRemoved(
              CastUtils.castCollectionToStrList((List<IPObject>) addedOrRemovedItems));
        }
      } catch (ClassCastException ex) {
        // For now, when an added/removed element/value is not among the ones we're supposed
        // to support in Teamscale, we simply add as an empty string array.
        // Alternatively, we could ignore the field as a change
        // (skip the field from the json output)
        if (isAdded) {
          fieldChange.setElementsAdded(new ArrayList<>());
        } else {
          fieldChange.setElementsRemoved(new ArrayList<>());
        }
      }
      fieldChanges.add(fieldChange);
    }

    return fieldChanges;
  }

  /**
   * Helper method for {@link #collectFieldDiffAsCollection(Collection, String, String, IFieldDiff,
   * boolean)}. This method first checks if there's a match between the linkRole and list of all
   * link roles expected by the request. If yes, it creates the link field diff and calls the {@link
   * #updateOppositeLinksMap(String, String, ILinkedWorkItemStruct, boolean)}. It returns null if
   * there isn't a match between the link role and the expected link roles from the request.
   */
  private void createLinkFieldChange(
      List<WorkItemFieldDiff> fieldChanges,
      String workItemId,
      String workItemChangeRevision,
      IFieldDiff fieldDiff,
      ILinkRoleOpt linkRole,
      ILinkedWorkItemStruct linkStruct,
      boolean isAdded) {

    if (Arrays.stream(includeLinkRoles).anyMatch(linkRole.getId()::equals)) {
      // Note: the link direction is always an out link (we don't need to check) since
      // Polarion does not generate a fieldDiff for IN links.
      WorkItemFieldDiff fieldChange =
          new LinkFieldDiff(
              fieldDiff.getFieldName(), linkRole.getId(), linkRole.getName(), LinkDirection.OUT);
      List<String> single = new ArrayList<>(1);
      single.add(linkStruct.getLinkedItem().getId());
      if (isAdded) {
        fieldChange.setElementsAdded(single);
      } else {
        fieldChange.setElementsRemoved(single);
      }
      fieldChanges.add(fieldChange);
      updateOppositeLinksMap(workItemId, workItemChangeRevision, linkStruct, isAdded);
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
   * @param isAdded is true if this a list of added links, otherwise these are removed links *
   */
  public void updateOppositeLinksMap(
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
      List<LinkBundle> newLinkBundles = new ArrayList<>();
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
    if (linkBundles == null) {
      return false;
    }

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
                            CastUtils.LINKED_WORK_ITEMS_FIELD_NAME,
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

    if (workItemChanges == null) {
      return null;
    }

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
      if (fieldChangeEntry.getFieldName().equals(CastUtils.LINKED_WORK_ITEMS_FIELD_NAME)
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
