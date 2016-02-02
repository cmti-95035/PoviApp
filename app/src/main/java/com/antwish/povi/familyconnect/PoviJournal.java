package com.antwish.povi.familyconnect;

public class PoviJournal{
    private String title;
    private String date;

    public PoviJournal(String title, String date) {
        this.date = date;
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
