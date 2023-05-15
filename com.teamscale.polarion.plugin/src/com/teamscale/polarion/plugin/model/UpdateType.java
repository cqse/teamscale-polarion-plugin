package com.teamscale.polarion.plugin.model;

/**
 * Marks a work item change as a work item update or deletion. Currently, the deletion type is only
 * used when work items are moved to the recycle bin, since Polarion does not provide a way to query
 * permanently deleted items. For detecting permantently deleted items, Teamscale has to use the
 * list of all existing items (even the unchanged ones) and do its own diff, that's why we add that
 * list to the json response.
 */
public enum UpdateType {
  UPDATED,
  DELETED
}
