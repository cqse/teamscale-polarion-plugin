package com.teamscale.polarion.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ICategory;
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
		
		//Check if the request params are valid IDs before putting them into a SQL query
		if (validateParameters(trackerService, projId, spaceId, docId)) {

			ArrayList<WorkItemForJson> allChanges = retrieveChanges(trackerService, projId, spaceId, docId);

			Gson gson = new Gson();
			System.out.println(gson.toJson(allChanges));
			String jsonResult = gson.toJson(allChanges);
			res.setContentType("application/json");
			PrintWriter out = res.getWriter();        
			out.print(jsonResult);
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
	
	private ArrayList<WorkItemForJson> retrieveChanges(ITrackerService trackerService, String projId, String spaceId, String docId) {
		
		String sqlQuery = "select * from WORKITEM WI "
				+ "inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI "
				+ "inner join MODULE M on WI.FK_URI_MODULE = M.C_URI "
				+ "where P.C_ID = '" + projId + "'"
				+ " AND M.C_ID = '" + docId + "'"
				+ " AND M.C_MODULEFOLDER = '" + spaceId + "'";
		//        		+ " AND WI.C_REV = 6";
		//				+ "AND M.C_LOCATION = 'elibrary/Testing/MySubSpace' "; //Doesn't work           
		//				+ "AND M.C_MODULELOCATION = 'MySubSpace/MyDummyDoc'"; //Doesn't work
		
		ArrayList<WorkItemForJson> allChanges = new ArrayList<WorkItemForJson>();

		IDataService dataService = trackerService.getDataService();
		IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);
		for (IWorkItem workItem : workItems) {
			System.out.println("WI id: " + workItem.getId() + " title: "+workItem.getTitle());
			// System.out.println(workItem.getLastRevision()); //"Returns the last revision in the History of this object. (Does not return revisions from included child objects like Work Items or Document Workflow Signatures.)"
			// System.out.println(workItem.getDataRevision()); //"Returns revision from which the data was actually read."
			// IModule module = trackerService.getModuleManager().getContainingModule(workItem);
			// System.out.println(" => Module Title or Name: "+ workItem.getModule().getTitleOrName()
			//		+ " ID: " + workItem.getModule().getId()
			//		+ " location: " + workItem.getModule().getModuleLocation()
			//		+ " folder: " + workItem.getModule().getFolder().getTitleOrName());

			WorkItemForJson workItemForJson = processHistory(workItem, dataService);			
			allChanges.add(workItemForJson);

		}
		//TODO: debugging only
		System.out.println("Total: "+workItems.size()); 
		
		return allChanges;
	}
	
	private WorkItemForJson processHistory(IWorkItem workItem, IDataService dataService) {
		WorkItemForJson workItemForJson = Utils.castWorkItem(workItem);
		IPObjectList<IWorkItem> workItemHistory = dataService.getObjectHistory(workItem);
		if (workItemHistory != null) {
			if (workItemHistory.size() == 1 && workItemHistory.get(0) != null ) {
				// No changes in history when size == 1 (the WI remains as created)
				workItemForJson.setRevision(workItemHistory.get(0).getRevision());
			} else if (workItemHistory.size() > 1) {
				IDiffManager diffManager = dataService.getDiffManager();
				Collection<WorkItemChange> workItemChanges = collectWorkItemChanges(workItemHistory, diffManager);
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
	
	private Collection<WorkItemChange> collectWorkItemChanges(IPObjectList<IWorkItem> workItemHistory, IDiffManager diffManager) {
		Collection<WorkItemChange> workItemChanges = new ArrayList<WorkItemChange>();
		int index = 0;
		int next = 1;
		while (next < workItemHistory.size()) {
			//TODO: ignore fields?
			IFieldDiff[] fieldDiffs = diffManager.generateDiff(workItemHistory.get(index), workItemHistory.get(next), new HashSet<String>());
			Collection<WorkItemChange> fieldChangesToAdd = collectFieldChanges(fieldDiffs, workItemHistory.get(next).getRevision());
			if (fieldChangesToAdd != null && fieldChangesToAdd.size() > 0) {
				workItemChanges.addAll(fieldChangesToAdd);
			}
			index++;
			next++;
		}	
		return workItemChanges;
	}
	
	private Collection<WorkItemChange> collectFieldChanges(IFieldDiff[] fieldDiffs, String revision) {
		Collection<WorkItemChange> fieldChanges = new ArrayList<WorkItemChange>();
		for (IFieldDiff fieldDiff : fieldDiffs) {
			WorkItemChange workItemChange = new WorkItemChange(revision, fieldDiff.getFieldName(), 
					null, null, null, null);
			if (fieldDiff.isCollection()) {
				Collection added = fieldDiff.getAdded(); //Casting to the generic IPObject and checking instance down below
				Collection removed = fieldDiff.getRemoved();//Casting directly to ICategory		
				if (added != null && added.size() > 0) {
					workItemChange.setElementsAdded(Utils.castCollectionToStrArray(added));
				}
				if (removed != null && removed.size() > 0) {
					workItemChange.setElementsRemoved(Utils.castCollectionToStrArray(removed));
				}			
			} else {
				workItemChange.setFieldValueBefore(
						Utils.castFieldValueToString(fieldDiff.getBefore()));
				workItemChange.setFieldValueAfter(
						Utils.castFieldValueToString(fieldDiff.getAfter()));
			}
			fieldChanges.add(workItemChange);		
		}
		return fieldChanges;
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
