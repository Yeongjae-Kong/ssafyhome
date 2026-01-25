package com.ssafy.home.model.dto;

public class SuggestionItem {
    private String type; // DONG | APT | ROAD
    private String code; // dong_code or apt_seq
    private String name;

    public SuggestionItem() {}
    public SuggestionItem(String type, String code, String name) {
        this.type = type;
        this.code = code;
        this.name = name;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

