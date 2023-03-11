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
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObjectList;
import com.teamscale.polarion.plugin.model.WorkItemChange;

/**
 * This is the servlet which ...
 * 
 * @author Bruno da Silva
 */
public class WorkItemUpdatesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static class ProjectTimePair {
        public String projectName;
        public long time = 0;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
       
        ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);
        //TODO: turn into sql prepared statement
        String sqlQuery = "select * from WORKITEM WI "
        		+ "inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI "
        		+ "inner join MODULE M on WI.FK_URI_MODULE = M.C_URI "
        		+ "where P.C_ID = '" + req.getAttribute("project") + "'"
        		+ " AND M.C_ID = '" + req.getAttribute("document") + "'"
        		+ " AND M.C_MODULEFOLDER = '" + req.getAttribute("space") + "'";
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
        
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }    
    
}
