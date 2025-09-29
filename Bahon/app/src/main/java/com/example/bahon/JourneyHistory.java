package com.example.bahon;

public class JourneyHistory {
    private String dateTime;
    private String journeyDetails;
    private String fare;

    public JourneyHistory(String dateTime, String journeyDetails, String fare) {
        this.dateTime = dateTime;
        this.journeyDetails = journeyDetails;
        this.fare = fare;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getJourneyDetails() {
        return journeyDetails;
    }

    public String getFare() {
        return fare;
    }
}