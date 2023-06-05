package com.teamscale.polarion.plugin;

import com.polarion.portal.tomcat.servlets.DoAsFilter;
import com.teamscale.polarion.plugin.utils.PluginLogger;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Custom servlet filter that'll do basic checks before the servlet kicks in, such as checking if
 * the requested URL contains the at least the required elements in the path. Then, it sets the
 * required request attributes (project, space, module) as separate string objects for the servlet
 * to use.
 */
public class ActionsFilter extends DoAsFilter implements Filter {

  private final PluginLogger logger = new PluginLogger();

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    if (req instanceof HttpServletRequest) {
      HttpServletRequest servletReq = (HttpServletRequest) req;
      if (isRequestToIsAlive(servletReq.getServletPath())) {
        chain.doFilter(req, res);
      } else if (validatePath(servletReq.getServletPath())) {
        setRequestPathParameters(servletReq);
        chain.doFilter(req, res);
      } else {
        logger.info("404, Resource not found.");
        ((HttpServletResponse) res)
            .sendError(
                HttpServletResponse.SC_NOT_FOUND, "The requested resource or action is not found");
      }
    } else {
      logger.info("This service supports only HTTP(s) requests.");
      throw new ServletException("This service supports only HTTP(s) requests.");
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
  }

  /**
   * Five parts are expected for example: For this path:
   * /elibrary/MyDummySpace/MyDummyDoc/work-item-updates The split will be: ["", "library",
   * "MyDummySpace", "MyDummyDoc", "work-item-updates"] It takes an empty string before the first
   * slash
   */
  private boolean validatePath(String path) {
    return (path.endsWith("work-item-updates") && path.split("/").length == 5);
  }

  private boolean isRequestToIsAlive(String path) {
    return (path.endsWith("is-alive") && path.split("/").length == 2);
  }
}
