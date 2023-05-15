package com.teamscale.polarion.plugin.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class encapsulates a Work Item change (revision) containing the revision number this change
 * represents and a collection of field diffs contained in the revision/change.
 */
public class WorkItemChange {

  /** the revision after the change */
  private final String revision;

  private Collection<WorkItemFieldDiff> fieldChanges;

  public WorkItemChange(String revision, Collection<WorkItemFieldDiff> fieldChanges) {
    this.revision = revision;
    this.fieldChanges = fieldChanges;
  }

  public WorkItemChange(String revision) {
    this.revision = revision;
    this.fieldChanges = new ArrayList<>();
  }

  public String getRevision() {
    return revision;
  }

  public void addFieldChange(WorkItemFieldDiff fieldChange) {
    fieldChanges.add(fieldChange);
  }

  public Collection<WorkItemFieldDiff> getFieldChanges() {
    return fieldChanges;
  }
}
