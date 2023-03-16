package com.teamscale.polarion.plugin.model;

import java.util.ArrayList;
import java.util.Collection;

public class WorkItemChange {

	private String revision; //the revision after the change
	private Collection<WorkItemFieldDiff> fieldChanges;

	
	public WorkItemChange(String revision) {
		this.revision = revision;
		fieldChanges = new ArrayList<WorkItemFieldDiff>();
	}
	
	public String getRevision() {
		return revision;
	}
	
	public void setRevision(String revision) {
		this.revision = revision;
	}
	
	public void addFieldChange(WorkItemFieldDiff fieldChange) {
		fieldChanges.add(fieldChange);
	}
	
	public Collection<WorkItemFieldDiff> getFieldChanges() {
		return fieldChanges;
	}
	
	public void setFieldChanges(Collection<WorkItemFieldDiff> changes) {
		fieldChanges = changes;
	}
}
