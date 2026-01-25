package com.ssafy.home.model.dto;

public class AptTradeRecord {
    private String dealAmount; // 원(문자열 숫자)
    private int dealYear;
    private int dealMonth;
    private int dealDay;
    private String aptName;
    private Double exclusiveArea; // ㎡
    private Integer floor;
    private String dong;
    private String jibun;
    private String roadName;
    private String lawdCd; // 시군구코드(5)
    private String serialNo;

    public String getDealAmount() { return dealAmount; }
    public void setDealAmount(String dealAmount) { this.dealAmount = dealAmount; }
    public int getDealYear() { return dealYear; }
    public void setDealYear(int dealYear) { this.dealYear = dealYear; }
    public int getDealMonth() { return dealMonth; }
    public void setDealMonth(int dealMonth) { this.dealMonth = dealMonth; }
    public int getDealDay() { return dealDay; }
    public void setDealDay(int dealDay) { this.dealDay = dealDay; }
    public String getAptName() { return aptName; }
    public void setAptName(String aptName) { this.aptName = aptName; }
    public Double getExclusiveArea() { return exclusiveArea; }
    public void setExclusiveArea(Double exclusiveArea) { this.exclusiveArea = exclusiveArea; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public String getDong() { return dong; }
    public void setDong(String dong) { this.dong = dong; }
    public String getJibun() { return jibun; }
    public void setJibun(String jibun) { this.jibun = jibun; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public String getLawdCd() { return lawdCd; }
    public void setLawdCd(String lawdCd) { this.lawdCd = lawdCd; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
}

