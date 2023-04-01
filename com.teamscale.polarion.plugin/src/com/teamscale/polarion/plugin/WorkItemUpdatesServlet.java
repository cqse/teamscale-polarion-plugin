package com.teamscale.polarion.plugin;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.diff.IChange;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.PermissionDeniedException;
import com.polarion.platform.service.repository.AccessDeniedException;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemFieldDiff;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is the servlet that represents the endpoint for the Teamscale Polarion plugin. Its main job
 * is to return a json object representing updates on work items of a particular document, in a
 * given folder (space) and project.
 *
 * @author Bruno da Silva
 */
public class WorkItemUpdatesServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  
  private ITrackerService trackerService =
          (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);
  
  private IModule module;
  
  private String version;
  
  /* (non-Javadoc)
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
      throws ServletException, IOException {

    String projId = (String) req.getAttribute("project");
    String spaceId = (String) req.getAttribute("space");
    String docId = (String) req.getAttribute("document");

    // base revision # for the request
    String lastUpdateRev = req.getParameter("lastUpdate");

    // Example of how the following arrays should be passed in the URL
    // includedWorkItemTypes=testcase&includedWorkItemTypes=requirement
    // That's how the Java servlet API gets String arrays in the URL
    String[] workItemTypes = req.getParameterValues("includedWorkItemTypes");

    // List of work item custom fields that should be included in the result.
    // If empty, no custom fields should be present.
    String[] includeCustomFields = req.getParameterValues("includedWorkItemCustomFields");

    // List of possible work item link roles that should be included in the result.
    // If empty, no work item links should be included.
    String[] includeLinkRoles = req.getParameterValues("includedWorkItemLinkRoles");
    
    // For experimentation. TODO: Remove the following flag
    version = (String)req.getAttribute("version") != null ? (String)req.getAttribute("version") : "v1";
    
    this.getServletContext().log("[Teamscale Polarion Plugin] Query strings: ");
    this.getServletContext().log("[Teamscale Polarion Plugin] lastUpdate: " + lastUpdateRev);
    this.getServletContext()
        .log("[Teamscale Polarion Plugin] includedWorkItemTypes: " + workItemTypes);
    this.getServletContext()
        .log("[Teamscale Polarion Plugin] includedWorkItemCustomFields: " + includeCustomFields);
    this.getServletContext()
        .log("[Teamscale Polarion Plugin] includedWorkItemLinkRoles: " + includeLinkRoles);

    try {
      // To prevent SQL injection issues
      // Check if the request params are valid IDs before putting them into the SQL query
      if (validateParameters(projId, spaceId, docId)) {
        ArrayList<WorkItemForJson> changes = new ArrayList<WorkItemForJson>();
        if (!validateLastUpdateString(lastUpdateRev)) {
          // Rather than raising an exception and sending an error response,
          // here we assume all the changes should be returned if lastUpdateRev is absent.
          lastUpdateRev = "0";
        }
        changes =
            retrieveChanges(
                projId,
                spaceId,
                docId,
                lastUpdateRev,
                workItemTypes,
                includeCustomFields,
                includeLinkRoles);
        sendResponse(changes, res);
        this.getServletContext().log("[Teamscale Polarion Plugin] Successful response sent");
      } else {
        this.getServletContext()
            .log("[Teamscale Polarion Plugin] Invalid conbination of projectId/folderId/documentId");
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
      }
    } catch (PermissionDeniedException permissionDenied) {
    		this.getServletContext()
        .log("[Teamscale Polarion Plugin] Permission denied raised by Polarion");
    		this.getServletContext().log(permissionDenied.getMessage());
    		res.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    catch (AccessDeniedException accessDenied) {
    		this.getServletContext()
        .log("[Teamscale Polarion Plugin] Access denied raised by Polarion");  		
    		this.getServletContext().log(accessDenied.getMessage());
    		res.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  private static void sendResponse(ArrayList<WorkItemForJson> result, HttpServletResponse resp)
      throws ServletException, IOException {
    Gson gson = new Gson();
    String jsonResult = gson.toJson(result);
    resp.setContentType("application/json");
    PrintWriter out = resp.getWriter();
    out.print(jsonResult);
  }

  private static String buildSqlQuery(
      String projId, String spaceId, String docId, String lastUpdate, String[] workItemTypes) {

    StringBuilder sqlQuery = new StringBuilder("select * from WORKITEM WI ");
    sqlQuery.append("inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI ");
    sqlQuery.append("inner join MODULE M on WI.FK_URI_MODULE = M.C_URI ");
    sqlQuery.append("where P.C_ID = '" + projId + "'");
    sqlQuery.append(" and M.C_ID = '" + docId + "'");
    sqlQuery.append(" and M.C_MODULEFOLDER = '" + spaceId + "'");
    sqlQuery.append(" and WI.C_REV > " + lastUpdate);
    sqlQuery.append(getWorkItemTypesAndClause(workItemTypes));

    return sqlQuery.toString();
  }

  /** If empty, work items of all types should be included. * */
  private static String getWorkItemTypesAndClause(String[] workItemTypes) {
    StringBuilder andClause = new StringBuilder("");
    if (workItemTypes != null && workItemTypes.length > 0) {
      andClause.append(" and WI.C_TYPE in (");
      for (int i = 0; i < workItemTypes.length; i++) {
        if (workItemTypes[i] != null && !workItemTypes[i].isBlank()) {
          andClause.append("'" + workItemTypes[i] + "',");
        }
      }
      if (andClause.toString().endsWith(",")) {
        andClause.deleteCharAt(andClause.length() - 1);
      }
      andClause.append(")");
    }
    return andClause.toString();
  }

  private static boolean validateLastUpdateString(String lastUpdate) {
    if (lastUpdate != null) {
      Pattern pattern = Pattern.compile("\\d+");
      Matcher matcher = pattern.matcher(lastUpdate);
      return matcher.matches();
    }
    return false;
  }

  private ArrayList<WorkItemForJson> retrieveChanges(
      String projId,
      String spaceId,
      String docId,
      String lastUpdate,
      String[] workItemTypes,
      String[] includeCustomFields,
      String[] includeLinkRoles) {

    String sqlQuery = buildSqlQuery(projId, spaceId, docId, lastUpdate, workItemTypes);
    ArrayList<WorkItemForJson> changes = new ArrayList<WorkItemForJson>();

    IDataService dataService = trackerService.getDataService();
    IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);
    WorkItemForJson workItemForJson;
    
    long startTime = System.currentTimeMillis();
    
    for (IWorkItem workItem : workItems) {
    		// This is because WIs moved to the trash can are still in the Polarion WI table we query
    	if (wasMovedToRecycleBin(workItem)) {
    			workItemForJson = buildDeletedWorkItemForJson(workItem);
    	} else {
    			workItemForJson =
    		          processHistory(workItem, dataService, lastUpdate, includeCustomFields, includeLinkRoles);
    	}
      changes.add(workItemForJson);
    }
    // changes.addAll(buildDeletedList(lastUpdate));
    long endTime = System.currentTimeMillis();
    System.out.println(version+" Took " + (endTime - startTime) + " milliseconds");
    return changes;
  }
  
  /** 
   *  In Polarion, WIs in the recycle bin will still come in the SQL query, as 
   *  in the database level they're still related to the module.
   *  However, the following API method exclude them and consider them as items
   *  NOT contained in the module.
   *  **/
  private boolean wasMovedToRecycleBin(IWorkItem workItem) {
  		return !module.containsWorkItem(workItem);
  }
  
  private WorkItemForJson buildDeletedWorkItemForJson(IWorkItem workItem) {
  		return new WorkItemForJson(workItem.getId(), 
							Utils.UpdateType.DELETED, workItem.getLastRevision());
  }
  
  //TODO: Need to debug why the compareRevisions throws an exception
  //Question: Could TS do the diff since it's supposed to have the objects
  // before until last update?
