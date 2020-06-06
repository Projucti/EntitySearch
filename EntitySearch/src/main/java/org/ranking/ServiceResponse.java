package org.ranking;

import com.fasterxml.jackson.annotation.JsonProperty;

class ServiceResponse {

	@JsonProperty
	String url;
	@JsonProperty
	String label;
	@JsonProperty
	double pagerank;
	@JsonProperty
	String entityType;

	ServiceResponse() {

	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	ServiceResponse(String url, String label, double pagerank, String entityType) {
		this.label = label;
		this.pagerank = pagerank;
		this.url = url;
		this.entityType = entityType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public double getPagerank() {
		return pagerank;
	}

	public void setPagerank(double pagerank) {
		this.pagerank = pagerank;
	}

}
