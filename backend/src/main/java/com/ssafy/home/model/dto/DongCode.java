package com.ssafy.home.model.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dongcodes")
public class DongCode {
    @Id
    @Column(name = "dong_code", length = 10)
    private String dongCode;

    @Column(name = "sido_name", length = 30)
    private String sidoName;

    @Column(name = "gugun_name", length = 30)
    private String gugunName;

    @Column(name = "dong_name", length = 30)
    private String dongName;

    public String getDongCode() { return dongCode; }
    public void setDongCode(String dongCode) { this.dongCode = dongCode; }
    public String getSidoName() { return sidoName; }
    public void setSidoName(String sidoName) { this.sidoName = sidoName; }
    public String getGugunName() { return gugunName; }
    public void setGugunName(String gugunName) { this.gugunName = gugunName; }
    public String getDongName() { return dongName; }
    public void setDongName(String dongName) { this.dongName = dongName; }
}
