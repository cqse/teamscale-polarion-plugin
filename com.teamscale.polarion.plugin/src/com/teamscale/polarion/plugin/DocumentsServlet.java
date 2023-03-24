package com.teamscale.polarion.plugin;

import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObjectList;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet was created for debugging purposes.
 * It prints out in the console document info. 

 * @author Bruno da Silva
 */
public class DocumentsServlet extends HttpServlet {

		private static final long serialVersionUID = 1L;

		/* (non-Javadoc)
		 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
		 */
		@Override
		protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

				ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);

				String projStr = "elibrary";

				String query = "select * from MODULE M "
								+ "inner join PROJECT P on M.FK_PROJECT = P.C_PK "
								+ "where P.C_ID = 'elibrary' ";
				//        		+ "AND M.C_ID = 'MyDummyDoc' "
				//				+ "AND M.C_LOCATION = 'elibrary/Testing/MySubSpace' ";        		
				//        		+ "AND M.C_MODULEFOLDER = 'MySubSpace'"; // Works
				//				+ "AND M.C_MODULELOCATION = 'MySubSpace/MyDummyDoc'"; //Doesn't work

				IDataService dataService = trackerService.getDataService();
				IPObjectList<IModule> modules = dataService.sqlSearch(query);
				for (IModule module : modules) {
						System.out.println("Module ID: " + module.getId()
						+ " \n Location path: " + module.getModuleLocation().getLocationPath() // <folder>/<docId>
						+ " \n KEY_LOCATION: "+module.getValue(IModule.KEY_LOCATION) // Full Location obj
						+ " \n KEY_TITLE: "+module.getValue(IModule.KEY_TITLE)
						+ " \n KEY_MODULEFOLDER: "+module.getValue(IModule.KEY_MODULEFOLDER) // <folder>
						+ " \n KEY_MODULELOCATION: "+module.getValue(IModule.KEY_MODULELOCATION)); // simple location obj
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
