package com.ssafy.home.model.dto;

import java.util.List;

public class ListResponse<T> {
    private int count;
    private List<T> items;

    public ListResponse() {}
    public ListResponse(List<T> items) {
        this.items = items;
        this.count = items == null ? 0 : items.size();
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
}

