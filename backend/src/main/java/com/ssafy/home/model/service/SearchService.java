package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.SuggestionItem;
import com.ssafy.home.model.dto.HouseInfo;

import java.util.List;

public interface SearchService {
    ListResponse<HouseInfo> search(String q, Integer page, Integer size, String sort);
    ListResponse<HouseInfo> searchInRegion(String sido, String gugun, String dong, String q, Integer page, Integer size);
    List<SuggestionItem> suggestions(String q, Integer limit);
}
