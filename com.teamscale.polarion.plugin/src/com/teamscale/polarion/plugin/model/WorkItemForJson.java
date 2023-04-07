package com.teamscale.polarion.plugin.model;

import java.util.Collection;
import java.util.HashMap;

import com.teamscale.polarion.plugin.utils.Utils;
import com.teamscale.polarion.plugin.utils.Utils.UpdateType;

/**
 * This class represents the final WorkItem object to be serialized to json
 * which then goes in the response body (as json).
 * */
public class WorkItemForJson {

  private String id;
  private Utils.UpdateType updateType;
  private String revision;
  private String description;
  private String created; // date-time format, ex: '1970-01-01T00:00:00Z'
  private String dueDate; // date-time format
  private String[] hyperLinks;
  private String initialEstimate;
  private String outlineNumber;
  private String plannedEnd; // date-time format
  private String plannedStart; // date-time format
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
  private HashMap<String, Object> customFields;
  private String[] assignees;
  private String[] attachments;
  private String author;
  private String[] categories;
  private String[] comments;
  private LinkedWorkItem[] linkedWorkItems; // All links (in and out links)
  private String moduleId;
  private String projectId;
  private String[] watchers;

  private Collection<WorkItemChange> workItemChanges;

  public WorkItemForJson(String id) {
    this.id = id;
  }
  
  public WorkItemForJson(String id, Utils.UpdateType updateType, String revision) {
  		this.id = id;
  		this.updateType = updateType;
  		this.revision = revision;
  }

  public String getId() {
    return id;
  }

  public void setId(String workItemId) {
    this.id = workItemId;
  }
  
  public Utils.UpdateType getUpdateType() {
  		return updateType;
  }
  
  public void setUpdateType(Utils.UpdateType updateType) {
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

  public String[] getHyperLinks() {
    return this.hyperLinks;
  }

  public void setHyperLinks(String[] hyperLinks) {
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

  public HashMap<String, Object> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(HashMap<String, Object> customFields) {
    this.customFields = customFields;
  }

  public String[] getAssignees() {
    return assignees;
  }

  public void setAssignees(String[] assignees) {
    this.assignees = assignees;
  }

  public String[] getAttachments() {
    return attachments;
  }

  public void setAttachments(String[] attachments) {
    this.attachments = attachments;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String[] getCategories() {
    return categories;
  }

  public void setCategories(String[] categories) {
    this.categories = categories;
  }

  public String[] getComments() {
    return comments;
  }

  public void setComments(String[] comments) {
    this.comments = comments;
  }

  public LinkedWorkItem[] getLinkedWorkItems() {
    return linkedWorkItems;
  }

  public void setLinkedWorkItems(LinkedWorkItem[] linkedWorkItems) {
    this.linkedWorkItems = linkedWorkItems;
  }

  public String getModuleId() {
    return moduleId;
  }

  public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String[] getWatchers() {
    return watchers;
  }

  public void setWatchers(String[] watchers) {
    this.watchers = watchers;
  }

  public Collection<WorkItemChange> getWorkItemChanges() {
    return workItemChanges;
  }

  public void setWorkItemChanges(Collection<WorkItemChange> workItemChanges) {
    this.workItemChanges = workItemChanges;
  }
}
