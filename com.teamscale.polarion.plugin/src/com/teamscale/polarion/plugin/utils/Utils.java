package com.teamscale.polarion.plugin.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.polarion.alm.tracker.model.ICategory;
import com.polarion.alm.tracker.model.IHyperlinkStruct;
import com.polarion.alm.tracker.model.ITestSteps;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.persistence.IEnumOption;
import com.teamscale.polarion.plugin.model.WorkItemForJson;

public class Utils {
	
	public static WorkItemForJson castWorkItem(IWorkItem workItem) {
		WorkItemForJson workItemForJson = new WorkItemForJson(workItem.getId());
		if (workItem.getRevision() != null)
			workItemForJson.setRevision(workItem.getRevision());
		if (workItem.getDescription() != null)
			workItemForJson.setDescription(workItem.getDescription().getContent());
		if (workItem.getCreated() != null)
			workItemForJson.setCreated(workItem.getCreated().toInstant().toString());
		if (workItem.getDueDate() != null)
			workItemForJson.setDueDate(workItem.getDueDate().getDate().toInstant().toString());
		if (!workItem.getHyperlinks().isEmpty())
			workItemForJson.setHyperLinks((String[])workItem.getHyperlinks().toArray(size -> new String[size]));
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
		if(workItem.getStatus() != null)
			workItemForJson.setStatus(workItem.getStatus().getName());
		if (workItem.getTimeSpent() != null)
			workItemForJson.setTimeSpent(workItem.getTimeSpent().toString());	
		if (workItem.getTitle() != null)
			workItemForJson.setTitle(workItem.getTitle());
		if(workItem.getType() != null)
			workItemForJson.setType(workItem.getType().getName());
		if (workItem.getUpdated() != null)
			workItemForJson.setUpdated(workItem.getUpdated().toInstant().toString());	
		if (workItem.getCustomFieldsList() != null &&
				workItem.getCustomFieldsList().size() > 0)
			workItemForJson.setCustomFields(castCustomFields(workItem));
		return workItemForJson;
	}
	
	private static HashMap<String, Object> castCustomFields(IWorkItem workItem) {
		Collection<String> customFieldsList = workItem.getCustomFieldsList();
		HashMap<String, Object> converted = new HashMap<String, Object>(customFieldsList.size());
		
		customFieldsList.forEach( fieldName -> {
			converted.put(fieldName, castCustomFieldValue(workItem.getCustomField(fieldName)));
		});
		return converted;
	}
	
	/**
	 * Currently, these are the field types that we convert to string:
	 *  - String
	 *  - enum values as Polarion obj IEnumOption
	 *  - dates coming from Polarion as java.util.Date
	 *  - time coming from Polarion as DurationTime
	 **/
	public static String castFieldValueToString(Object value) {
		if (value == null) {
			return "";
		} else if (value instanceof String) {
			return (String) value;
		} else if (value instanceof IEnumOption) {
			return ((IEnumOption) value).getName(); //Or should it be getId()?
		} else if (value instanceof java.util.Date) {
			return ((java.util.Date) value).toInstant().toString();
		} else if (value instanceof DurationTime)
			return ((DurationTime) value).toString();
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
			return ((IEnumOption) value).getName(); //Or should it be getId()?
		} else if (value instanceof java.util.Date) {
			return ((java.util.Date) value).toInstant().toString();
		} else if (value instanceof DurationTime)
			return ((DurationTime) value).toString();
		else if (value instanceof ITestSteps) {			
			return value.toString(); //TODO: Continue work here.
//			return convertTestStepValues((ITestSteps) value);
		}
		else {
			return value.toString();
		}		
	}
	
//	private static Object convertTestStepValues(ITestSteps testSteps) {
//		HashMap<String[], String[]> convertedSteps = new HashMap<String[], String[]>(testSteps.getKeys().size());
//		//TODO: Is it possible to have test steps without keys? Or at least a single test step value without key?
//		for (int index = 0; index < testSteps.getKeys().size(); index++) {
//			String key = testSteps.getKeys().get(index).getName();
//			if (testSteps.getSteps() != null && testSteps.getSteps().size() > 0) {
//				List<Text> textList = testSteps.getSteps().get(index).getValues();
//				String[] values = (String[])textList.stream().map(textElem -> textElem.getContent()).toArray(size -> new String[size]);
//				convertedSteps.put(key, values);
//			} else {
//				convertedSteps.put(key, new String[]{""});
//			}
//		}
//		return convertedSteps;	
//	}
	
	//TODO: What to do with other categorical fields that aren't mapped here?
	/** 
	 * Polarion returns the unparameterized Collection type.
	 * That's why we're not parameterizing Collection here
	 * **/
	public static String[] castCollectionToStrArray(Collection collection) {
		return (String[]) collection.stream().map(elem -> {
			if (elem instanceof ICategory) {
				return ((ICategory) elem).getName();
			} else if (elem instanceof IHyperlinkStruct) {
				return ((IHyperlinkStruct) elem).getValue(IHyperlinkStruct.KEY_URI);
			} else if (elem != null){ 
				return elem.toString(); 
			} 
			else {
				return "";
			}
		}).toArray( size -> new String[size]);
	}

}
