package com.teamscale.polarion.plugin.model;

/** 
 * This is a helper object that maintains link data and is used to process
 * and generate opposite link changes in the plugin.
 * **/
public class LinkBundle {

	// Whether this is an added or removed link
  private boolean added;
  
  // The object that represents the linked item and the role id
  private LinkedWorkItem linkedWorkItem;
  
  // The revision # this link represents
  private String revision;

  public void setAdded(boolean added) {
    this.added = added;
  }

  public boolean isAdded() {
    return added;
  }

  public void setLinkedWorkItem(LinkedWorkItem linkedWorkItem) {
    this.linkedWorkItem = linkedWorkItem;
  }

  public LinkedWorkItem getLinkedWorkItem() {
    return linkedWorkItem;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public String getRevision() {
    return revision;
  }
}
