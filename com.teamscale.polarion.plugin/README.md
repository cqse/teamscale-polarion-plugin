# Teamscale Polarion Plugin

This Polarion plugin follows the Servlet extension mechanism in Polarion. Find more info on how this Polarion plugin mechanism works [here](https://almdemo.polarion.com/polarion/sdk/doc/sdk.pdf).

Other useful Polarion documentation:
 - [SDK page](https://almdemo.polarion.com/polarion/sdk/index.html)
 - [Java API](https://almdemo.polarion.com/polarion/sdk/doc/javadoc/index.html)
 - [Database](https://almdemo.polarion.com/polarion/sdk/doc/database/index.html)

## How to install the plugin

TODO

## How to build the dev environment

TODO

## Assumptions, Design Decisions, or Design Implications Inherited from Polarion

**Request path parameters:**
 
 All required
 - projectId
 - spaceId (aka folderId)
 - moduleId (aka documentId)
 
**Request query parameters:**

All optional
 - lastUpdate: revision number that indicates the last update known by the client, so the server should look for changes after that.
 - endRevision: revision number that indicates the end revision this request should look for. All changes up to that revision (included).
 - includedWorkItemTypes: List of possible work item types to be included in the result. If empty, all items of all types should be included.
 - includedWorkItemCustomFields: List of work item custom fields that should be included in the result. If empty, no custom fields should be present.
 - includedWorkItemLinkRoles: List of possible work item link role Ids that should be included in the result. If empty, no work item links should be included.

**Revision numbers:** In Polarion, revision numbers are:
 - global and unique across projects of the same Polarion instance/installation
 - controlled by an embedded SVN engine
 - sequential and always positive
 - they grow by 1 for every change 
 - (note: in some cases multiple changes are mapped to a single revision number, for instance, when you change multiple work item fields at once in a single save).

TODO (there's much more to document here...)

## JSON Serialization
We currently use the opensource library [Gson](https://github.com/google/gson) which already comes available in the Polarion installation.

## Code formatting

We are following the Java coding style guidelines from Google.

And we use [this opensource tool](https://github.com/google/google-java-format) from Google locally in the dev workstation.

Also, we set up [this GitHub action](https://github.com/axel-op/googlejavaformat-action) to auto format the pushed code if need.

## Test Cases

The plugin implementation is tightly coupled with the Polarion data model and database, which makes the testability of the plugin really difficult. Automated tests would require a significant effort, so it's a TBD for now.

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

 TODO
 
### Using both the lastUpdate and endRevisionquery parameters

 TODO
 
### Using both the lastUpdate and endRevisionquery parameters in combination with a link changed after endRevision

 TODO
 
### Using both the lastUpdate and endRevisionquery parameters in combination with a link changed between lastUpdate and endRevision

 TODO
 
### WorkItem Deletion as Moved to the Recycle Bin
 
 TODO
 
### WorkItem Deletion (Permament)
 
TODO
 
### WorkItem Deletion (Permament) when deletion happened after endRevision
 
TODO
 
Please suggest other corner cases
