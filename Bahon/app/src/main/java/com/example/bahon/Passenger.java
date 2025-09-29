package com.example.bahon;

public class Passenger {

    private String name;
    private String card_no;
    private boolean onjourney;

    // Default constructor for Firebase
    public Passenger() {
    }

    public Passenger(String name, String card_no, boolean onjourney) {
        this.name = name;
        this.card_no = card_no;
        this.onjourney = onjourney;
    }

    // Getters and setters for Firebase data binding
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCard_no() {
        return card_no;
    }

    public void setCard_no(String card_no) {
        this.card_no = card_no;
    }

    public boolean isOnjourney() {
        return onjourney;
    }

    public void setOnjourney(boolean onjourney) {
        this.onjourney = onjourney;
    }
}