package com.teamscale.polarion.plugin.model;

/**
 * Model class that represents a LinkedWorkItem containing the id of the linked WorkItem and the
 * link role id, and name as exhibited in Polarion. 
 * Note: In Polarion the link role id might be different from link role user-facing name).
 * Note: Teamscale project configuration utilizes link role name (not link rolde id)
 */
public class LinkedWorkItem {
  private final String id;
  private final String uri;
  private final String linkRoleId;
  private final String linkRoleName;

  public LinkedWorkItem(
      String id, String uri, String linkRoleId, String linkRoleName) {
    this.id = id;
    this.uri = uri;
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
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

}
