package com.example.postoffline;

import java.util.List;

public class DateGroup {
    private String date;
    private List<Item> items;

    public DateGroup(String date, List<Item> items) {
        this.date = date;
        this.items = items;
    }

    public String getDate() {
        return date;
    }

    public List<Item> getItems() {
        return items;
    }
}
