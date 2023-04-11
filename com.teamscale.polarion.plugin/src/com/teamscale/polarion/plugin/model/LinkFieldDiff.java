package com.teamscale.polarion.plugin.model;

/**
 * This class represents a specific type of field diff to represent changes related to the work item
 * links. Different from the other fieldDiffs, this one we encapsulate not only the values
 * added/remover but also the role 'type' (uniquely represented as role id, not the user-facing role
 * name).
 */
public class LinkFieldDiff extends WorkItemFieldDiff {

  private String linkRoleId;

  public LinkFieldDiff(String fieldName, String linkRoleId) {
    super(fieldName);
    this.linkRoleId = linkRoleId;
  }

  public void setLinkRoleId(String linkRoleId) {
    this.linkRoleId = linkRoleId;
  }

  public String getLinkRoleId() {
    return linkRoleId;
  }
}
