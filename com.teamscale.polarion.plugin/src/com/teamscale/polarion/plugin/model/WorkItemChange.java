package com.teamscale.polarion.plugin.model;

import java.util.Collection;

/**
 * This class encapsulates a Work Item change (revision) containing the revision number this change
 * represents and a collection of field diffs contained in the revision/change.
 */
public class WorkItemChange {

  /** the revision after the change */
  private final String revision;

  private Collection<WorkItemFieldDiff> fieldChanges;

  /** User id of who authored the changes (in this revision) */
  private final String revAuthorId;

  public WorkItemChange(
      String revision, Collection<WorkItemFieldDiff> fieldChanges, String revAuthorId) {
    this.revision = revision;
    this.fieldChanges = fieldChanges;
    this.revAuthorId = revAuthorId;
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

  public String getRevAuthorId() {
    return revAuthorId;
  }
}
