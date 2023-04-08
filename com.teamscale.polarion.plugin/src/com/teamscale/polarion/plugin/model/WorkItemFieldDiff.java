package com.teamscale.polarion.plugin.model;

import java.util.ArrayList;
import java.util.List;

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
  private List<String> elementsAdded;
  private List<String> elementsRemoved;

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

  public List<String> getElementsAdded() {
    return elementsAdded;
  }

  public void setElementsAdded(List<String> elementsAdded) {
    this.elementsAdded = elementsAdded;
  }

  public List<String> getElementsRemoved() {
    return elementsRemoved;
  }

  public void setElementsRemoved(List<String> elementsRemoved) {
    this.elementsRemoved = elementsRemoved;
  }
  
  public void addElementAdded(String elementAdded) {
  		if (elementsAdded == null)
  				elementsAdded = new ArrayList<String>();
  		elementsAdded.add(elementAdded);
  }
  
  public void addElementRemoved(String elementRemoved) {
  		if (elementsRemoved == null)
  				elementsRemoved = new ArrayList<String>(); 		
  		elementsRemoved.add(elementRemoved);
  }
  
}
