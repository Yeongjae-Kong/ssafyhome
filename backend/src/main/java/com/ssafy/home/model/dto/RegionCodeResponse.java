package com.ssafy.home.model.dto;

import java.util.List;

public class RegionCodeResponse {
    private String dongCode; // when single dong specified
    private String sggCd;
    private String umdCd;
    private List<String> dongCodes; // when only gugun specified
    private Integer count;

    public String getDongCode() { return dongCode; }
    public void setDongCode(String dongCode) { this.dongCode = dongCode; }
    public String getSggCd() { return sggCd; }
    public void setSggCd(String sggCd) { this.sggCd = sggCd; }
    public String getUmdCd() { return umdCd; }
    public void setUmdCd(String umdCd) { this.umdCd = umdCd; }
    public List<String> getDongCodes() { return dongCodes; }
    public void setDongCodes(List<String> dongCodes) { this.dongCodes = dongCodes; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}

