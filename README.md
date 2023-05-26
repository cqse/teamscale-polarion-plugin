# Teamscale Polarion Plugin

With the purpose of processing work item history, the Teamscale Polarion Plugin aims to provide a more efficient communication between Polarion and Teamscale in comparison with Polarion provided SOAP and REST APIs. At the moment, the most recent version of Polarion REST API does not support work item history. While the SOAP API does provide support for work item history, it has not been an efficient soluton for Teamscale use cases.

This Polarion plugin follows the Servlet extension mechanism in Polarion. Find more info on how this Polarion plugin mechanism works [here](https://almdemo.polarion.com/polarion/sdk/doc/sdk.pdf).

Other useful Polarion documentation:
 - [SDK page](https://almdemo.polarion.com/polarion/sdk/index.html)
 - [Java API](https://almdemo.polarion.com/polarion/sdk/doc/javadoc/index.html)
 - [Database](https://almdemo.polarion.com/polarion/sdk/doc/database/index.html)

## How to install the plugin

First of all, the plugin needs to be deployed in an existing Polarion instance. It is out of the scope of this documentation to provide instructions on how to install Polarion.

Once Polarion is installed and running properly, there are two steps to deploy the plugin:
   1. Drop the plugin jar file in the specific Polarion plugins folder. The plugins folder location depends on your installation (e.g., /opt/polarion/polarion/plugins). If Polarion is already running when you drop the plugin jar, restarting the instance is necessary. See next step.
   2. From Polarion documentation: _Servlets loaded by Polarion are cached in: [Polarion_Home]/data/workspace/.config. If this folder is not deleted before deploying a
servlet extension (plugin) and restarting Polarion, then either the servlets will not be properly loaded, or the old ones will be loaded._ So, delete that caching folder and restart Polarion.

Test if the plugin is running: go to the browser and access \<domain\>/polarion/api/teamscale/is-alive
  - It should return an http 200 with the string _"Alive! I'm ready to crunch some work items!"_
  - Otherwise, the plugin is not loaded or Polarion is down.

## How to build the dev environment and run the Plugin in dev/debug mode

See Section 4.3 (Workspace Preparation) in this [Polarion SDK documentation](https://almdemo.polarion.com/polarion/sdk/doc/sdk.pdf).

Once the project is successfuly configured and building, follow instructions on Section 4.5 (Execution from Workspace).

With that, you can run the local Polarion instance with the plugin deployed, and use it for debugging and development purposes.

## Assumptions, Design Decisions, and Design Implications Inherited from Polarion

**Request path parameters:**
 
 All required
 - projectId
 - spaceId (aka folderId)
 - moduleId (aka documentId)
 
**Request query parameters:**

All optional
 - lastUpdate: revision number that indicates the last update known by the client, so the server should look for changes after that. If not included in the request the server will consider changes after revision 0.
 - endRevision: revision number that indicates the end revision the server should look for. All changes up to that revision (included). If not included in the request the server will consider changes up to the latest revision.
 - includedWorkItemTypes: List of possible work item types to be included in the result. If empty, all items of all types should be included.
 - includedWorkItemCustomFields: List of work item custom fields that should be included in the result. If empty, no custom fields should be present.
 - includedWorkItemLinkRoles: List of possible work item link role Ids that should be included in the result. If empty, no work item links should be included.

**Revision numbers:** In Polarion, changes on documents and work items are version-controled by and embedded SVN engine. Therefore:
 - revision numbers are global and unique across projects of the same Polarion instance/installation
 - revision numbers are sequential and always positive
 - revision numbers are unique and grow by 1 for every change (aka transaction in SVN lingo)
   - note: in some cases multiple changes are mapped to a single revision number, for instance, when you change multiple work item fields at once in a single save.
 - More info about [SVN revision numbers](https://svnbook.red-bean.com/en/1.7/svn-book.html#svn.basic.in-action.revs)

**Dealing with work item links:** Work Item links deserve some special attention. [Links can be configured](https://docs.plm.automation.siemens.com/content/polarion/20/help/en_US/user_and_administration_help/administrators_guide/configure_work_items/configure_work_item_linking.html) in different ways in diffent projects. Therefore, user facing link names may differ from project to project. Also, in Polarion, links are bi-directional. So if A links B, B also links A. For instance, for the parent-child link, if A _has-parent_ B, B _is-parent-of_ A. Note the _forward_ and _backward_ link names are different even though they are the same _link role_. Therefore, Polarion will tight different user-facing link names under the same link role id (in this example the link role id is _parent_). The plugin will always refer to links by its id. Therefore, in the json output the plugin generates, A would contain a link identified as _parent_ and so would B (even thouth in the Polarion document and work item UIs, they would display as _has-parent_ or _is-parent-of_).

**Work item link changes:** Polarion does not generate a change/field diff for the opposite end of the link (the backward link). Still, this an interesting information to clients, so they can act upon link changes (since both ends are interfeered). Therefore, the plugin creates those field diffs for the backward links on top of what Polarion returns as changes for the forward links. In the example above, Polarion returns a field diff representing A links B (using the _parent_ link role), besides creating a change that represents that link from A to B, the plugin also creates a change (on the json output only, not in Polarion database) to represent a work item change in B (as B also links A via the _parent_ role).

**Items moved to the recycle bin (soft deletion):** In Polarion, users can soft delete items by moving them to the [recycle bin](https://docs.plm.automation.siemens.com/content/polarion/19.3/help/en_US/user_and_administration_help/user_guide/work_with_documents/work_items_in_documents/work_item_recycle_bin.html). These items do not show up in documents (only if you open the recycle bin UI) but they're still valid items when we query the work item table in Polarion database (and consequently they're still related to the document in the database). As of now, the plugin only detects work item deletion if items are in the recycle bin. For [hard deletions](https://docs.plm.automation.siemens.com/content/polarion/20/help/en_US/user_and_administration_help/user_guide/work_items/work_item_actions/delete_work_items.html), the plugin is not currently able to detectem them. See next.

**Items deleted permanently:** We have not found a way to query [permanently deleted items](https://docs.plm.automation.siemens.com/content/polarion/20/help/en_US/user_and_administration_help/user_guide/work_items/work_item_actions/delete_work_items.html) utilizing the Polarion Java API. Therefore, the current solution is to leave that job to the client side. For that reason, every response the plugin sends back to the client contains a list of all work item ids that are valid (not deleted) at the moment. On the client side, applications can build some logic around that. For instance:
 - Client builds a set (A) of item ids known to the client before request
 - Client sends request and takes the set (B) of all item ids returned in the response field 'allItemsIds' (all items not deleted at the moment)
 - Client performs diff (A) - (B). If the result is empty, no items were deleted.
   - If the result is not empty, the remaining items are the deleted items since lastUpdate revision. 
   - Note that (B) can have more items than (A) if new items were created since lastUpdate revision.

**Thread Safety:**
The solution is thread safe. This Polarion plugin is implemented as a Java Servlet. Polarion utilizes the Apache Tomcat web container to run servlets. The Tomcat web container follows a multi-thread model, meaning that it creates a new thread per request directed to a any given servlet. Besides that, in the plugin, we do not utilize any shared data structures across different servlets from Polarion. Each request to the plugin servlet will run in a separate thread, which will query the database independently and work in its own data in memory.

## JSON Serialization
We currently use the opensource library [Gson](https://github.com/google/gson) which already comes available in the Polarion installation.

## Code formatting

We are following the Java coding style guidelines from Google.

And we use [this opensource tool](https://github.com/google/google-java-format) from Google locally in the dev workstation.

Also, we set up [this GitHub action](https://github.com/axel-op/googlejavaformat-action) to auto format the pushed code if need.

## Test Cases

The plugin implementation is tightly coupled with the Polarion data model and database, which makes the testability of the plugin really difficult. Automated tests would require a significant effort, so it's a TBD for now. In addition, the plugin code can be also tested on the client side using integration testing strategies.

Here some key test cases that we can do manually until we can automate them.

### WorkItemTypes
 - **Input:** includedWorkItemTypes=testcase
 - **Expected behavior/output:** The json output should include items of only that type

 - **Input:** <includedWorkItemTypes not passed in the request>
 - **Expected behavior/output:** The json output should include items of all types
 
### CustomFields
 - **Input:** includedWorkItemCustomFields=testCaseID&includedWorkItemCustomFields=testType
 - **Expected behavior/output:** All work items included in the response shall contain those two custom fields if the custom fields passed in the request are valid custom field Ids.
 
### LinkRoles
 - **Input:** includedWorkItemLinkRoles=parent&includedWorkItemLinkRoles=verifies
 - **Expected behavior/output:** For all the work items included in the result, there should be a field called linkedWorkItems that maps to an array. Each element of that array should contain two fields: the id of the work item it's linked to; and the linkRoleId that represents the link type (e.g., parent, verifies). Besides, all the work item changes related to links added/removed in the workItemChanges field should contain only links of those two types passed in the request (e.g., parent, verifies). Even if the work items were changed by adding/removing links of other types within the revision limits passed in the request, those changes should not show up in the response.
 
### Using the lastUpdate query parameter
 - **Input:** lastUpdate=0 or absent from the request
 - **Expected behavior/output:** All changes since revision 1.

 - **Input:** lastUpdate=1
 - **Expected behavior/output:** The response shall contain work items that were created or changed **after** revision 1 (note that doesn't include revision 1). Each work item that changed after revision 1 should contain in its field named workItemChanges an array containing its changes grouped by revision number in ascending order. For each revision, there should be an array of field changes (since it's possible to change multiple fields at once in some cases). Each element of such field change array shall contain the fieldName (which is unique) and the values before/after (if it's a simpple field) or values added/removed (if it's a collection field). If there are no changes after revision 1, then we should see an empty json object in the response. Additionally, the response should include a list of all work item ids currently valid (not deleted) at the latest revision for deletion check on the client side. 
 
### Using the endRevision query parameter
 - **Input:** endRevision=10
 - **Expected behavior/output:** The response shall contain work items that were created or changed **up to** revision 10. Each work item that changed up to revision 10 should contain in its field named workItemChanges an array containing its changes grouped by revision number in ascending order. For each revision, there should be an array of field changes (since it's possible to change multiple fields at once in some cases). Each element of such field change array shall contain the fieldName (which is unique) and the values before/after (if it's a simpple field) or values added/removed (if it's a collection field). If there are no changes after revision 1, then we should see an empty json object in the response. Additionally, the response should include a list of all work item ids currently valid (not deleted) at the latest revision for deletion check on the client side. 
 
### Using both the lastUpdate and endRevisionquery parameters
 - **Input:** lastUpdate=10&endRevision=10
 - **Expected behavior/output:** The response should be a 404. endRevision should be greater than lastUpdate.
 
 - **Input:** lastUpdate=50&endRevision=10
 - **Expected behavior/output:** The response should be a 404. endRevision should be greater than lastUpdate.
 
  - **Input:** lastUpdate=10&endRevision=20
  - **Expected behavior/output:** The response shall contain work items that were created or changed **after** revision 10 and **up to** (including) revision 20. The workItemChanges field should also only include changes that happened in revisions after 10 up to 20 (including). Note: work items that changed before lastUpdate and after endRevision, even though they're valid (not deleted) work items, they should not show up in the response. Additionally, regardless of the revision parameters, the response should include a list of all work item ids currently valid (not deleted) at the latest revision for deletion check on the client side. 

### Using both the lastUpdate and endRevisionquery parameters in combination with links changed between lastUpdate and endRevision

  - **Input:** lastUpdate=10&endRevision=20
  - **Expected behavior/output:** Link changes are trickier than other fields. All the links added/removed between revisions 10 and 20 should not be part of the response scope. The response shall contain work items that were created or changed **after** revision 10 and **up to** (including) revision 20. The current work item link state should be the one by revision 20. The workItemChanges field should also only include changes that happened in revisions after 10 up to 20 (including), considering links added/removed.
  
### Using both the lastUpdate and endRevisionquery parameters in combination with links changed after endRevision

  - **Input:** lastUpdate=10&endRevision=20
  - **Expected behavior/output:** If links were added/removed after the endRevision those changes, like any other changes, should not be part of the request/response scope. The response shall contain work items that were created or changed **after** revision 10 and **up to** (including) revision 20 without links added/removed after endRevision (since that represent their state up to revision 20).
 
### WorkItem Deletion as Moved to the Recycle Bin
 
  - **Input:** lastUpdate=10&endRevision=20
  - **Expected behavior/output:** If an item was moved to the recycle bin on revision 10 or earlier, then it should not be part of the response at all (in any state). If an item was moved to the recycle bin after revision 20, then it should not show as deleted and its changes (if there are changes) should be included in the response. If an item was moved to the recycle bin after revision 10 and up to revision 20 (including), then it should be part of the response and its updateType field should have the value DELETED along with the revision number when it was moved to the recyble bin. Note: Items in the recycle bin can still undergo changes. For instance, if any of their field values change, or their links, or even if their module changes id, it'll generate a new revision and changes will be tracked by Polarion. Therefore, the revision number showing in a DELETED item will either be the revision when item was moved to the recycle bin or the revision when item was lastly changed while in the recycle bin.
 
### WorkItem in the Recycle Bin is moved back to document
 
  - **Input:** lastUpdate=10&endRevision=20
  - **Expected behavior/output:** When an item moves to recycle bin, it does not generate an item history change. Therefore, the expected output should be all item changes between revisions 10 and 20 (including if changes happened while the item was in the recycle bin). If the item was moved back to the document from recycle bin after revision 20, then the response should show the item and its field values as of revision 20 (along with all changes that happened in its fields between revisions 10 and 20). Since the item is not currently in the recycle bin, and since Polarion does not generate a work item history change when items are moved to the recycle bin, even though the item was in the recycle bin in the requested revision window, the output will not show the item went to the recycle bin and back at some point. If this is required by the client, it needs to be tracked on the client side, currently.
  
### WorkItem Deletion (Permamently)
 
  - **Input:** lastUpdate=10&endRevision=20
  - **Expected behavior/output:** Regardless when items are deleted permanently, the response shall contain only existing items. If an item was deleted at any revision number, no matter the revision parameters are passed in the request, deleted items history is never visible, so we cannot query and return them in the response.

 
Please suggest other corner cases...
