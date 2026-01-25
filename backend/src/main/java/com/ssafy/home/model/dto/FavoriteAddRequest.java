package com.ssafy.home.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class FavoriteAddRequest {
    @NotBlank
    @Schema(description = "아파트 코드(apt_seq)", example = "A123456789")
    private String aptSeq;

    public String getAptSeq() { return aptSeq; }
    public void setAptSeq(String aptSeq) { this.aptSeq = aptSeq; }
}

