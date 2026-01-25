package com.ssafy.home.controller;

import com.ssafy.home.model.dto.*;
import com.ssafy.home.model.service.ItemService;
import com.ssafy.home.model.service.LiveTradeService;
import com.ssafy.home.model.dao.FavoriteDao;
import com.ssafy.home.model.dao.UserDao;
import com.ssafy.home.model.dto.FavoriteApartment;
import com.ssafy.home.model.service.AccessSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/items")
@Tag(name = "아파트", description = "아파트 상세 및 거래 조회")
public class ItemsController {

    private final ItemService itemService;
    private final LiveTradeService liveTradeService;
    private final AccessSummaryService accessSummaryService;
    private final FavoriteDao favoriteDao;
    private final UserDao userDao;

    public ItemsController(ItemService itemService, LiveTradeService liveTradeService, AccessSummaryService accessSummaryService,
                           FavoriteDao favoriteDao, UserDao userDao) {
        this.itemService = itemService;
        this.liveTradeService = liveTradeService;
        this.accessSummaryService = accessSummaryService;
        this.favoriteDao = favoriteDao;
        this.userDao = userDao;
    }

    @GetMapping("/{aptSeq}")
    @Operation(summary = "아파트 상세 조회", description = "withAccess=true 시 생활권 요약을 함께 반환")
    public ResponseEntity<?> getItem(@PathVariable("aptSeq") String aptSeq,
                                     @Parameter(description = "생활권 요약 포함 여부", example = "true")
                                     @RequestParam(value = "withAccess", required = false) Boolean withAccess) {
        HouseInfo info = itemService.getItem(aptSeq);
        if (info == null) return ResponseEntity.notFound().build();
        HouseInfoView view = HouseInfoView.from(info);
        boolean include = Boolean.TRUE.equals(withAccess);
        if (!include) {
            ItemDetailResponse resp = new ItemDetailResponse(view, null);
            enrichFavoriteSnapshot(resp, aptSeq);
            return ResponseEntity.ok(resp);
        }

        try {
            AccessSummaryResponse acc = accessSummaryService.summarize(aptSeq);
            ItemDetailResponse resp = new ItemDetailResponse(view, acc);
            enrichFavoriteSnapshot(resp, aptSeq);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            ItemDetailResponse resp = new ItemDetailResponse(view, null);
            enrichFavoriteSnapshot(resp, aptSeq);
            return ResponseEntity.ok(resp);
        }
    }

