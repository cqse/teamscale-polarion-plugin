package com.teamscale.polarion.plugin.model;

/** 
 * This class represents a WorkItem diff for a particular field.
 * The diff is generate by Polarion, while here we encapsulate what we need
 * for Teamscale.
 * */
public class WorkItemFieldDiff {

  private String fieldName;
  /**
   * We either use fieldValueBefore/After or we use elementsAdded/Removed. This is due to the way
   * Polarion treats categorical values vs. other values (e.g., string, numbers, dates).
   * https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/diff/IFieldDiff.html
   */
  private String fieldValueBefore;

  private String fieldValueAfter;
  private String[] elementsAdded;
  private String[] elementsRemoved;

  public WorkItemFieldDiff(String fieldName) {
  	this.fieldName = fieldName;
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
