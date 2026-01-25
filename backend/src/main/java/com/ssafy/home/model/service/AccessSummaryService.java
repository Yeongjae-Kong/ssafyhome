package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.AccessSummaryResponse;

public interface AccessSummaryService {
    AccessSummaryResponse summarize(String aptSeq);
}
