package com.teamscale.polarion.plugin.utils;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.ICategory;
import com.polarion.alm.tracker.model.IComment;
import com.polarion.alm.tracker.model.IHyperlinkStruct;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.ITestSteps;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {
		
	public enum UpdateType {
			UPDATED,
			DELETED
	}

  public static WorkItemForJson castWorkItem(
      IWorkItem workItem, String[] includeCustomFields, String[] includeLinkRoles) {

    WorkItemForJson workItemForJson = new WorkItemForJson(workItem.getId(), UpdateType.UPDATED,
    				workItem.getLastRevision());
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
    if (workItem.getProjectId() != null) workItemForJson.setProjectId(workItem.getProjectId());
    if (workItem.getAuthor() != null) workItemForJson.setAuthor(workItem.getAuthor().getId());
    if (workItem.getWatchingUsers() != null && !workItem.getWatchingUsers().isEmpty())
      workItemForJson.setWatchers(castCollectionToStrArray(workItem.getWatchingUsers()));
    if (includeCustomFields != null
        && includeCustomFields.length > 0
        && workItem.getCustomFieldsList() != null
        && !workItem.getCustomFieldsList().isEmpty())
      workItemForJson.setCustomFields(castCustomFields(workItem, includeCustomFields));
    if (workItem.getAssignees() != null && !workItem.getAssignees().isEmpty())
      workItemForJson.setAssignees(castCollectionToStrArray(workItem.getAssignees()));
    if (workItem.getAttachments() != null && !workItem.getAttachments().isEmpty())
      workItemForJson.setAttachments(castCollectionToStrArray(workItem.getAttachments()));
    if (workItem.getCategories() != null && !workItem.getCategories().isEmpty())
      workItemForJson.setCategories(castCollectionToStrArray(workItem.getCategories()));
    if (workItem.getHyperlinks() != null && !workItem.getHyperlinks().isEmpty())
      workItemForJson.setHyperLinks(castHyperlinksToStrArray(workItem.getHyperlinks()));
    if (workItem.getComments() != null && !workItem.getComments().isEmpty())
      workItemForJson.setComments(
          workItem.getComments().stream()
              .map(comment -> comment.getId())
              .toArray(size -> new String[size]));
    if (includeLinkRoles != null
        && includeLinkRoles.length > 0
        && workItem.getLinkedWorkItems() != null
        && !workItem.getLinkedWorkItems().isEmpty()) {
      // Getting all links (in and out links)
      Set<String> linksBackIdsSet =
          workItem.getLinkedWorkItemsStructsBack().stream()
              .filter(
                  linkStruct ->
                      Arrays.asList(includeLinkRoles).contains(linkStruct.getLinkRole().getId()))
              .map(linkStruct -> linkStruct.getLinkedItem().getId())
              .collect(Collectors.toSet());

      Set<String> linksDirectIdsSet =
          workItem.getLinkedWorkItemsStructsDirect().stream()
              .filter(
                  linkStruct ->
                      Arrays.asList(includeLinkRoles).contains(linkStruct.getLinkRole().getId()))
              .map(linkStruct -> linkStruct.getLinkedItem().getId())
              .collect(Collectors.toSet());

      if (!linksDirectIdsSet.isEmpty() || !linksBackIdsSet.isEmpty()) {
        linksDirectIdsSet.addAll(linksBackIdsSet);
        workItemForJson.setLinkedWorkItems(
            linksDirectIdsSet.toArray(new String[linksDirectIdsSet.size()]));
      }
    }

    return workItemForJson;
  }

  public static String[] castHyperlinksToStrArray(Collection hyperlinks) {
    String[] result = new String[] {""};
    if (isCollectionHyperlinkStructList(hyperlinks)) {
      try {
        List<IHyperlinkStruct> collection = (List<IHyperlinkStruct>) hyperlinks;
        result =
            collection.stream()
                .map(elem -> elem.getValue(IHyperlinkStruct.KEY_URI))
                .toArray(size -> new String[size]);
      } catch (ClassCastException ex) {
      	// not logging this very specific casting issue
      	// so, just return an empty string array.
        return result;
      }
    }
    return result;
  }

  public static String[] castLinkedWorkItemsToStrArray(Collection linkedItems) {
    String[] result = new String[] {""};
    if (isCollectionLinkedWorkItemStructList(linkedItems)) {
      try {
        List<ILinkedWorkItemStruct> collection = (List<ILinkedWorkItemStruct>) linkedItems;
        result =
            collection.stream()
                .map(elem -> elem.getLinkedItem().getId())
                .toArray(size -> new String[size]);
      } catch (ClassCastException ex) {
        // not logging this very specific casting issue
        // so, just return an empty string array.
      }
    }
    return result;
  }

  public static String[] castApprovalsToStrArray(Collection approvals) {
    String[] result = new String[] {""};
    if (isCollectionApprovalStructList(approvals)) {
      try {
        List<IApprovalStruct> collection = (List<IApprovalStruct>) approvals;
        result =
            collection.stream()
                .map(elem -> elem.getUser().getId())
                .toArray(size -> new String[size]);
      } catch (ClassCastException ex) {
      	// not logging this very specific casting issue
      	// so, just return an empty string array.
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
    //		Collection<String> customFieldsList = workItem.getCustomFieldsList();
    Set<String> customFields = new HashSet<>(workItem.getCustomFieldsList());
    Set<String> targetSet = new HashSet<>(Arrays.asList(includeCustomFields));

    //		customFieldsList.forEach( fieldName -> {
    //			if (includeCustomFields)
    //			converted.put(fieldName, castCustomFieldValue(workItem.getCustomField(fieldName)));
    //		});

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
    } else if (value instanceof DurationTime) return ((DurationTime) value).toString();
    else {
      return value.toString();
    }
  }

  private static Object castCustomFieldValue(Object value) {
    if (value == null) {
      return "";
    } else if (value instanceof String) {
      return (String) value;
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

  //	private static Object convertTestStepValues(ITestSteps testSteps) {
  //		HashMap<String[], String[]> convertedSteps = new HashMap<String[],
  // String[]>(testSteps.getKeys().size());
  //		//TODO: Is it possible to have test steps without keys? Or at least a single test step value
  // without key?
  //		for (int index = 0; index < testSteps.getKeys().size(); index++) {
  //			String key = testSteps.getKeys().get(index).getName();
  //			if (testSteps.getSteps() != null && testSteps.getSteps().size() > 0) {
  //				List<Text> textList = testSteps.getSteps().get(index).getValues();
  //				String[] values = (String[])textList.stream().map(textElem ->
  // textElem.getContent()).toArray(size -> new String[size]);
  //				convertedSteps.put(key, values);
  //			} else {
  //				convertedSteps.put(key, new String[]{""});
  //			}
  //		}
  //		return convertedSteps;
  //	}

  public static String[] castCollectionToStrArray(List<IPObject> collection) {
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
              } else if (elem instanceof ILinkedWorkItemStruct) {
                return ((IWorkItem) elem).getId();
              } else if (elem != null) {
                return elem.toString();
              } else {
                return "";
              }
            })
        .toArray(size -> new String[size]);
  }

}
