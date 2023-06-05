package com.teamscale.polarion.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a WorkItem diff for a particular field. The diff is generate by Polarion,
 * while here we encapsulate what we need for Teamscale.
 */
public class WorkItemFieldDiff {

  private final String fieldName;

  /**
   * We either use fieldValueBefore/After or we use elementsAdded/Removed. This is due to the way
   * Polarion treats categorical values vs. other values (e.g., string, numbers, dates).
   * https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/diff/IFieldDiff.html
   */
  private final String fieldValueBefore;

  private final String fieldValueAfter;

  /**
   * Cannot make these two fields immutable since we might need to add elements to them when we add
   * opposite links after processing all direct link changes
   */
  private List<String> elementsAdded;

  private List<String> elementsRemoved;

  public WorkItemFieldDiff(String fieldName, String fieldValueBefore, String fieldValueAfter) {
    this.fieldName = fieldName;
    this.fieldValueBefore = fieldValueBefore;
    this.fieldValueAfter = fieldValueAfter;
  }

  /** Use this constructor if you the field is a collection */
  public WorkItemFieldDiff(String fieldName) {
    this.fieldName = fieldName;
    this.fieldValueBefore = null;
    this.fieldValueAfter = null;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getFieldValueBefore() {
    return fieldValueBefore;
  }

  public String getFieldValueAfter() {
    return fieldValueAfter;
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

  /**
   * It'll add a single string element to the list of added values even if the list is empty/null
   */
  public void addElementAdded(String elementAdded) {
    if (elementsAdded == null) {
      elementsAdded = new ArrayList<>();
    }
    elementsAdded.add(elementAdded);
  }

  /**
   * It'll remove a single string element from the list of removed values even if the list is
   * empty/null
   */
  public void addElementRemoved(String elementRemoved) {
    if (elementsRemoved == null) {
      elementsRemoved = new ArrayList<>();
    }
    elementsRemoved.add(elementRemoved);
  }
}
