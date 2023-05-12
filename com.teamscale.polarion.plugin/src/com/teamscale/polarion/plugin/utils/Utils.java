package com.teamscale.polarion.plugin.utils;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.ICategory;
import com.polarion.alm.tracker.model.IComment;
import com.polarion.alm.tracker.model.IHyperlinkStruct;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestSteps;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.subterra.base.location.ILocation;
import com.teamscale.polarion.plugin.model.LinkDirection;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.UpdateType;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {

  public static final String LINKED_WORK_ITEMS_FIELD_NAME = "linkedWorkItems";

  public static WorkItemForJson castWorkItem(
      IWorkItem workItem,
      String[] includeCustomFields,
      String[] includeLinkRoles,
      Map<String, ILinkRoleOpt> linkNamesMap) {

    WorkItemForJson workItemForJson = new WorkItemForJson(workItem.getId(), UpdateType.UPDATED);
    if (workItem.getRevision() != null) workItemForJson.setRevision(workItem.getRevision());
    if (workItem.getDescription() != null)
      workItemForJson.setDescription(workItem.getDescription().getContent());
    if (workItem.getCreated() != null)
      workItemForJson.setCreated(workItem.getCreated().toInstant().toString());
    if (workItem.getDueDate() != null)
      workItemForJson.setDueDate(workItem.getDueDate().getDate().toInstant().toString());
    if (workItem.getInitialEstimate() != null)
      workItemForJson.setInitialEstimate(workItem.getInitialEstimate().toString());
    if (workItem.getOutlineNumber() != null)
      workItemForJson.setOutlineNumber(workItem.getOutlineNumber());
    if (workItem.getPlannedEnd() != null)
      workItemForJson.setPlannedEnd(workItem.getPlannedEnd().toInstant().toString());
    if (workItem.getPlannedStart() != null)
      workItemForJson.setPlannedStart(workItem.getPlannedStart().toInstant().toString());
    if (workItem.getPlannedIn() != null && !workItem.getPlannedIn().isEmpty()) {
      workItemForJson.setPlannedIn(
          workItem.getPlannedIn().stream()
              .map(plan -> plan.getId())
              .toArray(size -> new String[size]));
    }
    if (workItem.getPriority() != null)
      workItemForJson.setPriority(workItem.getPriority().getName());
    if (workItem.getRemainingEstimate() != null)
      workItemForJson.setRemainingEstimate(workItem.getRemainingEstimate().toString());
    if (workItem.getResolution() != null)
      workItemForJson.setResolution(workItem.getResolution().getName());
    if (workItem.getResolvedOn() != null)
      workItemForJson.setResolvedOn(workItem.getResolvedOn().toInstant().toString());
    if (workItem.getSeverity() != null)
      workItemForJson.setSeverity(workItem.getSeverity().getName());
    if (workItem.getStatus() != null) workItemForJson.setStatus(workItem.getStatus().getName());
    if (workItem.getTimeSpent() != null)
      workItemForJson.setTimeSpent(workItem.getTimeSpent().toString());
    if (workItem.getTitle() != null) workItemForJson.setTitle(workItem.getTitle());
    if (workItem.getType() != null) workItemForJson.setType(workItem.getType().getName());
    if (workItem.getUpdated() != null)
      workItemForJson.setUpdated(workItem.getUpdated().toInstant().toString());
    if (workItem.getModule() != null) workItemForJson.setModuleId(workItem.getModule().getId());
    if (workItem.getModule() != null)
      workItemForJson.setModuleTitle(workItem.getModule().getTitleOrName());
    if (workItem.getProjectId() != null) workItemForJson.setProjectId(workItem.getProjectId());
    if (workItem.getAuthor() != null) workItemForJson.setAuthor(workItem.getAuthor().getId());
    if (workItem.getWatchingUsers() != null && !workItem.getWatchingUsers().isEmpty())
      workItemForJson.setWatchers(castCollectionToStrList(workItem.getWatchingUsers()));
    if (includeCustomFields != null
        && includeCustomFields.length > 0
        && workItem.getCustomFieldsList() != null
        && !workItem.getCustomFieldsList().isEmpty())
      workItemForJson.setCustomFields(castCustomFields(workItem, includeCustomFields));
    if (workItem.getAssignees() != null && !workItem.getAssignees().isEmpty())
      workItemForJson.setAssignees(castCollectionToStrList(workItem.getAssignees()));
    if (workItem.getAttachments() != null && !workItem.getAttachments().isEmpty())
      workItemForJson.setAttachments(castCollectionToStrList(workItem.getAttachments()));
    if (workItem.getCategories() != null && !workItem.getCategories().isEmpty())
      workItemForJson.setCategories(castCollectionToStrList(workItem.getCategories()));
    if (workItem.getHyperlinks() != null && !workItem.getHyperlinks().isEmpty())
      workItemForJson.setHyperLinks(castHyperlinksToStrList(workItem.getHyperlinks()));
    if (workItem.getComments() != null && !workItem.getComments().isEmpty())
      workItemForJson.setComments(
          workItem.getComments().stream()
              .map(comment -> comment.getId())
              .toArray(size -> new String[size]));
    if (includeLinkRoles != null
        && includeLinkRoles.length > 0
        && workItem.getLinkedWorkItems() != null
        && !workItem.getLinkedWorkItems().isEmpty()) {

      List<ILinkedWorkItemStruct> directLinksStruct =
          (List<ILinkedWorkItemStruct>) workItem.getLinkedWorkItemsStructsDirect();
      List<LinkedWorkItem> linkedItems =
          (List<LinkedWorkItem>)
              directLinksStruct.stream()
                  .filter(
                      linkStruct ->
                          Arrays.asList(includeLinkRoles)
                              .contains(linkStruct.getLinkRole().getId()))
                  .map(
                      linkStruct -> {
                        linkNamesMap.putIfAbsent(
                            linkStruct.getLinkRole().getId(), linkStruct.getLinkRole());

                        return new LinkedWorkItem(
                            linkStruct.getLinkedItem().getId(),
                            linkStruct.getLinkRole().getId(),
                            linkStruct.getLinkRole().getName(),
                            LinkDirection.OUT);
                      })
                  .collect(Collectors.toList());
      if (!linkedItems.isEmpty()) {
        workItemForJson.setLinkedWorkItems(linkedItems);
      }
    }

    return workItemForJson;
  }

  public static List<String> castHyperlinksToStrList(Collection hyperlinks) {
    List<String> result = new ArrayList<String>();
    if (isCollectionHyperlinkStructList(hyperlinks)) {
      try {
        List<IHyperlinkStruct> collection = (List<IHyperlinkStruct>) hyperlinks;
        result =
            collection.stream()
                .map(elem -> ((IHyperlinkStruct) elem).getUri())
                .collect(Collectors.toList());
      } catch (ClassCastException ex) {
        // casting should not be an issue since we're checking it on the if
        return result;
      }
    }
    return result;
  }

  public static List<String> castLinkedWorkItemsToStrList(Collection linkedItems) {
    List<String> result = new ArrayList<String>();
    if (isCollectionLinkedWorkItemStructList(linkedItems)) {
      try {
        List<ILinkedWorkItemStruct> collection = (List<ILinkedWorkItemStruct>) linkedItems;
        result =
            collection.stream()
                .map(elem -> elem.getLinkedItem().getId())
                .collect(Collectors.toList());
      } catch (ClassCastException ex) {
        // casting should not be an issue since we're checking it on the if
      }
    }
    return result;
  }

  public static List<String> castApprovalsToStrList(Collection approvals) {
    List<String> result = new ArrayList<String>();
    if (isCollectionApprovalStructList(approvals)) {
      try {
        List<IApprovalStruct> collection = (List<IApprovalStruct>) approvals;
        result =
            collection.stream()
                .map(elem -> ((IApprovalStruct) elem).getUser().getId())
                .collect(Collectors.toList());
      } catch (ClassCastException ex) {
        // casting should not be an issue since we're checking it on the if
      }
    }
    return result;
  }

  /*
   * This will return false if the list is empty,
   * even if the list is of type IHyperlinkStruct
   * */
  public static boolean isCollectionHyperlinkStructList(Collection collection) {
    if (collection instanceof List) {
      List<?> list = (List<?>) collection;
      if (!list.isEmpty() && list.get(0) instanceof IHyperlinkStruct) {
        return true;
      }
    }
    return false;
  }

  /*
   * This will return false if the list is empty,
   * even if the list is of type ILinkedWorkedItemStruct
   * */
  public static boolean isCollectionLinkedWorkItemStructList(Collection collection) {
    if (collection instanceof List) {
      List<?> list = (List<?>) collection;
      if (!list.isEmpty() && list.get(0) instanceof ILinkedWorkItemStruct) {
        return true;
      }
    }
    return false;
  }

  /*
   * This will return false if the list is empty,
   * even if the list is of type IApprovalStruct
   * */
  public static boolean isCollectionApprovalStructList(Collection collection) {
    if (collection instanceof List) {
      List<?> list = (List<?>) collection;
      if (!list.isEmpty() && list.get(0) instanceof IApprovalStruct) {
        return true;
      }
    }
    return false;
  }

  private static HashMap<String, Object> castCustomFields(
      IWorkItem workItem, String[] includeCustomFields) {

    Set<String> customFields = new HashSet<>(workItem.getCustomFieldsList());
    Set<String> targetSet = new HashSet<>(Arrays.asList(includeCustomFields));

    targetSet.retainAll(customFields);

    HashMap<String, Object> converted = new HashMap<String, Object>(targetSet.size());
    targetSet.forEach(
        fieldName -> {
          converted.put(fieldName, castCustomFieldValue(workItem.getCustomField(fieldName)));
        });

    return converted;
  }

  /**
   * Currently, these are the field types that we convert to string: - String - enum values as
   * Polarion obj IEnumOption - dates coming from Polarion as java.util.Date - time coming from
   * Polarion as DurationTime
   */
  public static String castFieldValueToString(Object value) {
    if (value == null) {
      return "";
    } else if (value instanceof String) {
      return (String) value;
    } else if (value instanceof IEnumOption) {
      return ((IEnumOption) value).getName(); // Or should it be getId()?
    } else if (value instanceof java.util.Date) {
      return ((java.util.Date) value).toInstant().toString();
    } else if (value instanceof DurationTime) {
      return ((DurationTime) value).toString();
    } else if (value instanceof ILocation) {
      return ((ILocation) value).getLocationPath();
    } else if (value instanceof IModule) {
      return ((IModule) value).getId();
    } else {
      return value.toString();
    }
  }

  private static Object castCustomFieldValue(Object value) {
    if (value == null) {
      return "";
    } else if (value instanceof String) {
      return value;
    } else if (value instanceof IEnumOption) {
      return ((IEnumOption) value).getName(); // Or should it be getId()?
    } else if (value instanceof java.util.Date) {
      return ((java.util.Date) value).toInstant().toString();
    } else if (value instanceof DurationTime) return ((DurationTime) value).toString();
    else if (value instanceof ITestSteps) {
      return value.toString();
    } else {
      return value.toString();
    }
  }

  public static List<String> castCollectionToStrList(List<IPObject> collection) {
    return collection.stream()
        .map(
            elem -> {
              if (elem instanceof ICategory) {
                return ((ICategory) elem).getName();
              } else if (elem instanceof IUser) {
                return ((IUser) elem).getId();
              } else if (elem instanceof IAttachment) {
                return ((IAttachment) elem).getId();
              } else if (elem instanceof IComment) {
                return ((IComment) elem).getId();
              } else if (elem instanceof IWorkItem) {
                return ((IWorkItem) elem).getId();
              } else if (elem != null) {
                return elem.toString();
              } else {
                return "";
              }
            })
        .collect(Collectors.toList());
  }
}
