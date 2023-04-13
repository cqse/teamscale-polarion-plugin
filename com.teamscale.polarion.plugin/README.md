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

## Assumptions and Design Decisions

TODO

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
 - Input: includedWorkItemTypes=testcase
 - Expected behavior/output: The json output should include items of only that type
 
### CustomFields
 - Input: includedWorkItemCustomFields=testCaseID&includedWorkItemCustomFields=testType
 - Expected behavior/output: All work items included in the response should contain those two custom fields if the custom fields passed in the request are valid custom field Ids.
 

 