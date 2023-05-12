package com.teamscale.polarion.plugin.client;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This client class was created for debugging purposes only.
 *
 * @author Bruno da Silva
 */
public class PluginClient {

  public static final String POLARION_BASE_URL = "http://localhost/polarion/";

  public static final String POLARION_PWD_AUTH_URL = "j_security_check";

  public static final String POLARION_TEAMSCALE_URL_PATH = "api/teamscale/";

  public static final String WI_UPDATES_URL_PATH =
      "elibrary/Testing/Test%20Specification/work-item-updates";

  public static final String USERNAME_FORM_FIELD = "j_username";

  public static final String PWD_FORM_FIELD = "j_password";

  public static final String CSRF_TOKEN_FORM_FIELD = "csrfToken";

  public static final String CSRF_COOKIE_NAME = "X-CSRF-Token";

  private final HttpClient httpClient;

  public PluginClient() {
    CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    CookieHandler.setDefault(cookieManager);
    httpClient =
        HttpClient.newBuilder()
            .cookieHandler(CookieHandler.getDefault())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    clearCookies();
  }

  private void clearCookies() {
    List<HttpCookie> cookies =
        ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();
    for (HttpCookie cookie : cookies) {
      cookie.setMaxAge(0);
    }
  }

  private String getCsrfTokenFromCookie() {

    List<HttpCookie> cookies =
        ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();

    for (HttpCookie cookie : cookies) {
      if (cookie.getName().equals(CSRF_COOKIE_NAME)) {
        return cookie.getValue();
      }
    }

    return "";
  }

  public String formatUrlEncodedParams(Map<String, String> paramsMap) {
    return paramsMap.entrySet().stream()
        .map(
            entry ->
                Stream.of(
                        URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("=")))
        .collect(Collectors.joining("&"));
  }

  public HttpResponse<?> postLoginRequest() throws IOException, InterruptedException {

    String csrfToken = this.getCsrfTokenFromCookie();

    // TODO: replace hardcoded username and pwd
    Map<String, String> paramsMap =
        Map.of(
            USERNAME_FORM_FIELD, "admin",
            PWD_FORM_FIELD, "admin",
            CSRF_TOKEN_FORM_FIELD, csrfToken);

    String params = this.formatUrlEncodedParams(paramsMap);

    System.out.println("csrfToken: " + csrfToken);

    return this.sendPostFormUrlEncodedRequest(
        PluginClient.POLARION_BASE_URL + PluginClient.POLARION_PWD_AUTH_URL, params);
  }

  public HttpResponse<?> sendWorkItemUpdatesGetRequest() throws IOException, InterruptedException {
    return this.sendGetRequest(
        PluginClient.POLARION_BASE_URL
            + PluginClient.POLARION_TEAMSCALE_URL_PATH
            + PluginClient.WI_UPDATES_URL_PATH,
        "");
  }

  private HttpResponse<?> sendPostFormUrlEncodedRequest(String url, String params)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("content-type", "application/x-www-form-urlencoded")
            .header("Origin", "teamscale://teamscale.polartion.plugin.client")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .header("User-Agent", "Teamscale Polarion Plugin")
            .POST(BodyPublishers.ofString(params))
            .build();
    logCookies();
    System.out.println(
        "\n[Teamscale Polarion Plugin Client] Attempting to send POST request: " + url);
    HttpResponse<?> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    logHeaders(request.headers());
    System.out.println("\nRESPONSE STATUS CODE: " + response.statusCode());
    logHeaders(response.headers());
    logCookies();
    return response;
  }

  // TODO: support query parameters
  private HttpResponse<?> sendGetRequest(String url, String params)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Origin", "teamscale://teamscale.polartion.plugin.client")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .header("User-Agent", "Teamscale Polarion Plugin")
            .GET()
            .build();
    logCookies();
    System.out.println(
        "\n[Teamscale Polarion Plugin Client] Attempting to send GET request: " + url);
    HttpResponse<?> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    logHeaders(request.headers());
    System.out.println("\nRESPONSE STATUS CODE: " + response.statusCode());
    logHeaders(response.headers());
    logCookies();
    return response;
  }

  private void logHeaders(HttpHeaders httpHeaders) {
    if (httpHeaders != null) {
      Map<String, List<String>> headersMap = httpHeaders.map();
      for (Map.Entry<String, List<String>> header : headersMap.entrySet()) {
        System.out.println(header.getKey() + ":" + String.join("\n", header.getValue()));
      }
    }
  }

  private void logCookies() {
    List<HttpCookie> cookies =
        ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();
    System.out.println("Cookies:");
    for (HttpCookie cookie : cookies) {
      System.out.println(cookie.getName() + ":" + cookie.getValue() + " Path=" + cookie.getPath());
    }
  }

  public boolean isAuthRequired(HttpResponse<?> response) {
    if (response == null) return false;

    Map<String, List<String>> headersMap = response.headers().map();
    for (Map.Entry<String, List<String>> header : headersMap.entrySet()) {
      if (header.getKey().equals("x-com-ibm-team-repository-web-auth-msg")
          && header.getValue().get(0).equals("authrequired")) return true;
    }
    return false;
  }

  public static void main(String[] args) {
    try {
      PluginClient pluginClient = new PluginClient();

      HttpResponse<?> response = pluginClient.postLoginRequest();

      if (response.statusCode() == 200 && pluginClient.isAuthRequired(response)) {

        response = pluginClient.postLoginRequest();

        if (response.statusCode() != 403) {
          response = pluginClient.sendWorkItemUpdatesGetRequest();
          if (response.statusCode() == 200) {
            System.out.println(response.body());
          }
        } else if (response.statusCode() == 403) {
          System.out.println("Access forbidden!");
        }
      } else if (response.statusCode() == 302) {
        // Got a redirect
      } else if (response.statusCode() == 303) {
        // Got a redirect
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
