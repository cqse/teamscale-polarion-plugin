package com.teamscale.polarion.plugin.model;

public class WorkItemChange {

	private String workItemId;
	
	public WorkItemChange(String workItemId) {
		this.workItemId = workItemId;
	}
	
	public String getWorkItemId() {
		return workItemId;
	}
	
	public void setWorkItemId(String workItemId) {
		this.workItemId = workItemId;
	}
}
