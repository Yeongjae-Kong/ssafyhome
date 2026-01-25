package com.ssafy.home.model.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "houseinfos")
public class HouseInfo {
    @Id
    @Column(name = "apt_seq", length = 20)
    private String aptSeq;

    @Column(name = "sgg_cd", length = 5)
    private String sggCd;

    @Column(name = "umd_cd", length = 5)
    private String umdCd;

    @Column(name = "umd_nm", length = 20)
    private String umdNm;

    @Column(name = "jibun", length = 10)
    private String jibun;

    @Column(name = "road_nm_sgg_cd", length = 5)
    private String roadNmSggCd;

    @Column(name = "road_nm", length = 20)
    private String roadNm;

    @Column(name = "road_nm_bonbun", length = 10)
    private String roadNmBonbun;

    @Column(name = "road_nm_bubun", length = 10)
    private String roadNmBubun;

    @Column(name = "apt_nm", length = 40)
    private String aptNm;

    @Column(name = "build_year")
    private Integer buildYear;

    @Column(name = "latitude", length = 45)
    private String latitude;

    @Column(name = "longitude", length = 45)
    private String longitude;

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
}
