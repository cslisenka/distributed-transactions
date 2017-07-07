package com.example.axonexample.commands;

public class ComplaintFiledEvent {

    private String identifier;
    private String company;
    private String description;

    public ComplaintFiledEvent(String identifier, String company, String description) {
        this.identifier = identifier;
        this.company = company;
        this.description = description;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCompany() {
        return company;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "ComplaintFiledEvent{" +
                "identifier='" + identifier + '\'' +
                ", company='" + company + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}