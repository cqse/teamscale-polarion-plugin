package com.teamscale.polarion.plugin;

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
import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemFieldDiff;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.Utils;

/**
 * This is the servlet that represents the endpoint for the Teamscale Polarion plugin.
 * Its main job is to return a json object representing updates on work items
 * of a particular document, in a given folder (space) and project.
 * 
 * @author Bruno da Silva
 */
public class WorkItemUpdatesServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {

		ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);

		String projId = (String) req.getAttribute("project");
		String spaceId = (String) req.getAttribute("space");
		String docId = (String) req.getAttribute("document");
		String lastUpdateRev = req.getParameter("lastUpdate"); //base revision # for the request
		
		// Example of how the following arrays should be passed in the URL
		 //includedWorkItemTypes=testcase&includedWorkItemTypes=requirement
		 //That's how the Java servlet API gets String arrays in the URL
		String[] workItemTypes = req.getParameterValues("includedWorkItemTypes"); 
		
		// List of work item custom fields that should be included in the result. 
		//If empty, no custom fields should be present.
		String[] includeCustomFields = req.getParameterValues("includedWorkItemCustomFields");
		
		//List of possible work item link roles that should be included in the result. 
		  //If empty, no work item links should be included.
		String[] includeLinkRoles = req.getParameterValues("includedWorkItemLinkRoles");
		
		//To prevent SQL injection issues
		 //Check if the request params are valid IDs before putting them into the SQL query
		if (validateParameters(trackerService, projId, spaceId, docId)) {
			ArrayList<WorkItemForJson> changes = new ArrayList<WorkItemForJson>();
			if (!validateLastUpdateString(lastUpdateRev)) {
				//Rather than raising an exception and sending an error response,
				//here we assume all the changes should be returned if lastUpdateRev is absent.
				lastUpdateRev = "0";
			}
			changes = retrieveChanges(trackerService, projId, spaceId, docId, lastUpdateRev, 
					workItemTypes, includeCustomFields, includeLinkRoles);
			sendResponse(changes, res);
		} else {
			res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
		}			
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}  
	
	private static void sendResponse(ArrayList<WorkItemForJson> result, HttpServletResponse resp) throws ServletException, IOException{
		Gson gson = new Gson();
		String jsonResult = gson.toJson(result);
		resp.setContentType("application/json");
		PrintWriter out = resp.getWriter();        
		out.print(jsonResult);		
	}
	
	private static String buildSqlQuery(String projId, String spaceId, String docId, 
			String lastUpdate, String[] workItemTypes) {
	
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
	
	/** If empty, work items of all types should be included. **/
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
				andClause.deleteCharAt(andClause.length()-1);
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
	
	private ArrayList<WorkItemForJson> retrieveChanges(ITrackerService trackerService, 
			String projId, String spaceId, String docId,  String lastUpdate, 
			String[] workItemTypes, String[] includeCustomFields, String[] includeLinkRoles) {
		
		String sqlQuery = buildSqlQuery(projId, spaceId, docId, lastUpdate, workItemTypes);
		ArrayList<WorkItemForJson> changes = new ArrayList<WorkItemForJson>();

		IDataService dataService = trackerService.getDataService();
		IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);
		for (IWorkItem workItem : workItems) {
			WorkItemForJson workItemForJson = processHistory(workItem, dataService, 
					lastUpdate, includeCustomFields, includeLinkRoles);			
			changes.add(workItemForJson);

		}
//		//TODO: debugging only
//		System.out.println("Total: "+workItems.size()); 
		
		return changes;
	}
	
	private WorkItemForJson processHistory(IWorkItem workItem, IDataService dataService, 
			String lastUpdate, String[] includeCustomFields, String[] includeLinkRoles) {
		WorkItemForJson workItemForJson = Utils.castWorkItem(workItem, includeCustomFields, includeLinkRoles);
		IPObjectList<IWorkItem> workItemHistory = dataService.getObjectHistory(workItem);
		if (workItemHistory != null) {
			if (workItemHistory.size() == 1 && workItemHistory.get(0) != null ) {
				// No changes in history when size == 1 (the WI remains as created)
				workItemForJson.setRevision(workItemHistory.get(0).getRevision());
			} else if (workItemHistory.size() > 1) {
				IDiffManager diffManager = dataService.getDiffManager();
				Collection<WorkItemChange> workItemChanges = collectWorkItemChanges(workItemHistory, diffManager, lastUpdate);
				workItemForJson.setWorkItemChanges(workItemChanges);	
				/**
				 * From Polarion JavaDoc: "The history list is sorted from the oldest (first) to the newest (last)."
				 * https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)
				 * Then, we get the last one from the history as the current revision
				 **/
				workItemForJson.setRevision(workItemHistory.get(workItemHistory.size() - 1).getRevision());
			} else {
				/** 
				 * No history. Empty list. From Polarion JavaDoc:
				 *  "An empty list is returned if the object does not support history retrieval."
				 *  "https://almdemo.polarion.com/polarion/sdk/doc/javadoc/com/polarion/platform/persistence/IDataService.html#getObjectHistory(T)"
				 **/
			}
		}
		return workItemForJson;
	}
	
	private Collection<WorkItemChange> collectWorkItemChanges(IPObjectList<IWorkItem> workItemHistory, IDiffManager diffManager, String lastUpdate) {
		Collection<WorkItemChange> workItemChanges = new ArrayList<WorkItemChange>();
		int index = 0;
		int next = 1;
		while (next < workItemHistory.size()) {
			if (Long.valueOf(workItemHistory.get(next).getRevision()) > Long.valueOf(lastUpdate)) {
				//TODO: ignore fields?
				IFieldDiff[] fieldDiffs = diffManager.generateDiff(workItemHistory.get(index), workItemHistory.get(next), new HashSet<String>());
				WorkItemChange fieldChangesToAdd = collectFieldChanges(fieldDiffs, workItemHistory.get(next).getRevision());
				if (fieldChangesToAdd != null) {
					workItemChanges.add(fieldChangesToAdd);
				}				
			}
			index++;
			next++;
		}	
		return workItemChanges;
	}
	
	private WorkItemChange collectFieldChanges(IFieldDiff[] fieldDiffs, String revision) {
		WorkItemChange workItemChange = new WorkItemChange(revision);
		for (IFieldDiff fieldDiff : fieldDiffs) {
			if (fieldDiff.isCollection()) {
				collectFieldDiffAsCollection(workItemChange, fieldDiff);			
			} else {
				WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName(), null, null,
						null, null);
				fieldChange.setFieldValueBefore(
						Utils.castFieldValueToString(fieldDiff.getBefore()));
				fieldChange.setFieldValueAfter(
						Utils.castFieldValueToString(fieldDiff.getAfter()));
				workItemChange.addFieldChange(fieldChange);
			}	
		}
		return workItemChange;
	}

	private void collectFieldDiffAsCollection(WorkItemChange workItemChange, IFieldDiff fieldDiff) {
		//Polarion returns unparameterized Collections for these two methods
		Collection added = fieldDiff.getAdded();
		Collection removed = fieldDiff.getRemoved();
		if (added != null && added.size() > 0) {
			WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName(), null, null,
					null, null);
			// We check if the collection is hyperlink list first since they're not
			// convertible into IPObjectList. So, we treat them separately.
			if (Utils.isCollectionHyperlinkStructList(added)) {
				fieldChange.setElementsAdded(Utils.castHyperlinksToStrArray(added));
				//Then we check if they're ILiknedWorkItemStruc, because, again,
				// Polarion treats those 'struct' objects differently thank regular
				// IPObjects
			} else if (Utils.isCollectionLinkedWorkItemStructList(added)) {
				fieldChange.setElementsAdded(Utils.castLinkedWorkItemsToStrArray(added));
			} else if (Utils.isCollectionApprovalStructList(added)) { 
				fieldChange.setElementsAdded(Utils.castApprovalsToStrArray(added));
			} else {
				try {
					fieldChange.setElementsAdded(Utils.castCollectionToStrArray((List<IPObject>)added));	
				} catch(ClassCastException ex) {
					// TODO: log
					fieldChange.setElementsAdded(new String[] {""});
				}
			}			
			workItemChange.addFieldChange(fieldChange);
		}
		if (removed != null && removed.size() > 0) {
			WorkItemFieldDiff fieldChange = new WorkItemFieldDiff(fieldDiff.getFieldName(), null, null,
					null, null);
			if (Utils.isCollectionHyperlinkStructList(removed)) {
				fieldChange.setElementsRemoved(Utils.castHyperlinksToStrArray(removed));
			} else if (Utils.isCollectionLinkedWorkItemStructList(removed)) {
				fieldChange.setElementsRemoved(Utils.castLinkedWorkItemsToStrArray(removed));
			} else if (Utils.isCollectionApprovalStructList(removed)) { 
				fieldChange.setElementsRemoved(Utils.castApprovalsToStrArray(removed));
			} else {
				try {
					fieldChange.setElementsRemoved(Utils.castCollectionToStrArray((List<IPObject>)removed));
				} catch (ClassCastException ex) {
					//TODO: log
					fieldChange.setElementsRemoved(new String[] {""});
				}
			}
			workItemChange.addFieldChange(fieldChange);
		}
	}

	private boolean validateParameters(ITrackerService trackerService, String projectId, String space, String doc) {
		//Needs to be executed in this order. Space validation only runs after projectId is validated.
		// DocId is validated only if projectId and SpaceId are validated.
		return validateProjectId(trackerService, projectId) && 
				validateSpaceId(trackerService, projectId, space) &&
				validateDocumentId(trackerService, projectId, space, doc);
	}

	private boolean validateProjectId(ITrackerService trackerService, String projectId) {
		try {
			IProject projObj = trackerService.getProjectsService().getProject(projectId);

			this.getServletContext().log("Attempting to read projectID: " + projObj.getId());
			this.getServletContext().log("Project name: " + projObj.getName());
			this.getServletContext().log("Project description: " + projObj.getDescription());
			
			return true;
			
		}catch (UnresolvableObjectException exception) {
			return false;
		}	
	}
	
	private boolean validateSpaceId(ITrackerService trackerService, String projId, String spaceId) {
		this.getServletContext().log("Attempting to read folder: " + spaceId);
		return trackerService.getFolderManager().existFolder(projId, spaceId);	
	}	
	
	/** This helper method should be called after validating the space (aka folder) **/
	private boolean validateDocumentId(ITrackerService trackerService, String projId, String space, String docId) {
		this.getServletContext().log("Attempting to read module: " + docId);
		//Haven't found in the Polarion Java API a straightforward way to validate a docId. 
		//Possible solution:
		//select all documents in the given valid space (folder)
		// then iterate through the documents to validate the given docId
		IDataService dataService = trackerService.getDataService();
        String query = "select M.C_PK, M.C_ID from MODULE M "
        		+ "inner join PROJECT P on M.FK_URI_PROJECT = P.C_URI "
        		+ "and P.C_ID = '" + projId + "' "
        		+ "and M.C_MODULEFOLDER = '" + space + "'";
        IPObjectList<IModule> modules = dataService.sqlSearch(query);
        for (IModule module : modules) {
        	if (module.getId().equals(docId)) {
        		return true;
        	}
        }
		return false;
	}

}
