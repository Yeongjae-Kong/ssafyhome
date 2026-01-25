package com.ssafy.home.model.dto;

public class HouseDeal {
    private Integer no;
    private String aptSeq;
    private String aptName;
    private String aptDong;
    private String floor;
    private Integer dealYear;
    private Integer dealMonth;
    private Integer dealDay;
    private java.math.BigDecimal excluUseAr;
    private String dealAmount;

    public Integer getNo() { return no; }
    public void setNo(Integer no) { this.no = no; }
    public String getAptSeq() { return aptSeq; }
    public void setAptSeq(String aptSeq) { this.aptSeq = aptSeq; }
    public String getAptName() { return aptName; }
    public void setAptName(String aptName) { this.aptName = aptName; }
    public String getAptDong() { return aptDong; }
    public void setAptDong(String aptDong) { this.aptDong = aptDong; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public Integer getDealYear() { return dealYear; }
    public void setDealYear(Integer dealYear) { this.dealYear = dealYear; }
    public Integer getDealMonth() { return dealMonth; }
    public void setDealMonth(Integer dealMonth) { this.dealMonth = dealMonth; }
    public Integer getDealDay() { return dealDay; }
    public void setDealDay(Integer dealDay) { this.dealDay = dealDay; }
    public java.math.BigDecimal getExcluUseAr() { return excluUseAr; }
    public void setExcluUseAr(java.math.BigDecimal excluUseAr) { this.excluUseAr = excluUseAr; }
    public String getDealAmount() { return dealAmount; }
    public void setDealAmount(String dealAmount) { this.dealAmount = dealAmount; }
}
