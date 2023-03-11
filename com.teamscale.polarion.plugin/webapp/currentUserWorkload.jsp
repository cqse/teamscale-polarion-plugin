<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.polarion.alm.tracker.model.IWorkItem" %>
<%@ page import="com.teamscale.polarion.plugin.WorkItemUpdatesServlet.ProjectTimePair" %>

<%
	    Collection pairs  = (Collection)request.getAttribute("pairs"); 
	    long day = ((Long)request.getAttribute("dayLength")).longValue(); 
	    long hour = 3600 * 1000L;
	    long minute = 60 * 1000L;
	    long total = 0;
%>
This table shows projects where You have assigned work items.<br />
<table border='1'>
<tr><th>Project Name</th><th>Remaining time</th></tr>
<%
  	int i = 1;
    for (Iterator iterator = pairs.iterator(); iterator.hasNext();) {
       	ProjectTimePair pair = (ProjectTimePair)iterator.next();
		long days = pair.time / day;
		long hours = (pair.time - days * day) / hour;
		long minutes = (pair.time - days * day - hours * hour) / minute;

		StringBuffer time = new StringBuffer();	
		if (days > 0){
	        time.append(days)
	        	.append(" days ");
		}
		if (hours > 0){
	        time.append(hours)
	        	.append(" hours ");
		}
	    if (minutes > 0){
	        time.append(minutes)
	        	.append(" minutes ");
	    }
	    total += pair.time;
 
%>
	<tr><td><%=pair.projectName%></td><td><%=time.toString()%></td></tr> 
<% } %>
</table>
<br/>
<%
		long days = total / day;
		long hours = (total - days * day) / hour;
		long minutes = (total - days * day - hours * hour) / minute;
		
		StringBuffer time = new StringBuffer();	
		if( days > 0){
	        time.append(days)
	        	.append(" days ");
		}
	    if (hours > 0){
	        time.append(hours)
	        	.append(" hours ");
	    }
	    if (minutes > 0){
	        time.append(minutes)
	        	.append(" minutes ");
	    }
%>
Total time: <%=time.toString()%>