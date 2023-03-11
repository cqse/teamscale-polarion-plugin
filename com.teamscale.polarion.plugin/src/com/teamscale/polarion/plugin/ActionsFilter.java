package com.teamscale.polarion.plugin;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.polarion.portal.tomcat.servlets.DoAsFilter;

public class ActionsFilter extends DoAsFilter implements Filter {

	private ServletContext context;
	
	public void init(FilterConfig fConfig) throws ServletException {
		this.context = fConfig.getServletContext();
		this.context.log("RequestLoggingFilter initialized");
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		
		//This is something to try. Snippet found at the Polarion community forum
//		ISecurityService securityService = (ISecurityService) PlatformContext.getPlatform().lookupService(ISecurityService.class);
//		IPassword credentials = Password.of("admin", "admin");
//		try {
//			Subject userSubject = securityService.login().from("default").authenticator(Password.id("polarion_login_form")).with(credentials).perform();
////			securityService.doAsUser(userSubject, new PrivilegedAction<Object>() {
////		        
////				@SuppressWarnings("unchecked")
////		        public Object run() {
////		          return TransactionExecuter.execute(RunnableWEx.wrap(ActionsFilterServlet.this));
////		        }
////		      });
//		} catch (AuthenticationFailedException e) {
//			// TODO Auto-generated catch block
//			this.context.log("Failed to login.");
//			e.printStackTrace();
//		}
		
		if (req instanceof HttpServletRequest) {
			HttpServletRequest servletReq = (HttpServletRequest) req;
			if (validatePath(servletReq.getServletPath())) {
				//TODO: Remove sysouts
				System.out.println( ((HttpServletRequest) req).getServletPath());
				System.out.println( ((HttpServletRequest) req).getContextPath());
				setRequestPathParameters(servletReq);
				chain.doFilter(req, res);
			} else { 
				((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource or action is not found");
			}
		} else {
			throw new ServletException("This service supports only HTTP requests.");
		}
	}
	
	private void setRequestPathParameters(HttpServletRequest req) {
		String path = req.getServletPath();
		String[] pathParts = path.split("/");
		
		//The first part is an empty string since the 
		// path starts with '/'
		req.setAttribute("project", pathParts[1]);
		req.setAttribute("space", pathParts[2]);
		req.setAttribute("document", pathParts[3]);
		// The fourth part should be the action name which is fixed
		// and already validated at this point
	}
	
	/** Five parts are expected for example:
	 * For this path: /elibrary/MyDummySpace/MyDummyDoc/work-item-updates
	 * The split will be: ["", "library", "MyDummySpace", "MyDummyDoc", "work-item-updates"]
	 * It takes an empty string before the first slash
	 * **/
	private boolean validatePath(String path) {
		return (path.endsWith("work-item-updates") &&
				path.split("/").length == 5);
	}
	
}