package com.teamscale.polarion.plugin.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class represents the final WorkItem object to be serialized to json which then goes in the
 * response body.
 */
public class WorkItemForJson {

  private String id;
  private String uri;
  private UpdateType updateType;
  private String revision;
  private String description;
  private String created; // date-time format, ex: '1970-01-01T00:00:00Z'
  private String dueDate; // date-time format
  private List<String> hyperLinks;
  private String initialEstimate;
  private String outlineNumber;
  private String plannedEnd; // date-time format
  private String plannedStart; // date-time format
  private String[] plannedIn;
  private String priority;
  private String remainingEstimate;
  private String resolution;
  private String resolvedOn; // date-time format
  private String severity;
  private String status;
  private String timeSpent;
  private String title;
  private String type;
  private String updated; // date-time format
  private String moduleId;
  private String moduleTitle;
  private String moduleFolder;
  private String projectId;
  private Map<String, Object> customFields;
  private List<String> assignees;
  private List<String> attachments;
  private String author;
  private List<String> categories;
  private String[] comments;
  private List<LinkedWorkItem> linkedWorkItems;
  private List<String> watchers;

  private Collection<WorkItemChange> workItemChanges;

  public WorkItemForJson(String id, String uri) {
    this.id = id;
    this.uri = uri;
  }

  public WorkItemForJson(String id, String uri, UpdateType updateType) {
    this.id = id;
    this.uri = uri;
    this.updateType = updateType;
  }

  public String getId() {
    return id;
  }

  public void setId(String workItemId) {
    this.id = workItemId;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public UpdateType getUpdateType() {
    return updateType;
  }

  public void setUpdateType(UpdateType updateType) {
    this.updateType = updateType;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }

  public List<String> getHyperLinks() {
    return hyperLinks;
  }

  public void setHyperLinks(List<String> hyperLinks) {
    this.hyperLinks = hyperLinks;
  }

  public String getInitialEstimate() {
    return initialEstimate;
  }

  public void setInitialEstimate(String initialEstimate) {
    this.initialEstimate = initialEstimate;
  }

  public String getOutlineNumber() {
    return outlineNumber;
  }

  public void setOutlineNumber(String outlineNumber) {
    this.outlineNumber = outlineNumber;
  }

  public String getPlannedEnd() {
    return plannedEnd;
  }

  public void setPlannedEnd(String plannedEnd) {
    this.plannedEnd = plannedEnd;
  }

  public String getPlannedStart() {
    return plannedStart;
  }

  public void setPlannedStart(String plannedStart) {
    this.plannedStart = plannedStart;
  }

  public String[] getPlannedIn() {
    return plannedIn;
  }

  public void setPlannedIn(String[] plannedIn) {
    this.plannedIn = plannedIn;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getRemainingEstimate() {
    return remainingEstimate;
  }

  public void setRemainingEstimate(String remainingEstimate) {
    this.remainingEstimate = remainingEstimate;
  }

  public String getResolution() {
    return resolution;
  }

  public void setResolution(String resolution) {
    this.resolution = resolution;
  }

  public String getResolvedOn() {
    return resolvedOn;
  }

  public void setResolvedOn(String resolvedOn) {
    this.resolvedOn = resolvedOn;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getTimeSpent() {
    return timeSpent;
  }

  public void setTimeSpent(String timeSpent) {
    this.timeSpent = timeSpent;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUpdated() {
    return updated;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

  public Map<String, Object> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, Object> customFields) {
    this.customFields = customFields;
  }

  public List<String> getAssignees() {
    return assignees;
  }

  public void setAssignees(List<String> assignees) {
    this.assignees = assignees;
  }

  public List<String> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<String> attachments) {
    this.attachments = attachments;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public String[] getComments() {
    return comments;
  }

  public void setComments(String[] comments) {
    this.comments = comments;
  }

  /**
   * It's ok to return collection fields as null here rather than empty lists since null fields will
   * not be serialized into json (intentionally) rather than be serialized as empty fields. Side
   * effect: if client objects need to process this list, then they need to do a null check before
   * using it to avoid NPE.
   */
  public List<LinkedWorkItem> getLinkedWorkItems() {
    return linkedWorkItems;
  }

  public void setLinkedWorkItems(List<LinkedWorkItem> linkedWorkItems) {
    this.linkedWorkItems = linkedWorkItems;
  }

  /**
   * It'll add a one or more new LinkedWorkItem objects even if the list of linked items is
   * empty/null
   */
  public void addAllLinkedWorkItems(List<LinkedWorkItem> newLinkedWorkItems) {
    if (this.linkedWorkItems == null) {
      this.linkedWorkItems = newLinkedWorkItems;
    } else {
      this.linkedWorkItems.addAll(newLinkedWorkItems);
    }
  }

  public String getModuleId() {
    return moduleId;
  }

  public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
  }

  public String getModuleTitle() {
    return moduleTitle;
  }

  public void setModuleTitle(String moduleTitle) {
    this.moduleTitle = moduleTitle;
  }

  public String getModuleFolder() {
    return moduleFolder;
  }

  public void setModuleFolder(String moduleFolder) {
    this.moduleFolder = moduleFolder;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public List<String> getWatchers() {
    return watchers;
  }

  public void setWatchers(List<String> watchers) {
    this.watchers = watchers;
  }

  public Collection<WorkItemChange> getWorkItemChanges() {
    return workItemChanges;
  }

  public void setWorkItemChanges(Collection<WorkItemChange> workItemChanges) {
    this.workItemChanges = workItemChanges;
  }

  /**
   * It'll add a single workItemChange object even if the list of work item changes is empty/null
   */
  public void addWorkItemChange(WorkItemChange workItemChange) {
    if (workItemChanges == null) {
      workItemChanges = new ArrayList<>();
    }
    workItemChanges.add(workItemChange);
  }
}