    private void enrichFavoriteSnapshot(ItemDetailResponse resp, String aptSeq) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean enriched = false;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String email = auth.getName();
            com.ssafy.home.model.dto.User u = userDao.selectByEmail(email);
            if (u != null) {
                FavoriteApartment fav = favoriteDao.selectByUnique(u.getMno(), aptSeq);
                if (fav != null) {
                    resp.getInfo().setLastDealAmountKrw(fav.getLastDealAmountKrw());
                    resp.getInfo().setLastDealAreaPyeong(fav.getLastDealAreaPyeong());
                    // derive year/month from YYYYMM
                    String yyyymmStr = fav.getLastDealMonth();
                    if (yyyymmStr != null && yyyymmStr.length() == 6) {
                        try {
                            resp.getInfo().setLastDealYear(Integer.parseInt(yyyymmStr.substring(0,4)));
                            resp.getInfo().setLastDealMonthNum(Integer.parseInt(yyyymmStr.substring(4,6)));
                        } catch (Exception ignored) {}
                    }
                    String yyyymm = fav.getLastDealMonth();
                    if (yyyymm != null && yyyymm.length() == 6) {
                        try {
                            var list = liveTradeService.apartmentDeals(aptSeq, yyyymm, 1, 200).getItems();
                            int best = 0;
                            for (var h : list) {
                                if (h.getDealDay() != null && h.getDealDay() > best) best = h.getDealDay();
                            }
                            if (best > 0) resp.getInfo().setLastDealDay(best);
                        } catch (Exception ignored) {}
                    }
                    enriched = true;
                }
            }
        }
        // Fallback for unauthenticated or no favorite snapshot
        if (!enriched || resp.getInfo().getLastDealAmountKrw() == null) {
            enrichWithLiveFallback(resp, aptSeq, 12);
        }
    }

    private void enrichWithLiveFallback(ItemDetailResponse resp, String aptSeq, int monthsBack) {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMM");
        for (int i = 0; i < monthsBack; i++) {
            String period = now.minusMonths(i).format(fmt);
            try {
                var list = liveTradeService.apartmentDeals(aptSeq, period, 1, 500).getItems();
                if (list == null || list.isEmpty()) continue;
                // choose record with max dealDay
                int bestDay = 0;
                com.ssafy.home.model.dto.HouseDeal bestItem = null;
                for (var h : list) {
                    int d = (h.getDealDay() == null) ? 0 : h.getDealDay();
                    if (d >= bestDay) { bestDay = d; bestItem = h; }
                }
                if (bestItem == null) bestItem = list.get(0);
                Long amt = null;
                try {
                    if (bestItem.getDealAmount() != null) {
                        amt = Long.parseLong(bestItem.getDealAmount().replace(",", ""));
                        amt = amt * 10000L;
                    }
                } catch (Exception ignored) {}
                resp.getInfo().setLastDealAmountKrw(amt);
                try {
                    resp.getInfo().setLastDealYear(Integer.parseInt(period.substring(0,4)));
                    resp.getInfo().setLastDealMonthNum(Integer.parseInt(period.substring(4,6)));
                } catch (Exception ignored) {}
                if (bestDay > 0) resp.getInfo().setLastDealDay(bestDay);
                if (bestItem.getExcluUseAr() != null) {
                    double p = bestItem.getExcluUseAr().doubleValue() / 3.3;
                    resp.getInfo().setLastDealAreaPyeong(Math.round(p * 100.0) / 100.0);
                }
                return;
            } catch (Exception ignored) {}
        }
    }

    @GetMapping("/{aptSeq}/transactions")
    @Operation(summary = "아파트 거래 목록(월, OpenAPI)")
    public ResponseEntity<ListResponse<HouseDeal>> getTransactions(@PathVariable("aptSeq") String aptSeq,
                                                                   @Parameter(description = "YYYYMM", example = "202501")
                                                                   @RequestParam("period") String period,
                                                                   @Parameter(description = "페이지 번호(1부터)", example = "1")
                                                                   @RequestParam(value = "page", required = false) Integer page,
                                                                   @Parameter(description = "페이지 크기", example = "100")
                                                                   @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(liveTradeService.apartmentDeals(aptSeq, period, page, size));
    }

    @GetMapping("/{aptSeq}/transactions/recent")
    @Operation(summary = "아파트 거래 목록(최근 N년, 최대 100건)", description = "최근 N년(기본 1년) 범위에서 최신 순으로 최대 limit건(기본 100) 수집")
    public ResponseEntity<ListResponse<HouseDeal>> getRecentTransactions(@PathVariable("aptSeq") String aptSeq,
                                                                         @Parameter(description = "수집 연한(년)", example = "1")
                                                                         @RequestParam(value = "years", required = false) Integer years,
                                                                         @Parameter(description = "최대 건수", example = "100")
                                                                         @RequestParam(value = "limit", required = false) Integer limit) {
        // Default 1 year, cap at 3 years for performance
        int y = years == null ? 1 : Math.max(1, Math.min(3, years));
        int cap = limit == null ? 100 : Math.max(1, Math.min(1000, limit));
        int months = y * 12;
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        List<String> periods = new ArrayList<>();
        for (int i = 0; i < months; i++) periods.add(now.minusMonths(i).format(fmt));

        // Parallel fetch monthly pages to reduce latency
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(6, periods.size()));
        List<Future<List<HouseDeal>>> futures = new ArrayList<>();
        for (String p : periods) {
            Callable<List<HouseDeal>> task = () -> {
                try {
                    var lr = liveTradeService.apartmentDeals(aptSeq, p, 1, 500);
                    return lr == null ? List.of() : (lr.getItems() == null ? List.of() : lr.getItems());
                } catch (Exception e) {
                    return List.of();
                }
            };
            futures.add(pool.submit(task));
        }
        List<HouseDeal> all = new ArrayList<>();
        for (Future<List<HouseDeal>> f : futures) {
            try {
                List<HouseDeal> items = f.get();
                if (items != null && !items.isEmpty()) all.addAll(items);
            } catch (InterruptedException | ExecutionException ignored) {}
        }
        pool.shutdown();
        // 정렬: 최신(연,월,일) 내림차순
        all.sort(Comparator
                .comparing(HouseDeal::getDealYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HouseDeal::getDealMonth, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HouseDeal::getDealDay, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());
        if (all.size() > cap) all = all.subList(0, cap);
        return ResponseEntity.ok(new ListResponse<>(all));
    }
}
