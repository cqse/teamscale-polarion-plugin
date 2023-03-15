package com.teamscale.polarion.plugin.model;

import java.util.Collection;

public class WorkItemForJson {
	
	private String workItemId;
	private String revision;
	private String description;
	private String dueDate;
	private String[] hyperLinks;
	private String initialEstimate;
	
	private String status;
	
	private Collection<WorkItemChange> workItemChanges;
	
	public WorkItemForJson(String workItemId) {
		this.workItemId = workItemId;
	}
	
	public String getWorkItemId() {
		return workItemId;
	}
	
	public void setWorkItemId(String workItemId) {
		this.workItemId = workItemId;
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
	
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}

	public Collection<WorkItemChange> getWorkItemChanges() {
		return workItemChanges;
	}

	public void setWorkItemChanges(Collection<WorkItemChange> workItemChanges) {
		this.workItemChanges = workItemChanges;
	}
}
