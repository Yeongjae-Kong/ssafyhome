package com.ssafy.home.model.service;

import com.ssafy.home.external.molit.MolitAptTradeClient;
import com.ssafy.home.model.dao.DongCodeDao;
import com.ssafy.home.model.dao.FavoriteDao;
import com.ssafy.home.model.dao.HouseInfoDao;
import com.ssafy.home.model.dto.DongCode;
import com.ssafy.home.model.dto.FavoriteApartment;
import com.ssafy.home.model.dto.HouseInfo;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FavoriteApartmentServiceImpl implements FavoriteApartmentService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteApartmentServiceImpl.class);

    private final FavoriteDao favoriteDao;
    private final HouseInfoDao houseInfoDao;
    private final DongCodeDao dongCodeDao;
    private final MolitAptTradeClient molitClient;

    public FavoriteApartmentServiceImpl(FavoriteDao favoriteDao,
                                        HouseInfoDao houseInfoDao,
                                        DongCodeDao dongCodeDao,
                                        MolitAptTradeClient molitClient) {
        this.favoriteDao = favoriteDao;
        this.houseInfoDao = houseInfoDao;
        this.dongCodeDao = dongCodeDao;
        this.molitClient = molitClient;
    }

    @Override
    public List<FavoriteApartment> list(int userId) {
        return favoriteDao.selectByUserId(userId);
    }

    @Override
    @Transactional
    public FavoriteApartment add(int userId, String aptSeq) {
        FavoriteApartment exists = favoriteDao.selectByUnique(userId, aptSeq);
        if (exists != null) return exists;

        HouseInfo info = houseInfoDao.selectById(aptSeq);
        if (info == null) throw new IllegalArgumentException("apartment not found");

        String sgg = info.getSggCd();
        String umd = info.getUmdCd();
        String dongCode = (sgg == null || umd == null) ? null : sgg + umd;
        String sidoName = null, gugunName = null;
        if (dongCode != null) {
            DongCode dc = dongCodeDao.selectByCode(dongCode);
            if (dc != null) {
                sidoName = dc.getSidoName();
                gugunName = dc.getGugunName();
            }
        }
        // 최소 정보 세팅
        FavoriteApartment f = new FavoriteApartment();
        f.setUserId(userId);
        f.setAptSeq(aptSeq);
        f.setAptName(info.getAptNm());
        f.setSido(sidoName == null ? "" : sidoName);
        f.setGugun(gugunName == null ? "" : gugunName);
        f.setDong(info.getUmdNm());
        f.setBuildYear(info.getBuildYear());

        // 최근 거래(월)
        try {
            if (sgg == null || sgg.isBlank()) {
                log.warn("[favorites:add] sggCd is null/blank for aptSeq={}, aptName={}", aptSeq, info.getAptNm());
            }
            var latest = findLatestTrade(sgg, info, 12);
            if (latest != null) {
                Long amount = parseLong(latest.getDealAmount());
                if (amount != null) amount = amount * 10000L; // MOLIT 금액 단위(만원) → 원 변환
                String ym = String.format("%04d%02d", parseInt(latest.getYear()), parseInt(latest.getMonth()));
                f.setLastDealAmountKrw(amount);
                f.setLastDealMonth(ym);
                // 면적(㎡) → 평 변환(1평 = 3.3㎡)
                Double m2 = parseDouble(latest.getExclusiveArea());
                if (m2 != null) {
                    double p = m2 / 3.3;
                    f.setLastDealAreaPyeong(Math.round(p * 100.0) / 100.0);
                }
                log.info("[favorites:add] latest trade found: aptSeq={}, sggCd={}, ym={}, amount={}, areaM2={}", aptSeq, sgg, ym, amount, latest.getExclusiveArea());
            } else {
                log.warn("[favorites:add] no recent trade found within {} months for aptSeq={}, sggCd={}, aptName={}", 12, aptSeq, sgg, info.getAptNm());
            }
        } catch (Exception e) {
            log.warn("[favorites:add] failed to fetch latest trade for aptSeq={}, sggCd={}, aptName={}, error={}", aptSeq, sgg, info.getAptNm(), e.toString());
        }

        favoriteDao.insert(f);
        return f;
    }

    @Override
    @Transactional
    public boolean remove(int userId, int favoriteId) {
        return favoriteDao.deleteByIdAndUserId(favoriteId, userId) > 0;
    }

    private MolitAptTradeClient.Record findLatestTrade(String sggCd, HouseInfo info, int monthsBack) {
        if (sggCd == null || info == null) return null;
        String aptName = info.getAptNm();
        String dongName = info.getUmdNm();
        String jibun = info.getJibun();
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        for (int i = 0; i < monthsBack; i++) {
            LocalDate t = now.minusMonths(i);
            String yyyymm = t.format(fmt);
            int page = 1;
            int size = 1500; // try larger page size to include more records
            MolitAptTradeClient.Record best = null;
            while (true) {
                List<MolitAptTradeClient.Record> rows = molitClient.fetch(sggCd, yyyymm, page, size);
                if (rows == null || rows.isEmpty()) {
                    if (page == 1) log.debug("[favorites:add] molit fetch empty: sggCd={}, yyyymm={}", sggCd, yyyymm);
                    break;
                } else {
                    if (page == 1) log.debug("[favorites:add] molit fetch rows: sggCd={}, yyyymm={}, count(page1)={}", sggCd, yyyymm, rows.size());
                }
                for (var r : rows) {
                    boolean nameOk = matchesAptName(aptName, r.getAptName());
                    boolean dongOk = sameDong(dongName, r.getDong());
                    boolean jibunOk = jibunMatches(jibun, r.getJibun());
                    if (nameOk || jibunOk || dongOk) {
                        if (best == null) best = r;
                        else {
                            int d1 = parseInt(best.getDay());
                            int d2 = parseInt(r.getDay());
                            if (d2 > d1) best = r;
                        }
                    }
                }
                if (rows.size() < size || page >= 10) break;
                page++;
            }
            if (best != null) return best;
        }
        return null;
    }

    private static boolean matchesAptName(String a, String b) {
        if (a == null || b == null) return false;
        String na = normalize(a), nb = normalize(b);
        if (na.equals(nb)) return true;
        int min = Math.min(na.length(), nb.length());
        int max = Math.max(na.length(), nb.length());
        boolean similar = (min * 100 / Math.max(1, max)) >= 70;
        return similar && (na.contains(nb) || nb.contains(na));
    }

    private static boolean sameDong(String a, String b) {
        if (a == null || b == null) return false;
        String na = normalize(a);
        String nb = normalize(b);
        return !na.isEmpty() && na.equals(nb);
    }

    private static boolean jibunMatches(String a, String b) {
        if (a == null || b == null) return false;
        String na = a.trim();
        String nb = b.trim();
        // compare main number (bonbun). e.g., "123-45" -> 123
        Integer ba = parseInt(na.split("-", 2)[0]);
        Integer bb = parseInt(nb.split("-", 2)[0]);
        if (ba != null && bb != null && ba.equals(bb)) return true;
        // if exact match after removing spaces
        String ra = na.replaceAll("\\s", "");
        String rb = nb.replaceAll("\\s", "");
        return ra.equals(rb);
    }
    private static String normalize(String s) {
        String x = s.replaceAll("\\(.*?\\)", "");
        x = x.replaceAll("[^0-9A-Za-z가-힣]", "");
        return x.toUpperCase().trim();
    }
    private static Integer parseInt(String s) { try { return s==null? null: Integer.parseInt(s); } catch (Exception e){ return null; } }
    private static Long parseLong(String s) { try { return s==null? null: Long.parseLong(s); } catch (Exception e){ return null; } }
    private static Double parseDouble(String s) { try { return s==null? null: Double.parseDouble(s); } catch (Exception e){ return null; } }
}
