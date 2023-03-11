package com.teamscale.polarion.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.IModuleManager;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;

/**
 * This servlet was created for debugging purposes.
 * It prints out in the console document info. 
 * 
 * @author Bruno da Silva
 */
public class DocumentsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static class ProjectTimePair {
        public String projectName;
        public long time = 0;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
       
        // Strategy 1: from the tracker service get the moduleManager (IModuleManager)
    	//Then get the WIs by calling getModuleWorkItems​(IProject project, java.lang.String query, java.lang.String sort, java.lang.String moduleFolder, int limit)
       	
        ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);

        String projStr = "elibrary";
     
        String query = "select * from MODULE M "
        		+ "inner join PROJECT P on M.FK_PROJECT = P.C_PK "
        		+ "where P.C_ID = 'elibrary' ";
//        		+ "AND M.C_ID = 'MyDummyDoc' "
//				+ "AND M.C_LOCATION = 'elibrary/Testing/MySubSpace' ";        		
//        		+ "AND M.C_MODULEFOLDER = 'MySubSpace'"; // Works
//				+ "AND M.C_MODULELOCATION = 'MySubSpace/MyDummyDoc'"; //Doesn't worl

        IDataService dataService = trackerService.getDataService();
        IPObjectList<IModule> modules = dataService.sqlSearch(query);
        for (IModule module : modules) {
        	System.out.println("Module ID: " + module.getId()
        	+ " \n Location path: " + module.getModuleLocation().getLocationPath()
        	+ " \n KEY_LOCATION: "+module.getValue(IModule.KEY_LOCATION)
        	+ " \n KEY_TITLE: "+module.getValue(IModule.KEY_TITLE)
        	+ " \n KEY_MODULEFOLDER: "+module.getValue(IModule.KEY_MODULEFOLDER)
        	+ " \n KEY_MODULELOCATION: "+module.getValue(IModule.KEY_MODULELOCATION));
        }
        System.out.println("Total: "+modules.size());  
        
//        req.setAttribute("pairs", pairs);
//        req.setAttribute("dayLength", dayLength);
        
//        getServletContext().getRequestDispatcher("/currentUserWorkload.jsp").forward(req, resp);
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }    
    
}