//  private List<WorkItemForJson> buildDeletedList(String lastUpdate) {
//  		List<WorkItemForJson> deletedList = new ArrayList<WorkItemForJson>();
//  		try {
//  				if (module == null)
//  						return deletedList;
//					// From Polarion doc: pass null for HEAD/current
//					IBaselineDiff baselineDiff = module.compareRevisions(lastUpdate, module.getLastRevision());
//					ITypeInfo typeInfo = baselineDiff.getWorkItemDiff();
//					// The list of objects deleted between the baselines, the objects are taken from 
//					// the beginning revision.
//					for (Object item: typeInfo.getDeletedObjects()) {
//							if (item instanceof IWorkItem) {
//									deletedList.add(buildDeletedWorkItemForJson((IWorkItem)item));
//							}
//					}
//					return deletedList;
//			} catch (UnresolvableObjectException unresolvable) {
//					// if module did not exist in one of given revisions
//					unresolvable.printStackTrace();
//					return deletedList;
//			} 				
//  }

  private WorkItemForJson processHistory(
      IWorkItem workItem,
      IDataService dataService,
      String lastUpdate,
      String[] includeCustomFields,
      String[] includeLinkRoles) {
    WorkItemForJson workItemForJson =
        Utils.castWorkItem(workItem, includeCustomFields, includeLinkRoles);
    
    // For experimentation. TODO: Remove this if section that checks for v2
    if (version !=null && version.equals("v2")) {  		
    		IDiffManager diffManager = dataService.getDiffManager();
    		//No fields to ignore, no fields ordering, and does not include empty field diffs
    		IChange[] changes = diffManager.generateHistory(workItem, new HashSet<String>(), new String[0], false);
    		if (changes != null && changes.length > 0) {
            Collection<WorkItemChange> workItemChanges =
                    collectWorkItemChanges(changes, lastUpdate);
                workItemForJson.setWorkItemChanges(workItemChanges);
            
                // ORDERED??
            workItemForJson.setRevision(changes[changes.length - 1].getRevision());
    		} else {
    				// no history
    		}
    		return workItemForJson;
    }
    
    IPObjectList<IWorkItem> workItemHistory = dataService.getObjectHistory(workItem);
    if (workItemHistory != null) {
      if (workItemHistory.size() == 1 && workItemHistory.get(0) != null) {
        // No changes in history when size == 1 (the WI remains as created)
        workItemForJson.setRevision(workItemHistory.get(0).getRevision());
      } else if (workItemHistory.size() > 1) {
      		/**
      		 * From Polarion JavaDoc: "The history list is sorted from the oldest (first) to the newest
      		 * (last)."
      		 * https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)
      		 * Then, we get the last one from the history as the current revision
      		 */
      	List<IWorkItem> reducedWorkItemHistory = cutDownWorkItemHistory(workItemHistory, lastUpdate);
        IDiffManager diffManager = dataService.getDiffManager();
        Collection<WorkItemChange> workItemChanges =
            collectWorkItemChanges(reducedWorkItemHistory, diffManager, lastUpdate);
        workItemForJson.setWorkItemChanges(workItemChanges);

        workItemForJson.setRevision(workItemHistory.get(workItemHistory.size() - 1).getRevision());
      } else {
        /**
         * No history. Empty list. From Polarion JavaDoc: "An empty list is returned if the object
         * does not support history retrieval."
         * "https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)"
         */
      }
    }
    return workItemForJson;
  }

  private Collection<WorkItemChange> collectWorkItemChanges(
      List<IWorkItem> workItemHistory, IDiffManager diffManager, String lastUpdate) {
    Collection<WorkItemChange> workItemChanges = new ArrayList<WorkItemChange>();
    int index = 0;
    int next = 1;
    while (next < workItemHistory.size()) {
      if (Long.valueOf(workItemHistory.get(next).getRevision()) > Long.valueOf(lastUpdate)) {
        IFieldDiff[] fieldDiffs =
            diffManager.generateDiff(
                workItemHistory.get(index), workItemHistory.get(next), new HashSet<String>());
        WorkItemChange fieldChangesToAdd =
            collectFieldChanges(fieldDiffs, workItemHistory.get(next).getRevision());
        if (fieldChangesToAdd != null) {
          workItemChanges.add(fieldChangesToAdd);
        }
      }
      index++;
      next++;
    }
    return workItemChanges;
  }
  
  private Collection<WorkItemChange> collectWorkItemChanges(
  				IChange[] changes, String lastUpdate) {
  		Collection<WorkItemChange> workItemChanges = new ArrayList<WorkItemChange>();

  		for (int index = 0; index < changes.length; index++) {
  				if (Long.valueOf(changes[index].getRevision()) > Long.valueOf(lastUpdate)) {
  						IFieldDiff[] fieldDiffs = changes[index].getDiffs();
  						WorkItemChange fieldChangesToAdd =
  										collectFieldChanges(fieldDiffs, changes[index].getRevision());
  						if (fieldChangesToAdd != null) {
  								workItemChanges.add(fieldChangesToAdd);
  						}  						
  				}
  		}

  		return workItemChanges;
  } 

  private WorkItemChange collectFieldChanges(IFieldDiff[] fieldDiffs, String revision) {
    WorkItemChange workItemChange = new WorkItemChange(revision);
    
    if (fieldDiffs == null) return null;
    
    for (IFieldDiff fieldDiff : fieldDiffs) {
      if (fieldDiff.isCollection()) {
        collectFieldDiffAsCollection(workItemChange, fieldDiff);
      } else {
        WorkItemFieldDiff fieldChange =
            new WorkItemFieldDiff(fieldDiff.getFieldName(), null, null, null, null);
        fieldChange.setFieldValueBefore(Utils.castFieldValueToString(fieldDiff.getBefore()));
        fieldChange.setFieldValueAfter(Utils.castFieldValueToString(fieldDiff.getAfter()));
        workItemChange.addFieldChange(fieldChange);
      }
    }
    return workItemChange;
  }

  private void collectFieldDiffAsCollection(WorkItemChange workItemChange, IFieldDiff fieldDiff) {
    // Polarion returns unparameterized Collections for these two methods
    Collection added = fieldDiff.getAdded();
    Collection removed = fieldDiff.getRemoved();
    if (added != null && added.size() > 0) {
      WorkItemFieldDiff fieldChange =
          new WorkItemFieldDiff(fieldDiff.getFieldName(), null, null, null, null);
      // We check if the collection is hyperlink list first since they're not
      // convertible into IPObjectList. So, we treat them separately.
      if (Utils.isCollectionHyperlinkStructList(added)) {
        fieldChange.setElementsAdded(Utils.castHyperlinksToStrArray(added));
        // Then we check if they're ILiknedWorkItemStruc, because, again,
        // Polarion treats those 'struct' objects differently thank regular
        // IPObjects
      } else if (Utils.isCollectionLinkedWorkItemStructList(added)) {
        fieldChange.setElementsAdded(Utils.castLinkedWorkItemsToStrArray(added));
      } else if (Utils.isCollectionApprovalStructList(added)) {
        fieldChange.setElementsAdded(Utils.castApprovalsToStrArray(added));
      } else {
        try {
          fieldChange.setElementsAdded(Utils.castCollectionToStrArray((List<IPObject>) added));
        } catch (ClassCastException ex) {
          // For now, when an added element/value is not among the ones we're supposed
          // to support in Teamscale, we simply add as an empty string array.
          // Alternatively, we could ignore the field as a change
          // (skip the field from the json output)
          fieldChange.setElementsAdded(new String[] {""});
        }
      }
      workItemChange.addFieldChange(fieldChange);
    }
    if (removed != null && removed.size() > 0) {
      WorkItemFieldDiff fieldChange =
          new WorkItemFieldDiff(fieldDiff.getFieldName(), null, null, null, null);
      if (Utils.isCollectionHyperlinkStructList(removed)) {
        fieldChange.setElementsRemoved(Utils.castHyperlinksToStrArray(removed));
      } else if (Utils.isCollectionLinkedWorkItemStructList(removed)) {
        fieldChange.setElementsRemoved(Utils.castLinkedWorkItemsToStrArray(removed));
      } else if (Utils.isCollectionApprovalStructList(removed)) {
        fieldChange.setElementsRemoved(Utils.castApprovalsToStrArray(removed));
      } else {
        try {
          fieldChange.setElementsRemoved(Utils.castCollectionToStrArray((List<IPObject>) removed));
        } catch (ClassCastException ex) {
          // For now, when a removed element/value is not among the ones we're supposed
          // to support in Teamscale, we simply add as an empty string array.
          // Alternatively, we could ignore the field as a change
          // (skip the field from the output)
          fieldChange.setElementsRemoved(new String[] {""});
        }
      }
      workItemChange.addFieldChange(fieldChange);
    }
  }
  
  /** Binary search to cut down the search space since the list is ordered **/
  private List<IWorkItem> cutDownWorkItemHistory(IPObjectList<IWorkItem> workItemHistory, 
  				String lastUpdate) {
  		
  		List<IWorkItem> reducedList = new ArrayList<IWorkItem>();
  		
      int left = 0;
      int right = workItemHistory.size() - 1;
      int index = -1;  		
      
      // find the index of the first WI with revision >= lastUpdate
      while (left <= right) {
          int mid = (left + right) / 2;
          if (Long.valueOf(workItemHistory.get(mid).getRevision()) < Long.valueOf(lastUpdate)) {
              left = mid + 1;
          } else {
              index = mid;
              right = mid - 1;
          }
      }
      
      for (int i = index; i < workItemHistory.size(); i++) {
      		if (Long.valueOf(workItemHistory.get(index).getRevision()) < Long.valueOf(lastUpdate)) {
      				reducedList.add(workItemHistory.get(index));
          }
      }  
      return reducedList;
  }

  private boolean validateParameters(String projectId, String space, String doc) {
    // Needs to be executed in this order. Space validation only runs after projectId is validated.
    // DocId is validated only if projectId and SpaceId are validated.
    return validateProjectId(projectId)
        && validateSpaceId(projectId, space)
        && validateDocumentId(projectId, space, doc);
  }

  private boolean validateProjectId(String projectId) {
    try {
      IProject projObj = trackerService.getProjectsService().getProject(projectId);

      this.getServletContext()
          .log("[Teamscale Polarion Plugin] Attempting to read projectID: " + projObj.getId());
      this.getServletContext()
          .log("[Teamscale Polarion Plugin] Project name: " + projObj.getName());

      return true;

    } catch (UnresolvableObjectException exception) {
      this.getServletContext()
          .log("[Teamscale Polarion Plugin] Not possible to resolve project with id: " + projectId);
      return false;
    }
  }

  private boolean validateSpaceId(String projId, String spaceId) {
    this.getServletContext().log("Attempting to read folder: " + spaceId);
    if (trackerService.getFolderManager().existFolder(projId, spaceId)) {
      this.getServletContext()
          .log("[Teamscale Polarion Plugin] Attempting to read folder: " + spaceId);
      return true;
    }
    this.getServletContext()
        .log("[Teamscale Polarion Plugin] Not possible to find folder with id: " + spaceId);
    return false;
  }

  /** This helper method should be called after validating the space (aka folder) * */
  private boolean validateDocumentId(String projId, String space, String docId) {
    // Haven't found in the Polarion Java API a straightforward way to validate a docId.
    // That's why the current solution has to select all documents in the given
    // valid space (folder) then loops through the documents to validate the given docId
    IDataService dataService = trackerService.getDataService();
    String query =
        "select * from MODULE M "
            + "inner join PROJECT P on M.FK_URI_PROJECT = P.C_URI "
            + "and P.C_ID = '"
            + projId
            + "' "
            + "and M.C_MODULEFOLDER = '"
            + space
            + "'";
    IPObjectList<IModule> modules = dataService.sqlSearch(query);
    for (IModule module : modules) {
      if (module.getId().equals(docId)) {
      	this.module = module;
        this.getServletContext()
            .log("[Teamscale Polarion Plugin] Attempting to read document: " + docId);
        return true;
      }
    }
    this.getServletContext()
        .log("[Teamscale Polarion Plugin] Not possible to find document with id: " + docId);
    return false;
  }
}
