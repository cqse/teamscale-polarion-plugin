package com.teamscale.polarion.plugin.model;

import com.teamscale.polarion.plugin.utils.Utils;

/**
 * Model class that represents a LinkedWorkItem containing the id of the linked WorkItem and the
 * link role id. Note: In Polarion the link role id might be different than link role user-facing
 * name).
 */
public class LinkedWorkItem {
  private String id;
  private String linkRoleId;
  private String linkRoleName;
  private Utils.LinkDirection linkDirection;

  public LinkedWorkItem(
      String id, String linkRoleId, String linkRoleName, Utils.LinkDirection linkDirection) {
    this.id = id;
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
    this.linkDirection = linkDirection;
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

  public void setLinkRoleId(String linkRoleId) {
    this.linkRoleId = linkRoleId;
  }

  public String getLinkRoleName() {
    return linkRoleName;
  }

  public void setLinkRoleName(String linkRoleName) {
    this.linkRoleName = linkRoleName;
  }

  public Utils.LinkDirection getLinkDirection() {
    return linkDirection;
  }

  public void setLinkDirection(Utils.LinkDirection linkDirection) {
    this.linkDirection = linkDirection;
  }

  public boolean equals(Object linkedWorkItem) {
    if (linkedWorkItem instanceof LinkedWorkItem) {
      return (id.equals(((LinkedWorkItem) linkedWorkItem).getId()));
    }
    return false;
  }

  public int hashCode() {
    return id.hashCode();
  }
}
