package com.teamscale.polarion.plugin.model;

import java.util.Collection;

/** This class wraps the response content to be serialized to Json. */
public class Response {

  /** All items present (not deleted) in the latest revision of the proj/folder/doc */
  private final Collection<String> allItemsIds;

  private final ResponseType responseType;

  /**
   * Following two fields will tell in the response the revision numbers of the response scope.
   * Client then should take them and build another request if it's a partial response.
   */
  private final String fromRevision;

  private final String toRevision;

  /** All items that have changed in the proj/folder/doc given (lastUpdate, endRevision] */
  private final Collection<WorkItemForJson> workItems;

  /**
   * Clients can detected deleted items by running a diff as follows: 1) build a set (A) of items
   * known to the client before request 2) Request and take the set of items (B) returned in the
   * response field 'allItemsIds' 3) Perform (A) - (B). If the result is empty, no items were
   * deleted. If the result is not empty, the remaining items are the deleted items since lastUpdate
   * revision. Note that (B) can have more items than (A) if new items were created since lastUpdate
   * revision.
   */
  public Response(
      Collection<String> allItemsIds,
      Collection<WorkItemForJson> workItems,
      ResponseType responseType,
      String fromRevision,
      String toRevision) {
    this.allItemsIds = allItemsIds;
    this.workItems = workItems;
    this.responseType = responseType;
    this.fromRevision = fromRevision;
    this.toRevision = toRevision;
  }

  public Collection<String> getAllItemsIds() {
    return allItemsIds;
  }

  public Collection<WorkItemForJson> getWorkItems() {
    return workItems;
  }

  public ResponseType getResponseType() {
    return responseType;
  }

  public String getFromRevision() {
    return fromRevision;
  }

  public String getToRevision() {
    return toRevision;
  }
}
