package com.ssafy.home.model.dto;

import java.util.Date;

public class FavoriteApartment {
    private Integer id;
    private Integer userId;
    private String aptSeq;
    private String aptName;
    private String sido;
    private String gugun;
    private String dong;
    private Long lastDealAmountKrw;
    private String lastDealMonth; // YYYYMM
    private Double lastDealAreaPyeong; // 최근 거래 전용면적(평)
    private Integer buildYear;
    private Date createdAt;
    private Date updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAptSeq() { return aptSeq; }
    public void setAptSeq(String aptSeq) { this.aptSeq = aptSeq; }
    public String getAptName() { return aptName; }
    public void setAptName(String aptName) { this.aptName = aptName; }
    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getGugun() { return gugun; }
    public void setGugun(String gugun) { this.gugun = gugun; }
    public String getDong() { return dong; }
    public void setDong(String dong) { this.dong = dong; }
    public Long getLastDealAmountKrw() { return lastDealAmountKrw; }
    public void setLastDealAmountKrw(Long lastDealAmountKrw) { this.lastDealAmountKrw = lastDealAmountKrw; }
    public String getLastDealMonth() { return lastDealMonth; }
    public void setLastDealMonth(String lastDealMonth) { this.lastDealMonth = lastDealMonth; }
    public Integer getBuildYear() { return buildYear; }
    public void setBuildYear(Integer buildYear) { this.buildYear = buildYear; }
    public Double getLastDealAreaPyeong() { return lastDealAreaPyeong; }
    public void setLastDealAreaPyeong(Double lastDealAreaPyeong) { this.lastDealAreaPyeong = lastDealAreaPyeong; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
