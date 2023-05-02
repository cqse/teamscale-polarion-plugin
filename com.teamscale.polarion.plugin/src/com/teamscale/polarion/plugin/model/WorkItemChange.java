package com.teamscale.polarion.plugin.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class encapsulates a Work Item change (revision) containing the revision number this change
 * represents and a collection of field diffs contained in the revision/change.
 */
public class WorkItemChange {

  /** the revision after the change */
  private String revision;

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

  public void addFieldChanges(Collection<WorkItemFieldDiff> fieldChanges) {
    this.fieldChanges.addAll(fieldChanges);
  }

  public Collection<WorkItemFieldDiff> getFieldChanges() {
    return fieldChanges;
  }

  public void setFieldChanges(Collection<WorkItemFieldDiff> changes) {
    fieldChanges = changes;
  }
}
