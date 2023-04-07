package com.teamscale.polarion.plugin.model;

/**
 * Model class that represents a LinkedWorkItem containing
 * the id of the linked WorkItem and the link role id.
 * Note: In Polarion the link role id might be different than link role user-facing name).
 * */
public class LinkedWorkItem {
		private String id;
		private String linkRole;
		
		public LinkedWorkItem(String id, String linkRole) {
				this.id = id;
				this.linkRole = linkRole;
		}
		
		public String getId() {
				return id;
		}
		
		public void setId(String id) {
				this.id = id;
		}
		
		public String getLinkRole() {
				return linkRole;
		}
		
		public void setLinkRole(String linkRole) {
				this.linkRole = linkRole;
		}

		public boolean equals(Object linkedWorkItem) {
				if (linkedWorkItem instanceof LinkedWorkItem) {
						return (this.id.equals(((LinkedWorkItem)linkedWorkItem).getId()));
				}
				return false;
		}

		public int hashCode() {
				return id.hashCode();
		}
		
}