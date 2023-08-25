package com.teamscale.polarion.plugin.model;

/**
 * Model class that represents a LinkedWorkItem containing the id of the linked WorkItem and the
 * link role id, name, and direction. Note: In Polarion the link role id might be different from
 * link role user-facing name).
 */
public class LinkedWorkItem {
  private final String id;
  private final String uri;
  private final String linkRoleId;
  private final String linkRoleName;
  private final String oppositeLinkRoleName;

  public LinkedWorkItem(
      String id, String uri, String linkRoleId, String linkRoleName, String oppositeLinkRoleName) {
    this.id = id;
    this.uri = uri;
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
    this.oppositeLinkRoleName = oppositeLinkRoleName;
  }

  public String getId() {
    return id;
  }

  public String getUri() {
    return uri;
  }

  public String getLinkRoleId() {
    return linkRoleId;
  }

  public String getLinkRoleName() {
    return linkRoleName;
  }

  public String getOppositeLinkRoleName() {
    return oppositeLinkRoleName;
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
