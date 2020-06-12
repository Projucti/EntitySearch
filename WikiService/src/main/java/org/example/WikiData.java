package org.example;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WikiData {
    public String label;
    public String identifier;
    public String description;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
