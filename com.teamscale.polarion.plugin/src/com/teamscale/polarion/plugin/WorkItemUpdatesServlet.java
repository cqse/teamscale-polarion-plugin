package com.teamscale.polarion.plugin;

import com.google.gson.Gson;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.logging.ILogger;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.diff.IDiffManager;
import com.polarion.platform.persistence.diff.IFieldDiff;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.PermissionDeniedException;
import com.polarion.platform.service.repository.AccessDeniedException;
import com.teamscale.polarion.plugin.model.LinkBundle;
import com.teamscale.polarion.plugin.model.LinkFieldDiff;
import com.teamscale.polarion.plugin.model.LinkedWorkItem;
import com.teamscale.polarion.plugin.model.WorkItemChange;
import com.teamscale.polarion.plugin.model.WorkItemFieldDiff;
import com.teamscale.polarion.plugin.model.WorkItemForJson;
import com.teamscale.polarion.plugin.utils.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  
  private static final ILogger logger = Logger.getLogger(WorkItemUpdatesServlet.class);
  
  private ITrackerService trackerService =
          (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);
  
  private IModule module;
  
  // If empty, no work item links should be included. We expect role IDs (not role names)
  private String[] includeLinkRoles;
  
  // Base revision # for the request
  private String lastUpdate;
  
  // End revision # to indicate the final revision (included) the request is looking for
  private String endRevision;
  
  // List of possible types the result can have. If empty, items of all types should be included.
  private String[] workItemTypes;
  
  // List of work item custom fields that should be included in the result.
  // If empty, no custom fields should be present.
  private String[] includeCustomFields;
  
  // This is to generate changes in the json result related to backward links, since
  // Polarion doesn't return a diff/change associated with the opposite end of the link
  // when a link changes (added/removed). 
  // The key of this map is the workItemId to receive the change
  private Map<String, List<LinkBundle>> backwardLinksTobeAdded;
  
  // This is to keep in memory all result objects (type WorkItemsForJson) indexed by WorkItem ID
  // This provides O(1) access when, at the end, we need to go back and feed them with
  // the work items opposite link changes.
  private Map<String, WorkItemForJson> allItemsToSend;
  
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

    lastUpdate = req.getParameter("lastUpdate");
    endRevision = req.getParameter("endRevision");

    workItemTypes = req.getParameterValues("includedWorkItemTypes");

    includeCustomFields = req.getParameterValues("includedWorkItemCustomFields");

    includeLinkRoles = req.getParameterValues("includedWorkItemLinkRoles");
    
    if (!processRevisionNumbers()) {
        logger.info("[Teamscale Polarion Plugin] Invalid revision numbers. Review the lastUpdate and endRevision strings.");
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
        return ;
    }
    
    try {
      // To prevent SQL injection issues
      // Check if the request params are valid IDs before putting them into the SQL query
      if (validateParameters(projId, spaceId, docId)) {
        ArrayList<WorkItemForJson> changes = new ArrayList<WorkItemForJson>();
        
        //Resetting these Servlet global maps.
        backwardLinksTobeAdded = new HashMap<String, List<LinkBundle>>();
        allItemsToSend = new HashMap<String, WorkItemForJson>();
        
        retrieveChanges(projId, spaceId, docId);
        sendResponse(res);
        
        logger.info("[Teamscale Polarion Plugin] Successful response sent");
      } else {
        logger.info("[Teamscale Polarion Plugin] Invalid conbination of projectId/folderId/documentId");
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not found");
      }
    } catch (PermissionDeniedException permissionDenied) {
    		logger.error("[Teamscale Polarion Plugin] Permission denied raised by Polarion",
    						permissionDenied);
    		res.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    catch (AccessDeniedException accessDenied) {
    		logger.error("[Teamscale Polarion Plugin] Access denied raised by Polarion", 
    						accessDenied);  		
    		res.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  /* This method is necessary since Polarion redirects a POST request when the original GET request to this servlet 
   * is attempted without being authenticated. Polarion redirects the client to a login form.
   * Once the client sends an auth request (which is a POST) and is successfully authenticated, 
   * Polarion then redirects the auth post request to this servlet (which we then call the method doGet).
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }
  
  private boolean processRevisionNumbers() {
  		if (lastUpdate == null) {
  				lastUpdate = "0"; // process from beginning
  		} else if (!validateRevisionNumberString(lastUpdate)){
  				return false;
  		}

  		if (endRevision == null) {
  				// process all the way to HEAD
  				endRevision = String.valueOf(Integer.MAX_VALUE); 
  		} else if (!validateRevisionNumberString(endRevision)) {
  				return false;
  		}
  		
  		return (Integer.valueOf(lastUpdate) < Integer.valueOf(endRevision));
  }

  private void sendResponse(HttpServletResponse resp)
      throws ServletException, IOException {
    Gson gson = new Gson();
    String jsonResult = gson.toJson(allItemsToSend.values());
    resp.setContentType("application/json");
    PrintWriter out = resp.getWriter();
    out.print(jsonResult);
  }

  private String buildSqlQuery(String projId, String spaceId, String docId) {

    StringBuilder sqlQuery = new StringBuilder("select * from WORKITEM WI ");
    sqlQuery.append("inner join PROJECT P on WI.FK_URI_PROJECT = P.C_URI ");
    sqlQuery.append("inner join MODULE M on WI.FK_URI_MODULE = M.C_URI ");
    sqlQuery.append("where P.C_ID = '" + projId + "'");
    sqlQuery.append(" and M.C_ID = '" + docId + "'");
    sqlQuery.append(" and M.C_MODULEFOLDER = '" + spaceId + "'");
    sqlQuery.append(" and WI.C_REV > " + lastUpdate);
    sqlQuery.append(generateWorkItemTypesAndClause());

    return sqlQuery.toString();
  }

  /** If empty, work items of all types should be included. * */
  private String generateWorkItemTypesAndClause() {
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

  /** 
   * Based on Polarion documentation, the revision column is INTEGER.
   * In Postgresql, the max integer is the same as the max Java integer, which is the
   * maximum revision number a project can have in Polarion/SVN.
   * For reference: https://almdemo.polarion.com/polarion/sdk/doc/database/FullDBSchema.pdf
   * */
  private boolean validateRevisionNumberString(String revision) {
    if (revision != null) {
    		try {
    				int n = Integer.parseInt(revision);
    				return n >= 0;
    		} catch (NumberFormatException e) {
    				return false;
    		}
    }
    return false;
  }

  private void retrieveChanges(String projId, String spaceId, String docId) {

    String sqlQuery = buildSqlQuery(projId, spaceId, docId);
    
    IDataService dataService = trackerService.getDataService();
    
    long timeBefore = System.currentTimeMillis();
    
    IPObjectList<IWorkItem> workItems = dataService.sqlSearch(sqlQuery);
    
    long timeAfter = System.currentTimeMillis();
    logger.info("[Teamscale Polarion Plugin] Finished sql query. Execution time in ms: " + (timeAfter - timeBefore));
    
    timeBefore = System.currentTimeMillis();
    
    for (IWorkItem workItem : workItems) {
    	WorkItemForJson workItemForJson;
    	// This is because WIs moved to the trash can are still in the Polarion WI table we query
    	if (wasMovedToRecycleBin(workItem)) {
    			workItemForJson = buildDeletedWorkItemForJson(workItem);
    	} else {
    			workItemForJson =
    		          processHistory(workItem, dataService);
    	}
    	allItemsToSend.put(workItem.getId(), workItemForJson);
    }
    
    createLinkChangesOppositeEntries();
    
    timeAfter = System.currentTimeMillis();
    logger.info("[Teamscale Polarion Plugin] Finished processing request results. "
        + "Execution time (ms): " + (timeAfter - timeBefore));
    
  }
  
  /** This method will create the WI changes on the opposite side of the link changes **/
  private void createLinkChangesOppositeEntries() {
      backwardLinksTobeAdded.forEach((workItemId, linkBundles) -> {
      		WorkItemForJson workItemForJson = allItemsToSend.get(workItemId);
      		if (workItemForJson != null) {
      				Collection<WorkItemChange> workItemChanges = workItemForJson.getWorkItemChanges();
      				linkBundles.forEach(linkBundle -> {
          				WorkItemChange workItemChange = findRevisionEntry(workItemChanges, linkBundle);
          				if (workItemChange == null) {
          						workItemChange = new WorkItemChange(linkBundle.getRevision());	
          						workItemForJson.addWorkItemChange(workItemChange);
          				} 
      						WorkItemFieldDiff fieldChangeEntry = findFieldChangeEntry(workItemChange, linkBundle);
      						if (fieldChangeEntry == null) {
      								fieldChangeEntry = new LinkFieldDiff(Utils.LINKED_WORK_ITEMS_FIELD_NAME, 
      												linkBundle.getLinkedWorkItem().getLinkRoleId());
      								workItemChange.addFieldChange(fieldChangeEntry);
      						}
      						if (linkBundle.isAdded()) {
      								fieldChangeEntry.addElementAdded(linkBundle.getLinkedWorkItem().getId());
      						} else {
      								fieldChangeEntry.addElementRemoved(linkBundle.getLinkedWorkItem().getId());
      						}    						
      				});
      		}
      });  		
  }
  
  /** Helper method for {@link #createLinkChangesOppositeEntries}. **/
  private WorkItemChange findRevisionEntry(Collection<WorkItemChange> workItemChanges, LinkBundle linkBundle) {
  		
  		if (workItemChanges == null) return null;
  		
  		for (WorkItemChange workItemChange : workItemChanges) {
  				if (workItemChange.getRevision().equals(linkBundle.getRevision()) ) { 
  						return workItemChange;
  				}
  		}
  		return null;
  }
  
  /** Helper method for {@link #createLinkChangesOppositeEntries}. **/
  private WorkItemFieldDiff findFieldChangeEntry(WorkItemChange workItemChange, LinkBundle linkBundle) {
			for (WorkItemFieldDiff fieldChangeEntry : workItemChange.getFieldChanges()) {		
					if (fieldChangeEntry.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)
									&& fieldChangeEntry instanceof LinkFieldDiff
									&& ((LinkFieldDiff)fieldChangeEntry)
										.getLinkRoleId()
										.equals(linkBundle.getLinkedWorkItem().getLinkRoleId())) {
							return fieldChangeEntry;
					}
			}  	
			return null;
  }
  
  /** 
   *  In Polarion, WIs in the recycle bin will still come in the SQL query, as 
   *  in the database level they're still related to the module.
   *  However, the following API method excludes them and consider them as items
   *  NOT contained in the module.
   *  **/
  private boolean wasMovedToRecycleBin(IWorkItem workItem) {
  		return !module.containsWorkItem(workItem);
  }
  
  /** Create the work item object as DELETED **/
  private WorkItemForJson buildDeletedWorkItemForJson(IWorkItem workItem) {
  		return new WorkItemForJson(workItem.getId(), 
							Utils.UpdateType.DELETED, workItem.getLastRevision());
  }

  /** Main method that will process the work item history based on the parameters in the request **/
  private WorkItemForJson processHistory(IWorkItem workItem, IDataService dataService) {
  		
  		//No matter what endRevision is, we're always returning the latest version of the WI.
  		// Plus its changes up to the endRevision.
    WorkItemForJson workItemForJson =
        Utils.castWorkItem(workItem, includeCustomFields, includeLinkRoles);
    
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
      	int lastUpdateIndex = searchIndexWorkItemHistory(workItemHistory);
        IDiffManager diffManager = dataService.getDiffManager();
        Collection<WorkItemChange> workItemChanges =
            collectWorkItemChanges(workItem.getId(), workItemHistory, diffManager, lastUpdateIndex);
        //If we want to return the WI version matching the end revision #, we could just
        //reuse the loop in collectWorkItemChanges that will always stop at the endRevision
        // or at the last element of the list
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
  
  /** 
   * Helper method that will collect work item changes (WorkItemChange) based on the diff of each 
   * pair of work item versions in a given work item history.
   * **/
  private Collection<WorkItemChange> collectWorkItemChanges(
  				String workItemId, 
  				List<IWorkItem> workItemHistory, 
  				IDiffManager diffManager, 
  				int lastUpdateIndex) {
  		Collection<WorkItemChange> workItemChanges = new ArrayList<WorkItemChange>();
  		
  		// Short circuit: no changes to look for
  		// This would happen when the lastUpdate request parameter is greater than the latest revision #
  		if (lastUpdateIndex < 0) return workItemChanges;

  		int index = lastUpdateIndex;
  		int next = index + 1;
  		while (next < workItemHistory.size() && 
  						Integer.valueOf(workItemHistory.get(next).getRevision()) <= Integer.valueOf(endRevision)) {
  				
  				if (Integer.valueOf(workItemHistory.get(next).getRevision()) > Integer.valueOf(lastUpdate)) {
  						IFieldDiff[] fieldDiffs =
  										diffManager.generateDiff(
  														workItemHistory.get(index), workItemHistory.get(next), new HashSet<String>());
  						WorkItemChange fieldChangesToAdd =
  										collectFieldChanges(workItemId, fieldDiffs, workItemHistory.get(next).getRevision());
  						if (fieldChangesToAdd != null) {
  								workItemChanges.add(fieldChangesToAdd);
  						}
  				}
  				index++;
  				next++;
  		}
  		return workItemChanges;
  }

  /** 
   * Helper method that will collect field changes (to be included in a WorkItemChange object) based on the diff
   * created at {@link #collectWorkItemChanges(String, List, IDiffManager, int)}
   * **/
  private WorkItemChange collectFieldChanges(String workItemId, IFieldDiff[] fieldDiffs, String revision) {
    WorkItemChange workItemChange = new WorkItemChange(revision);
    
    if (fieldDiffs == null) return null;
    
    for (IFieldDiff fieldDiff : fieldDiffs) {
      if (fieldDiff.isCollection()) {
        collectFieldDiffAsCollection(workItemId, workItemChange, fieldDiff);
      } else {
        WorkItemFieldDiff fieldChange =
            new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setFieldValueBefore(Utils.castFieldValueToString(fieldDiff.getBefore()));
        fieldChange.setFieldValueAfter(Utils.castFieldValueToString(fieldDiff.getAfter()));
        workItemChange.addFieldChange(fieldChange);
      }
    }
    return workItemChange;
  }

  /** 
   * Helper method that will process field changes when the field is a collection type of field.
   * Rather then having a 'before value' replaced by an 'after value' Polarion returns these fields
   * with elements that were added or removed in the revision.
   * **/ 
  private void collectFieldDiffAsCollection(String workItemId, WorkItemChange workItemChange, IFieldDiff fieldDiff) {
    // Polarion returns unparameterized Collections for these two methods
    Collection added = fieldDiff.getAdded();
    Collection removed = fieldDiff.getRemoved();
    if (added != null && added.size() > 0) {
      WorkItemFieldDiff fieldChange = null;
      // We check if the collection is hyperlink list first since they're not
      // convertible into IPObjectList. So, we treat them separately.
      if (Utils.isCollectionHyperlinkStructList(added)) {
      	fieldChange	= new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsAdded(Utils.castHyperlinksToStrList(added));
        // Then we check if they're ILiknedWorkItemStruc, because, again,
        // Polarion treats those 'struct' objects differently thank regular
        // IPObjects
      } else if (includeLinkRoles != null && Utils.isCollectionLinkedWorkItemStructList(added)) {
      	// If the collection is a list of LinkedWorkItemStruct, we also treat them specifically
      	String linkRoleId = ((ILinkedWorkItemStruct)added.iterator().next()).getLinkRole().getId();
      	if (Arrays.stream(includeLinkRoles).anyMatch(linkRoleId::equals) 
      					&& fieldDiff.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)) {
      			fieldChange	= new LinkFieldDiff(fieldDiff.getFieldName(), 
      								((ILinkedWorkItemStruct)added.iterator().next()).getLinkRole().getId());
      			fieldChange.setElementsAdded(Utils.castLinkedWorkItemsToStrList(added));	
      			updateOppositeLinksMap(workItemId, workItemChange.getRevision(), added, true);	
      	}
      } else if (Utils.isCollectionApprovalStructList(added)) {
      	// If the collection is a list of ApprovalStruct, we also treat them specifically
      	fieldChange	= new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsAdded(Utils.castApprovalsToStrList(added));
      } else if (!Utils.isCollectionLinkedWorkItemStructList(added)) {
      	fieldChange	= new WorkItemFieldDiff(fieldDiff.getFieldName());
        try {
          fieldChange.setElementsAdded(Utils.castCollectionToStrList((List<IPObject>) added));
        } catch (ClassCastException ex) {
          // For now, when an added element/value is not among the ones we're supposed
          // to support in Teamscale, we simply add as an empty string array.
          // Alternatively, we could ignore the field as a change
          // (skip the field from the json output)
          fieldChange.setElementsAdded(new ArrayList<String>());
        }
      }
      
      if (fieldChange != null) {
      		workItemChange.addFieldChange(fieldChange);
      }
    }
    if (removed != null && removed.size() > 0) {
      WorkItemFieldDiff fieldChange = null;
      if (Utils.isCollectionHyperlinkStructList(removed)) {
      	fieldChange	= new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsRemoved(Utils.castHyperlinksToStrList(removed));
      } else if (includeLinkRoles != null && Utils.isCollectionLinkedWorkItemStructList(removed)) {
      		String linkRoleId = ((ILinkedWorkItemStruct)removed.iterator().next()).getLinkRole().getId();
        	if (Arrays.stream(includeLinkRoles).anyMatch(linkRoleId::equals) 
        					&& fieldDiff.getFieldName().equals(Utils.LINKED_WORK_ITEMS_FIELD_NAME)) {
        			fieldChange	= new LinkFieldDiff(fieldDiff.getFieldName(), 
        								((ILinkedWorkItemStruct)removed.iterator().next()).getLinkRole().getId());
        			fieldChange.setElementsRemoved(Utils.castLinkedWorkItemsToStrList(removed));	
        			updateOppositeLinksMap(workItemId, workItemChange.getRevision(), removed, false);
        	}
      } else if (Utils.isCollectionApprovalStructList(removed)) {
      	fieldChange	= new WorkItemFieldDiff(fieldDiff.getFieldName());
        fieldChange.setElementsRemoved(Utils.castApprovalsToStrList(removed));
      } else if (!Utils.isCollectionLinkedWorkItemStructList(removed)) {
      	fieldChange	= new WorkItemFieldDiff(fieldDiff.getFieldName());
        try {
          fieldChange.setElementsRemoved(Utils.castCollectionToStrList((List<IPObject>) removed));
        } catch (ClassCastException ex) {
          // For now, when a removed element/value is not among the ones we're supposed
          // to support in Teamscale, we simply add as an empty string array.
          // Alternatively, we could ignore the field as a change
          // (skip the field from the output)
          fieldChange.setElementsRemoved(new ArrayList<String>());
        }
      }
      if (fieldChange != null) {
      		workItemChange.addFieldChange(fieldChange);
      }
    }
  }
  
  /** 
   * This maintains a map of opposite work item links. This is necessary since Polarion
   * doesn't generate a change/field diff for the opposite end of the link.
   * So, we need to generate those link changes manually for receiving end.
   * @param	workItemId represents the link origin. On the map, this id will be the receiver (reverse)
   * @param revision represents the revision number when the link changed (added/removed)
   * @param links represents a collection generated by Polarion containing the added/removed links
   * @param added is true if this a list of added links, otherwise these are removed links
   *  **/
  private void updateOppositeLinksMap(
  				String workItemId, 
  				String revision, 
  				Collection<ILinkedWorkItemStruct> links,
  				boolean added) {
  		
  		// For each link struct, get the WI id, check if there's an entry in the map
  		// if there is, check if there's a link of same type, action (added/removed), and revision
  		// if there isn't, add an entry to the map flipping the ids (reverse)
  		for (ILinkedWorkItemStruct linkStruct : links) {
  				List<LinkBundle> linkBundles = backwardLinksTobeAdded.get(linkStruct.getLinkedItem().getId());
  				LinkBundle reverse = null;
  				if (linkBundles == null) {
  						reverse = new LinkBundle();
  	  				reverse.setAdded(added);
  	  				reverse.setRevision(revision);
  	  				reverse.setLinkedWorkItem(new LinkedWorkItem(workItemId, linkStruct.getLinkRole().getId()));
  	  				List<LinkBundle> newLinkBundles = new ArrayList<LinkBundle>();
  	  				newLinkBundles.add(reverse);
  	  				backwardLinksTobeAdded.put(linkStruct.getLinkedItem().getId(), newLinkBundles);
  				} else {
  						if (!alreadyHasLinkBundle(linkBundles, linkStruct, revision, added)) {
  								reverse = new LinkBundle();
  								reverse.setAdded(added);
  								reverse.setRevision(revision);
  								reverse.setLinkedWorkItem(new LinkedWorkItem(workItemId, linkStruct.getLinkRole().getId()));
  								linkBundles.add(reverse);  								
  						}
  				}
  		}
  }
  
  /** Helper method called by {@link #updateOppositeLinksMap()} to check if a LinkBundle was already created in the map **/
  private boolean alreadyHasLinkBundle(List<LinkBundle> linkBundles, ILinkedWorkItemStruct linkStruct, String revision, boolean added) {
			if (linkBundles == null) return false;
  		for (LinkBundle linkBundle : linkBundles) {
					if (linkBundle.getRevision().equals(revision) && linkBundle.isAdded() == added 
									&& linkBundle.getLinkedWorkItem().getLinkRoleId().equals(linkStruct.getLinkRole().getId())
									&& linkBundle.getLinkedWorkItem().getId().equals(linkStruct.getLinkedItem().getId())){
							return true;
					}
			}
  		return false;
  }
  
  /** Binary search to cut down the search space since the list is ordered in ascending order**/
  private int searchIndexWorkItemHistory(IPObjectList<IWorkItem> workItemHistory) {
  		// Short circuit: don't need to binarysearch if you're looking for all history
  		if (Integer.valueOf(lastUpdate) <= 0) return 0;
  		
  	  // Short circuit: don't need to search if lastUpdate already points to the last history item
  		if (Integer.valueOf(workItemHistory.get(workItemHistory.size() - 1)
  						.getRevision()) == Integer.valueOf(lastUpdate)) 
  						return workItemHistory.size() - 1;
  		
  	  // Short circuit: don't need to search if all the changes are before lastUpdate
  		if (Integer.valueOf(workItemHistory.get(workItemHistory.size() - 1)
  						.getRevision()) < Integer.valueOf(lastUpdate)) 
  						return -1;  
  		
      int left = 0;
      int right = workItemHistory.size() - 1;
      int index = -1;  		
      
      //binary search the 'lastUpdate index'
      while (left <= right) {
          int mid = (left + right) / 2;
          if (Integer.valueOf(workItemHistory.get(mid).getRevision()) < Integer.valueOf(lastUpdate)) {
              left = mid + 1;
          } else if (Integer.valueOf(workItemHistory.get(mid).getRevision()) == Integer.valueOf(lastUpdate)){
              return mid;
          } else {
          		index = mid;
          		right = mid -1;
          }
      }
  
      return (index == 0 ? index : index - 1);
  }

  /*
   * This method validates these required request attributes.
   * Split into three separate helper methods, one for each param.
   *  */
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

      logger.info("[Teamscale Polarion Plugin] Attempting to read projectID: " + projObj.getId());

      return true;

    } catch (UnresolvableObjectException exception) {
      logger.error("[Teamscale Polarion Plugin] Not possible to resolve project with id: " + 
      				projectId, exception);
      return false;
    }
  }

  private boolean validateSpaceId(String projId, String spaceId) {
    if (trackerService.getFolderManager().existFolder(projId, spaceId)) {
      logger.info("[Teamscale Polarion Plugin] Attempting to read folder: " + spaceId);
      return true;
    }
    logger.info("[Teamscale Polarion Plugin] Not possible to find folder with id: " + spaceId);
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
        logger.info("[Teamscale Polarion Plugin] Attempting to read document: " + docId);
        return true;
      }
    }
    logger.info("[Teamscale Polarion Plugin] Not possible to find document with id: " + docId);
    return false;
  }
}
