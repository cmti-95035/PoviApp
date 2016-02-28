package com.antwish.povi.familyconnect;

public class PoviStory {
    private String title;
    private String category;
    private String author;
    private String fullStory;
    private String poviResponse;
    private String[] followupQuestions;

    public PoviStory() {
    }

    public PoviStory(String author, String category, String[] followupQuestions, String fullStory, String poviResponse, String title) {
        this.author = author;
        this.category = category;
        this.followupQuestions = followupQuestions;
        this.fullStory = fullStory;
        this.poviResponse = poviResponse;
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String[] getFollowupQuestions() {
        return followupQuestions;
    }

    public void setFollowupQuestions(String[] followupQuestions) {
        this.followupQuestions = followupQuestions;
    }

    public String getFullStory() {
        return fullStory;
    }

    public void setFullStory(String fullStory) {
        this.fullStory = fullStory;
    }

    public String getPoviResponse() {
        return poviResponse;
    }

    public void setPoviResponse(String poviResponse) {
        this.poviResponse = poviResponse;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
