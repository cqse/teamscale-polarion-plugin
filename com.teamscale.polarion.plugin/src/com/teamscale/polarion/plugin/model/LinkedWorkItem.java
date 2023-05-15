package com.teamscale.polarion.plugin.model;

/**
 * Model class that represents a LinkedWorkItem containing the id of the linked WorkItem and the
 * link role id, name, and direction. Note: In Polarion the link role id might be different than
 * link role user-facing name).
 */
public class LinkedWorkItem {
  private final String id;
  private final String linkRoleId;
  private final String linkRoleName;
  private final LinkDirection linkDirection;

  public LinkedWorkItem(
      String id, String linkRoleId, String linkRoleName, LinkDirection linkDirection) {
    this.id = id;
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
    this.linkDirection = linkDirection;
  }

  public String getId() {
    return id;
  }

  public String getLinkRoleId() {
    return linkRoleId;
  }

  public String getLinkRoleName() {
    return linkRoleName;
  }

  public LinkDirection getLinkDirection() {
    return linkDirection;
  }

  /** LinkedWorkItem objects are compared by id */
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
