package com.teamscale.polarion.plugin.model;

/**
 * This is used to mark the response as either PARTIAl or COMPLETE.
 *
 * <p>PARTIAL: means the response does not completely fulfill the request, in other words, the
 * request would take longer than the timeout threshold, so to avoid timeout errors, we respond
 * partially, so the client can work on sending follow-up requests.
 *
 * <p>COMPLETE: means the response fulfills the request completely.
 */
public enum ResponseType {
  PARTIAL,
  COMPLETE
}
