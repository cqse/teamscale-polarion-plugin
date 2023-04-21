package com.teamscale.polarion.plugin.model;

/**
 * Model class that represents a LinkedWorkItem containing the id of the linked WorkItem and the
 * link role id. Note: In Polarion the link role id might be different than link role user-facing
 * name).
 */
public class LinkedWorkItem {
  private String id;
  private String linkRoleId;
  private String linkRoleName;

  public LinkedWorkItem(String id, String linkRoleId, String linkRoleName) {
    this.id = id;
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLinkRoleId() {
    return linkRoleId;
  }

  public void setLinkRole(String linkRoleId) {
    this.linkRoleId = linkRoleId;
  }

  public String getLinkRoleName() {
    return linkRoleName;
  }

  public void setLinkRoleName() {
    this.linkRoleName = linkRoleName;
  }

  public boolean equals(Object linkedWorkItem) {
    if (linkedWorkItem instanceof LinkedWorkItem) {
      return (this.id.equals(((LinkedWorkItem) linkedWorkItem).getId()));
    }
    return false;
  }

  public int hashCode() {
    return id.hashCode();
  }
}
