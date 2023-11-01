package com.teamscale.polarion.plugin.utils;

import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.ICategory;
import com.polarion.alm.tracker.model.IComment;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.ITypedList;
import com.polarion.subterra.base.location.ILocation;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.UpdateType;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * General utility class for helping work item history and prepping objects for json serialization.
 */
public class CastUtils {

  /** Polarion field name for linked work items -- */
  public static final String LINKED_WORK_ITEMS_FIELD_NAME = "linkedWorkItems";

  /**
   * Takes a Polarion work item object and converts to a model object that will be further
   * serialized into json.
   */
  public static WorkItemForJson castWorkItem(
      IWorkItem workItem,
      String[] includeCustomFields,
      String[] includeLinkRoles,
      Map<String, ILinkRoleOpt> linkNamesMap) {

    WorkItemForJson workItemForJson =
        new WorkItemForJson(workItem.getId(), workItem.getUri().toString(), UpdateType.UPDATED);

    if (workItem.getRevision() != null) {
      workItemForJson.setRevision(workItem.getRevision());
    }
    if (workItem.getDescription() != null) {
      workItemForJson.setDescription(workItem.getDescription().getContent());
    }
    if (workItem.getCreated() != null) {
      workItemForJson.setCreated(workItem.getCreated().toInstant().toString());
    }
    if (workItem.getDueDate() != null) {
      workItemForJson.setDueDate(workItem.getDueDate().getDate().toInstant().toString());
    }
    if (workItem.getInitialEstimate() != null) {
      workItemForJson.setInitialEstimate(workItem.getInitialEstimate().toString());
    }
    if (workItem.getOutlineNumber() != null) {
      workItemForJson.setOutlineNumber(workItem.getOutlineNumber());
    }
    if (workItem.getPlannedEnd() != null) {
      workItemForJson.setPlannedEnd(workItem.getPlannedEnd().toInstant().toString());
    }
    if (workItem.getPlannedStart() != null) {
      workItemForJson.setPlannedStart(workItem.getPlannedStart().toInstant().toString());
    }
    if (workItem.getPlannedIn() != null && !workItem.getPlannedIn().isEmpty()) {
      workItemForJson.setPlannedIn(
          workItem.getPlannedIn().stream()
              .map(plan -> plan.getId())
              .toArray(size -> new String[size]));
    }
    if (workItem.getPriority() != null) {
      workItemForJson.setPriority(workItem.getPriority().getName());
    }
    if (workItem.getRemainingEstimate() != null) {
      workItemForJson.setRemainingEstimate(workItem.getRemainingEstimate().toString());
    }
    if (workItem.getResolution() != null) {
      workItemForJson.setResolution(workItem.getResolution().getName());
    }
    if (workItem.getResolvedOn() != null) {
      workItemForJson.setResolvedOn(workItem.getResolvedOn().toInstant().toString());
    }
    if (workItem.getSeverity() != null) {
      workItemForJson.setSeverity(workItem.getSeverity().getName());
    }
    if (workItem.getStatus() != null) {
      workItemForJson.setStatus(workItem.getStatus().getName());
    }
    if (workItem.getTimeSpent() != null) {
      workItemForJson.setTimeSpent(workItem.getTimeSpent().toString());
    }
    if (workItem.getTitle() != null) {
      workItemForJson.setTitle(workItem.getTitle());
    }
    if (workItem.getType() != null) {
      workItemForJson.setType(workItem.getType().getId());
    }
    if (workItem.getUpdated() != null) {
      workItemForJson.setUpdated(workItem.getUpdated().toInstant().toString());
    }
    if (workItem.getModule() != null) {
      IModule module = workItem.getModule();
      workItemForJson.setModuleId(module.getId());
      workItemForJson.setModuleTitle(module.getTitleOrName());
      workItemForJson.setModuleFolder(module.getModuleFolder());
    }
    if (workItem.getProjectId() != null) {
      workItemForJson.setProjectId(workItem.getProjectId());
    }
    if (workItem.getAuthor() != null) {
      workItemForJson.setAuthor(workItem.getAuthor().getId());
    }
    if (workItem.getWatchingUsers() != null && !workItem.getWatchingUsers().isEmpty()) {
      workItemForJson.setWatchers(castCollectionToStrList(workItem.getWatchingUsers()));
    }
    if (includeCustomFields != null
        && includeCustomFields.length > 0
        && workItem.getCustomFieldsList() != null
        && !workItem.getCustomFieldsList().isEmpty()) {
      workItemForJson.setCustomFields(castCustomFields(workItem, includeCustomFields));
    }
    if (workItem.getAssignees() != null && !workItem.getAssignees().isEmpty()) {
      workItemForJson.setAssignees(castCollectionToStrList(workItem.getAssignees()));
    }
    if (workItem.getAttachments() != null && !workItem.getAttachments().isEmpty()) {
      workItemForJson.setAttachments(castCollectionToStrList(workItem.getAttachments()));
    }
    if (workItem.getCategories() != null && !workItem.getCategories().isEmpty()) {
      workItemForJson.setCategories(castCollectionToStrList(workItem.getCategories()));
    }
    if (workItem.getHyperlinks() != null && !workItem.getHyperlinks().isEmpty()) {
      workItemForJson.setHyperLinks(
          CollectionsAndEnumsUtils.castHyperlinksToStrList(workItem.getHyperlinks()));
    }
    if (workItem.getComments() != null && !workItem.getComments().isEmpty()) {
      workItemForJson.setComments(
          workItem.getComments().stream()
              .map(comment -> comment.getId())
              .toArray(size -> new String[size]));
    }
    if (includeLinkRoles != null
        && includeLinkRoles.length > 0
        && workItem.getLinkedWorkItems() != null
        && !workItem.getLinkedWorkItems().isEmpty()) {

      List<ILinkedWorkItemStruct> directLinksStruct =
          (List<ILinkedWorkItemStruct>) workItem.getLinkedWorkItemsStructsDirect();
      List<ILinkedWorkItemStruct> backLinksStruct =
          (List<ILinkedWorkItemStruct>) workItem.getLinkedWorkItemsStructsBack();
      directLinksStruct.addAll(backLinksStruct); // both direct and back links
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
                            linkStruct.getLinkedItem().getUri().toString(),
                            linkStruct.getLinkRole().getId(),
                            linkStruct.getLinkRole().getName(),
                            linkStruct.getLinkRole().getOppositeName());
                      })
                  .collect(Collectors.toList());
      if (!linkedItems.isEmpty()) {
        workItemForJson.setLinkedWorkItems(linkedItems);
      }
    }

    return workItemForJson;
  }

  private static HashMap<String, Object> castCustomFields(
      IWorkItem workItem, String[] includeCustomFields) {

    Set<String> customFields = new HashSet<>(workItem.getCustomFieldsList());
    Set<String> targetSet = new HashSet<>(Arrays.asList(includeCustomFields));

    targetSet.retainAll(customFields);

    HashMap<String, Object> converted = new HashMap<>(targetSet.size());
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
    } else if (value instanceof IUniqueObject) {
      return ((IUniqueObject) value).getId();
    } else if (value instanceof String) {
      return (String) value;
    } else if (value instanceof IEnumOption) {
      return ((IEnumOption) value).getId();
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
      return ((IEnumOption) value).getId();
    } else if (value instanceof java.util.Date) {
      return ((java.util.Date) value).toInstant().toString();
    } else if (value instanceof DurationTime) {
      return ((DurationTime) value).toString();
    } else if (value instanceof ITypedList) {
      return castCustomFieldTypedList((ITypedList) value);
    } else {
      return value.toString();
    }
  }

  private static Object castCustomFieldTypedList(ITypedList<?> list) {
    return list.stream().map(elem -> castCustomFieldValue(elem)).collect(Collectors.toList());
  }

  /**
   * Takes a collection of Polarion objects and converts to a list of strings (usually Ids). The
   * else/ifs were initially added to give us flexibility to handle those conversions depending on
   * the object specific type. For now, all of them are converted to Ids because all those cases are
   * of type IUniqueObject (which has getId()) but not all IPObjects are necessarily IUniqueObjects
   * in Polarion.
   */
  public static List<String> castCollectionToStrList(List<IPObject> collection) {
    return collection.stream()
        .map(
            elem -> {
              if (elem instanceof ICategory) {
                return ((ICategory) elem).getId();
              } else if (elem instanceof IUser) {
                return ((IUser) elem).getId();
              } else if (elem instanceof IAttachment) {
                return ((IAttachment) elem).getId();
              } else if (elem instanceof IComment) {
                return ((IComment) elem).getId();
              } else if (elem instanceof IWorkItem) {
                return ((IWorkItem) elem).getId();
              } else if (elem instanceof IEnumOption) {
                return ((IEnumOption) elem).getId();
              } else if (elem != null) {
                return elem.toString();
              } else {
                return "";
              }
            })
        .collect(Collectors.toList());
  }

  /** Checks if the collection is a list of IEnumOptions and early returns false if it's empty */
  public static boolean isUntypedListWithEnumOptions(Collection collection) {
    if (!collection.isEmpty() && collection instanceof List) {
      List<?> list = (List<?>) collection;
      return list.get(0) instanceof IEnumOption;
    }
    return false;
  }

  /**
   * Takes an untyped list returned by Polarion API and converts it to a list of strings (usually
   * Ids). If the input is not a list of EnumOptions then returns empty strings as elements. Use
   * case: for custom fields Polarion returns an untyped List (ArrayList) and typically the internal
   * elements are of type EnumOption (as custom field types are typically defined as enumerations on
   * Polarion admin settings).
   */
  public static List<String> castUntypedListToStrList(List<?> list) {
    return list.stream()
        .map(
            elem -> {
              if (elem instanceof IEnumOption) {
                return ((IEnumOption) elem).getId();
              } else {
                return "";
              }
            })
        .collect(Collectors.toList());
  }
}
