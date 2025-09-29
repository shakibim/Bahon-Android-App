package com.example.bahon;



public class RechargeData {
    private String dateTime;
    private String amount;

    public RechargeData(String dateTime, String amount) {
        this.dateTime = dateTime;
        this.amount = amount;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getAmount() {
        return amount;
    }
}