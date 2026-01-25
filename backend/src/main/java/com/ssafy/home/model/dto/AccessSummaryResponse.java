package com.ssafy.home.model.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AccessSummaryResponse {
    private String aptSeq;
    private Origin origin;
    private List<Item> items = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    public String getAptSeq() { return aptSeq; }
    public void setAptSeq(String aptSeq) { this.aptSeq = aptSeq; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class Origin {
        private double lat;
        private double lon;
        public Origin() {}
        public Origin(double lat, double lon) { this.lat = lat; this.lon = lon; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }

    public static class Item {
        private String type; // SUBWAY/MART/CONVENIENCE/SCHOOL/HOSPITAL/etc
        private String name;
        private Integer distanceM; // 거리
        private String onFootMin; // 분으로 변경(카카오맵 기준)

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDistanceM() { return distanceM; }
        public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }
        public String getOnFootMin() { return onFootMin; }
        public void setOnFootMin(String onFootMin) { this.onFootMin = onFootMin; }
    }
}
