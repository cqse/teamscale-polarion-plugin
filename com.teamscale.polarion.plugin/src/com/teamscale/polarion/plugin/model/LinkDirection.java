package com.teamscale.polarion.plugin.model;

/**
 * Polarion does not have a type for the link direction, so we added this enum that'll represent an
 * extra field in the json output every time we see a work item link field. This is helpful for
 * letting Teamscale know the link direction without having to run extra business logic and make
 * extra calls to Polarion
 */
public enum LinkDirection {
  IN, // incoming link
  OUT // outgoing link
}
