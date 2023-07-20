package com.teamscale.polarion.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet defines the /is-alive endpoint. It'll return either a 200 with a string representing
 * the 'is alive' or an http error (e.g., 404, 500, 403, etc) depending on the scenario.
 *
 * @author Bruno da Silva
 */
public class IsAliveServletV2 extends HttpServlet {

  private static final long serialVersionUID = 1L;

  /**
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("text/plain");
    PrintWriter out = res.getWriter();
    out.print("Alive! I'm ready to crunch some work items!");
  }

  /**
   * This method is necessary since Polarion redirects a POST request when the original GET request
   * to this servlet is attempted without being authenticated. Polarion redirects the client to a
   * login form. Once the client sends an auth request (which is a POST) and is successfully
   * authenticated, Polarion then redirects the auth post request to this servlet (which we then
   * call the method doGet).
   *
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }
}
