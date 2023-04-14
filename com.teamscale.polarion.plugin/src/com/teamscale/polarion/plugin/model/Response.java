package com.teamscale.polarion.plugin.model;

import java.util.Collection;

/** This class wraps the response content to be serialized to Json. */
public class Response {

  // All items present (not deleted) in the latest revision of the proj/folder/doc
  Collection<String> allItemsIds;

  // All items that have changed in the proj/folder/doc given (lastUpdate, endRevision]
  Collection<WorkItemForJson> workItems;

  /* Clients can detected deleted items by running a diff as follows:
   * 1) build a set (A) of items known to the client before request
   * 2) Request and take the set of items (B) returned in the response field 'allItemsIds'
   * 3) Perform (A) - (B). If the result is empty, no items were deleted.
   * If the result is not empty, the remaining items are the deleted items since
   * lastUpdate revision. Note that (B) can have more items than (A) if new items
   * were created since lastUpdate revision.
   *  */

  public Collection<String> getAllItemsIds() {
    return allItemsIds;
  }

  public void setAllItemsIds(Collection<String> allItemsIds) {
    this.allItemsIds = allItemsIds;
  }

  public Collection<WorkItemForJson> getWorkItems() {
    return workItems;
  }

  public void setAllWorkItemsForJson(Collection<WorkItemForJson> workItems) {
    this.workItems = workItems;
  }
}
