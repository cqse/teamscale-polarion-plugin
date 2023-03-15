package com.teamscale.polarion.plugin.model;

public class WorkItemChange {

	private String revision; //the revision after the change
	private String fieldName;
	private String fieldValueBefore;
	private String fieldValueAfter;
	private String[] elementsAdded;
	private String[] elementsRemoved;
	
	public WorkItemChange(String revision, String fieldName, String fieldValueBefore, 
			String fieldValueAfter, String[] elementsAdded, String[] elementsRemoved) {
		this.revision = revision;
		this.fieldName = fieldName;
		this.fieldValueBefore = fieldValueBefore;
		this.fieldValueAfter = fieldValueAfter;
		this.elementsAdded = elementsAdded;
		this.elementsRemoved = elementsRemoved;
	}
	
	public String getRevision() {
		return revision;
	}
	
	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldValueBefore() {
		return fieldValueBefore;
	}

	public void setFieldValueBefore(String fieldValueBefore) {
		this.fieldValueBefore = fieldValueBefore;
	}

	public String getFieldValueAfter() {
		return fieldValueAfter;
	}

	public void setFieldValueAfter(String fieldValueAfter) {
		this.fieldValueAfter = fieldValueAfter;
	}

	public String[] getElementsAdded() {
		return elementsAdded;
	}

	public void setElementsAdded(String[] elementsAdded) {
		this.elementsAdded = elementsAdded;
	}

	public String[] getElementsRemoved() {
		return elementsRemoved;
	}

	public void setElementsRemoved(String[] elementsRemoved) {
		this.elementsRemoved = elementsRemoved;
	}
}
