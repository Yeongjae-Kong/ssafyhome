package com.ssafy.home.model.dto;

public class ItemDetailResponse {
    private HouseInfoView info;
    private AccessSummaryResponse access;

    public ItemDetailResponse() {}
    public ItemDetailResponse(HouseInfoView info, AccessSummaryResponse access) {
        this.info = info;
        this.access = access;
    }

    public HouseInfoView getInfo() { return info; }
    public void setInfo(HouseInfoView info) { this.info = info; }
    public AccessSummaryResponse getAccess() { return access; }
    public void setAccess(AccessSummaryResponse access) { this.access = access; }
}
