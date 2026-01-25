package com.ssafy.home.model.dto;

import java.util.List;
import java.util.Map;

public class DealsByAptResponse {
    private String sido;
    private String gugun;
    private int months;
    private Map<String, List<HouseDeal>> items;

    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getGugun() { return gugun; }
    public void setGugun(String gugun) { this.gugun = gugun; }
    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }
    public Map<String, List<HouseDeal>> getItems() { return items; }
    public void setItems(Map<String, List<HouseDeal>> items) { this.items = items; }
}

