package com.teamscale.polarion.plugin.model;

/**
 * This class represents a specific type of field diff to represent changes related to the work item
 * links. Different from the other fieldDiffs, this one we encapsulate not only the values
 * added/remover but also the role 'type' (uniquely represented as role id, not the user-facing role
 * name).
 */
public class LinkFieldDiff extends WorkItemFieldDiff {

  private final String linkRoleId;
  private final String linkRoleName;
  private final String oppositeLinkRoleName;

  public LinkFieldDiff(
      String fieldName, String linkRoleId, String linkRoleName, String oppositeLinkRoleName) {
    super(fieldName);
    this.linkRoleId = linkRoleId;
    this.linkRoleName = linkRoleName;
    this.oppositeLinkRoleName = oppositeLinkRoleName;
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
}
