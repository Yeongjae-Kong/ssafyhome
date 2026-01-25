package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.SuggestionItem;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dao.DongCodeDao;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.service.SearchService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private final HouseInfoDao houseInfoDao;
    private final DongCodeDao dongCodeDao;

    public SearchServiceImpl(HouseInfoDao houseInfoDao, DongCodeDao dongCodeDao) {
        this.houseInfoDao = houseInfoDao;
        this.dongCodeDao = dongCodeDao;
    }

    @Override
    public ListResponse<HouseInfo> search(String q, Integer page, Integer size, String sort) {
        String term = q == null ? "" : q.trim();
        List<HouseInfo> all = houseInfoDao.searchByKeyword(term);
        log.debug("[search] term='{}' -> {} rows (pre-paging)", term, all == null ? 0 : all.size());
        // Simple in-memory pagination for now
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? 20 : Math.max(size, 1);
        int from = Math.min(p * s, all.size());
        int to = Math.min(from + s, all.size());
        List<HouseInfo> slice = all.subList(from, to);
        log.debug("[search] paging p={}, s={} -> {} rows", p, s, slice.size());
        return new ListResponse<>(slice);
    }

    @Override
    public ListResponse<HouseInfo> searchInRegion(String sido, String gugun, String dong, String q, Integer page, Integer size) {
        String term = q == null ? "" : q.trim();
        List<HouseInfo> all;
        // Resolve sgg/umd code
        if (dong != null && !dong.isBlank()) {
            com.ssafy.home.model.dto.DongCode d = dongCodeDao.selectBySidoGugunDong(sido, gugun, dong);
            if (d == null || d.getDongCode() == null || d.getDongCode().length() < 10) {
                all = List.of();
            } else {
                String code = d.getDongCode();
                String sgg = code.substring(0, 5);
                String umd = code.substring(5);
                all = houseInfoDao.searchByKeywordInUmd(sgg, umd, term);
            }
        } else {
            java.util.List<com.ssafy.home.model.dto.DongCode> list = dongCodeDao.selectBySidoGugun(sido, gugun);
            if (list == null || list.isEmpty() || list.get(0).getDongCode() == null || list.get(0).getDongCode().length() < 5) {
                all = List.of();
            } else {
                String sgg = list.get(0).getDongCode().substring(0, 5);
                all = houseInfoDao.searchByKeywordInSgg(sgg, term);
            }
        }
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? 20 : Math.max(size, 1);
        int from = Math.min(p * s, all.size());
        int to = Math.min(from + s, all.size());
        List<HouseInfo> slice = all.subList(from, to);
        return new ListResponse<>(slice);
    }

    @Override
    public List<SuggestionItem> suggestions(String q, Integer limit) {
        String term = q == null ? "" : q.trim();
        int lim = limit == null ? 10 : Math.max(1, limit);
        List<SuggestionItem> result = new ArrayList<>();
        // DONG suggestions
        dongCodeDao.selectByDongNameLike(term).stream().limit(lim)
                .forEach(d -> result.add(new SuggestionItem(
                        "DONG", d.getDongCode(), d.getSidoName() + " " + d.getGugunName() + " " + d.getDongName()
                )));
        // APT suggestions
        houseInfoDao
                .searchByKeyword(term)
                .stream().limit(lim)
                .forEach(hi -> result.add(new SuggestionItem("APT", hi.getAptSeq(), hi.getAptNm())));
        // Trim to limit
        if (result.size() > lim) return result.subList(0, lim);
        return result;
    }
}
