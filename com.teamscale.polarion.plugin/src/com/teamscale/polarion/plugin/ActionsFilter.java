package com.teamscale.polarion.plugin;

import com.polarion.portal.tomcat.servlets.DoAsFilter;
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

public class ActionsFilter extends DoAsFilter implements Filter {

  private ServletContext context;

  public void init(FilterConfig fConfig) throws ServletException {
    this.context = fConfig.getServletContext();
    this.context.log("[Teamscale Polarion Plugin] RequestLoggingFilter initialized");
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    if (req instanceof HttpServletRequest) {
      HttpServletRequest servletReq = (HttpServletRequest) req;
      if (validatePath(servletReq.getServletPath())) {
        this.context.log(
            "[Teamscale Polarion Plugin] Request received. Servlet context: "
                + ((HttpServletRequest) req).getContextPath());
        this.context.log(
            "[Teamscale Polarion Plugin] Servlet path: "
                + ((HttpServletRequest) req).getServletPath());
        setRequestPathParameters(servletReq);
        chain.doFilter(req, res);
        // TODO: Following Else/if for experimenting. Remove!
      } else if (servletReq.getServletPath().startsWith("/v2") && 
      				servletReq.getServletPath().endsWith("work-item-updates") && 
      				servletReq.getServletPath().split("/").length == 6) {
      		
          String[] pathParts = servletReq.getServletPath().split("/");

          req.setAttribute("project", pathParts[2]);
          req.setAttribute("space", pathParts[3]);
          req.setAttribute("document", pathParts[4]);
      		servletReq.setAttribute("version", "v2");
      		chain.doFilter(req, res);
      }
      else {
        ((HttpServletResponse) res)
            .sendError(
                HttpServletResponse.SC_NOT_FOUND, "The requested resource or action is not found");
      }
    } 
    else {
      throw new ServletException("This service supports only HTTP requests.");
    }
  }

  private void setRequestPathParameters(HttpServletRequest req) {
    String path = req.getServletPath();
    String[] pathParts = path.split("/");

    // The first part is an empty string since the
    // path starts with '/'
    req.setAttribute("project", pathParts[1]);
    req.setAttribute("space", pathParts[2]);
    req.setAttribute("document", pathParts[3]);
    // The fourth part should be the action name which is fixed
    // and already validated at this point
    this.context.log(
        "[Teamscale Polarion Plugin] Path parameters: "
            + "project: "
            + pathParts[1]
            + " space: "
            + pathParts[2]
            + " doc: "
            + pathParts[3]);
  }

  /**
   * Five parts are expected for example: For this path:
   * /elibrary/MyDummySpace/MyDummyDoc/work-item-updates The split will be: ["", "library",
   * "MyDummySpace", "MyDummyDoc", "work-item-updates"] It takes an empty string before the first
   * slash *
   */
  private boolean validatePath(String path) {
    return (path.endsWith("work-item-updates") && path.split("/").length == 5);
  }
}
