package com.teamscale.polarion.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObjectList;
import com.teamscale.polarion.plugin.model.WorkItemChange;

/**
 * This is the servlet ...
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
			String sqlQuery = "select * from WORKITEM WI "
					+ "inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI "
					+ "inner join MODULE M on WI.FK_URI_MODULE = M.C_URI "
					+ "where P.C_ID = '" + projId + "'"
					+ " AND M.C_ID = '" + docId + "'"
					+ " AND M.C_MODULEFOLDER = '" + spaceId + "'";
			//        		+ " AND WI.C_REV = 6";
			//				+ "AND M.C_LOCATION = 'elibrary/Testing/MySubSpace' "; //Doesn't work           
			//				+ "AND M.C_MODULELOCATION = 'MySubSpace/MyDummyDoc'"; //Doesn't work

			System.out.println(sqlQuery);

			ArrayList<WorkItemChange> workItemsChange = new ArrayList<WorkItemChange>();

			IDataService dataService = trackerService.getDataService();
			IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);
			for (IWorkItem workItem : workItems) {
				System.out.println("WI id: " + workItem.getId() + " title: "+workItem.getTitle());
				// System.out.println(workItem.getLastRevision()); //"Returns the last revision in the History of this object. (Does not return revisions from included child objects like Work Items or Document Workflow Signatures.)"
				// System.out.println(workItem.getDataRevision()); //"Returns revision from which the data was actualy read."
				// IModule module = trackerService.getModuleManager().getContainingModule(workItem);
				// System.out.println(" => Module Title or Name: "+ workItem.getModule().getTitleOrName()
				//		+ " ID: " + workItem.getModule().getId()
				//		+ " location: " + workItem.getModule().getModuleLocation()
				//		+ " folder: " + workItem.getModule().getFolder().getTitleOrName());

				WorkItemChange workItemChange = new WorkItemChange(workItem.getId());
				workItemsChange.add(workItemChange);

				if (workItem.getId().equals("EL-36")) {
					IPObjectList<IWorkItem> workItemsHistory = dataService.getObjectHistory(workItem);
					for (IWorkItem iWorkItem : workItemsHistory) {
						System.out.println(iWorkItem.getId() + " rev: " + iWorkItem.getDataRevision());

					}
					IDiffManager diffManager = dataService.getDiffManager();
					IFieldDiff[] fieldDiffs = diffManager.generateDiff(workItemsHistory.get(1), workItemsHistory.get(2), new HashSet<String>());
					for (int i = 0; i < fieldDiffs.length; i++) {
						System.out.println(fieldDiffs[i].getFieldName() + " : \n"
								+ fieldDiffs[i].getBefore() + " => " + fieldDiffs[i].getAfter() + "\n"
								+ "added? " + fieldDiffs[i].getAdded() + "\n"
								+ "removed? " + fieldDiffs[i].getRemoved());
					}

				}
			}
			System.out.println("Total: "+workItems.size());   

			Gson gson = new Gson();
			System.out.println(gson.toJson(workItemsChange));
			String jsonResult = gson.toJson(workItemsChange);
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

	private boolean validateParameters(ITrackerService trackerService, String projectId, String space, String doc) {
		return validateProjectId(trackerService, projectId) && 
				validateSpaceId(trackerService, projectId, space);
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
	
	private boolean validateDocumentId(ITrackerService trackerService, String space, String docId) {
		//I haven't found in the Polarion Java API a straightforward way to validade a docId. 
		//Possible solution:
		//select all documents in the given valid space (folder)
		// then iterate through the documents to validate the given docId
		return true;
	}

}
