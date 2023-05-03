package com.teamscale.polarion.plugin.model;

/**
 * This class represents a specific type of field diff to represent changes related to the work item
 * links. Different from the other fieldDiffs, this one we encapsulate not only the values
 * added/remover but also the role 'type' (uniquely represented as role id, not the user-facing role
 * name).
 */
public class LinkFieldDiff extends WorkItemFieldDiff {

  private String linkRoleId;
  private String linkRoleName;
  private LinkDirection linkDirection;

  public LinkFieldDiff(
      String fieldName, String linkRoleId, String linkRoleName, LinkDirection linkDirection) {
    super(fieldName);
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
    this.linkDirection = linkDirection;
  }

  public void setLinkRoleId(String linkRoleId) {
    this.linkRoleId = linkRoleId;
  }

  public String getLinkRoleId() {
    return linkRoleId;
  }

  public void setLinkRoleName(String linkRoleName) {
    this.linkRoleName = linkRoleName;
  }

  public String getLinkRoleName() {
    return linkRoleName;
  }

  public void setLinkDirection(LinkDirection linkDirection) {
    this.linkDirection = linkDirection;
  }

  public LinkDirection getLinkDirection() {
    return linkDirection;
  }
}
