package com.antwish.povi.familyconnect;

public class PoviSubscription {
    private String title;
    private int numberOfStories;
    private String description;
    private float price;
    private int imageId;
    private PoviSubscriptiontype subscriptiontype;

    public PoviSubscription(String description, int imageId, int numberOfStories, float price, PoviSubscriptiontype subscriptiontype, String title) {
        this.description = description;
        this.imageId = imageId;
        this.numberOfStories = numberOfStories;
        this.price = price;
        this.subscriptiontype = subscriptiontype;
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public int getNumberOfStories() {
        return numberOfStories;
    }

    public void setNumberOfStories(int numberOfStories) {
        this.numberOfStories = numberOfStories;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PoviSubscriptiontype getSubscriptiontype() {
        return subscriptiontype;
    }

    public void setSubscriptiontype(PoviSubscriptiontype subscriptiontype) {
        this.subscriptiontype = subscriptiontype;
    }
}
