package com.example.snaplog;

public class Item {
    private int id;
    private String image;
    private String weight;
    private String date;

    public Item(int id, String image, String weight, String date) {
        this.id = id;
        this.image = image;
        this.weight = weight;
        this.date = date;
    }


    public int getId() {
        return id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getWeight() {
        return weight;
    }

    public String getDate() {
        return date;
    }
}
