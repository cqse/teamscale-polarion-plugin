package com.teamscale.polarion.plugin.model;

public class LinkBundle {
		
		private boolean added;
		private LinkedWorkItem linkedWorkItem;
		private String revision;
		
		public void setAdded(boolean added) {
				this.added = added;
		}
		
		public boolean isAdded() {
				return added;
		}
		
		public void setLinkedWorkItem(LinkedWorkItem linkedWorkItem) {
				this.linkedWorkItem = linkedWorkItem;
		}
		
		public LinkedWorkItem getLinkedWorkItem() {
				return linkedWorkItem;
		}
		
		public void setRevision(String revision) {
				this.revision = revision;
		}
		
		public String getRevision() {
				return revision;
		}
}