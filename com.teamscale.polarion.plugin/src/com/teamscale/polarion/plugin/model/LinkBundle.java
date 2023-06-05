package com.teamscale.polarion.plugin.model;

/**
 * This is a helper object that maintains link data and is used to process and generate opposite
 * link changes in the plugin. *
 */
public class LinkBundle {

  /** Whether this is an added or removed link */
  private final boolean added;

  /** The object that represents the linked item and the role id */
  private final LinkedWorkItem linkedWorkItem;

  /** The revision number this link represents */
  private final String revision;

  public LinkBundle(boolean added, LinkedWorkItem linkedWorkItem, String revision) {
    this.added = added;
    this.linkedWorkItem = linkedWorkItem;
    this.revision = revision;
  }

  public boolean isAdded() {
    return added;
  }

  public LinkedWorkItem getLinkedWorkItem() {
    return linkedWorkItem;
  }

  public String getRevision() {
    return revision;
  }
}
