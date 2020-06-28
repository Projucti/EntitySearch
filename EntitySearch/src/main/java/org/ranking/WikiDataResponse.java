package org.ranking;

public class WikiDataResponse {

	String label;
	String Url;
	String description;
	String unique_identifier;

	public String getUnique_identifier() {
		return unique_identifier;
	}

	public void setUnique_identifier(String unique_identifier) {
		this.unique_identifier = unique_identifier;
	}

	@Override
	public String toString() {
		return "WikiData{" + "label='" + label + '\'' + ", Url='" + Url + '\'' + ", unique_identifier='"
				+ unique_identifier + '\'' + ", description='" + description + '\'' + '}';
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUrl() {
		return Url;
	}

	public void setUrl(String url) {
		Url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
