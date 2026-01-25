package com.ssafy.home.model.dto;

// Read-only DTO for item detail: base house info + last deal snapshot
public class HouseInfoView {
    // Base house info fields
    private String aptSeq;
    private String sggCd;
    private String umdCd;
    private String umdNm;
    private String jibun;
    private String roadNmSggCd;
    private String roadNm;
    private String roadNmBonbun;
    private String roadNmBubun;
    private String aptNm;
    private Integer buildYear;
    private String latitude;
    private String longitude;

    // Last deal snapshot fields
    private Long lastDealAmountKrw;
    private Integer lastDealYear;
    private Integer lastDealMonthNum;
    private Integer lastDealDay;
    private Double lastDealAreaPyeong;

    public static HouseInfoView from(HouseInfo h) {
        HouseInfoView v = new HouseInfoView();
        if (h == null) return v;
        v.setAptSeq(h.getAptSeq());
        v.setSggCd(h.getSggCd());
        v.setUmdCd(h.getUmdCd());
        v.setUmdNm(h.getUmdNm());
        v.setJibun(h.getJibun());
        v.setRoadNmSggCd(h.getRoadNmSggCd());
        v.setRoadNm(h.getRoadNm());
        v.setRoadNmBonbun(h.getRoadNmBonbun());
        v.setRoadNmBubun(h.getRoadNmBubun());
        v.setAptNm(h.getAptNm());
        v.setBuildYear(h.getBuildYear());
        v.setLatitude(h.getLatitude());
        v.setLongitude(h.getLongitude());
        return v;
    }

    public String getAptSeq() { return aptSeq; }
    public void setAptSeq(String aptSeq) { this.aptSeq = aptSeq; }
    public String getSggCd() { return sggCd; }
    public void setSggCd(String sggCd) { this.sggCd = sggCd; }
    public String getUmdCd() { return umdCd; }
    public void setUmdCd(String umdCd) { this.umdCd = umdCd; }
    public String getUmdNm() { return umdNm; }
    public void setUmdNm(String umdNm) { this.umdNm = umdNm; }
    public String getJibun() { return jibun; }
    public void setJibun(String jibun) { this.jibun = jibun; }
    public String getRoadNmSggCd() { return roadNmSggCd; }
    public void setRoadNmSggCd(String roadNmSggCd) { this.roadNmSggCd = roadNmSggCd; }
    public String getRoadNm() { return roadNm; }
    public void setRoadNm(String roadNm) { this.roadNm = roadNm; }
    public String getRoadNmBonbun() { return roadNmBonbun; }
    public void setRoadNmBonbun(String roadNmBonbun) { this.roadNmBonbun = roadNmBonbun; }
    public String getRoadNmBubun() { return roadNmBubun; }
    public void setRoadNmBubun(String roadNmBubun) { this.roadNmBubun = roadNmBubun; }
    public String getAptNm() { return aptNm; }
    public void setAptNm(String aptNm) { this.aptNm = aptNm; }
    public Integer getBuildYear() { return buildYear; }
    public void setBuildYear(Integer buildYear) { this.buildYear = buildYear; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public Long getLastDealAmountKrw() { return lastDealAmountKrw; }
    public void setLastDealAmountKrw(Long lastDealAmountKrw) { this.lastDealAmountKrw = lastDealAmountKrw; }
    public Integer getLastDealYear() { return lastDealYear; }
    public void setLastDealYear(Integer lastDealYear) { this.lastDealYear = lastDealYear; }
    public Integer getLastDealMonthNum() { return lastDealMonthNum; }
    public void setLastDealMonthNum(Integer lastDealMonthNum) { this.lastDealMonthNum = lastDealMonthNum; }
    public Integer getLastDealDay() { return lastDealDay; }
    public void setLastDealDay(Integer lastDealDay) { this.lastDealDay = lastDealDay; }
    public Double getLastDealAreaPyeong() { return lastDealAreaPyeong; }
    public void setLastDealAreaPyeong(Double lastDealAreaPyeong) { this.lastDealAreaPyeong = lastDealAreaPyeong; }
}

