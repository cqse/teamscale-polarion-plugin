package com.teamscale.polarion.plugin.model;

import java.util.Collection;
import java.util.HashMap;

public class WorkItemForJson {
	
	private String id;
	private String revision;
	private String description; 
	private String created; //date-time format, ex: '1970-01-01T00:00:00Z'
	private String dueDate; //date-time format
	private String[] hyperLinks;
	private String initialEstimate;
	private String outlineNumber;
	private String plannedEnd; //date-time format
	private String plannedStart; //date-time format
	private String priority;
	private String remainingEstimate;
	private String resolution;
	private String resolvedOn; //date-time format
	private String severity;	
	private String status;
	private String timeSpent;
	private String title;
	private String type;
	private String updated; //date-time format
	private HashMap<String, Object> customFields;
	private String[] assignees;
	
	private Collection<WorkItemChange> workItemChanges;
	
	public WorkItemForJson(String workItemId) {
		this.id = workItemId;
	}
	
	public String getWorkItemId() {
		return id;
	}
	
	public void setWorkItemId(String workItemId) {
		this.id = workItemId;
	}
	
	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getDueDate() {
		return dueDate;
	}
	
	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}
	
	public String[] getHyperLinks() {
		return this.hyperLinks;
	}
	
	public void setHyperLinks(String[] hyperLinks) {
		this.hyperLinks = hyperLinks;
	}
	
	public String getInitialEstimate() {
		return initialEstimate;
	}
	
	public void setInitialEstimate(String initialEstimate) {
		this.initialEstimate = initialEstimate;
	}
	
	public String getOutlineNumber() {
		return outlineNumber;
	}

	public void setOutlineNumber(String outlineNumber) {
		this.outlineNumber = outlineNumber;
	}

	public String getPlannedEnd() {
		return plannedEnd;
	}

	public void setPlannedEnd(String plannedEnd) {
		this.plannedEnd = plannedEnd;
	}

	public String getPlannedStart() {
		return plannedStart;
	}

	public void setPlannedStart(String plannedStart) {
		this.plannedStart = plannedStart;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getRemainingEstimate() {
		return remainingEstimate;
	}

	public void setRemainingEstimate(String remainingEstimate) {
		this.remainingEstimate = remainingEstimate;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getResolvedOn() {
		return resolvedOn;
	}

	public void setResolvedOn(String resolvedOn) {
		this.resolvedOn = resolvedOn;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}

	public String getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(String timeSpent) {
		this.timeSpent = timeSpent;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}

	public HashMap<String, Object> getCustomFields() {
		return customFields;
	}

	public void setCustomFields(HashMap<String, Object> customFields) {
		this.customFields = customFields;
	}

	public String[] getAssignees() {
		return assignees;
	}

	public void setAssignees(String[] assignees) {
		this.assignees = assignees;
	}

	public Collection<WorkItemChange> getWorkItemChanges() {
		return workItemChanges;
	}

	public void setWorkItemChanges(Collection<WorkItemChange> workItemChanges) {
		this.workItemChanges = workItemChanges;
	}
}
