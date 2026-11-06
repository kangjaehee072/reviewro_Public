package com.reviewro.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final EntityManager em;

    // 관리자 대시보드 KPI
    public Map<String, Object> kpi() {
        LocalDateTime weekAgo = LocalDate.now().minusDays(6).atStartOfDay();

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("memberTotal", count("MemberEntity", null));
        kpi.put("memberWeek", count("MemberEntity", weekAgo));
        kpi.put("reviewTotal", count("ReviewEntity", null));
        kpi.put("reviewWeek", count("ReviewEntity", weekAgo));
        kpi.put("postTotal", count("FreeBoardEntity", null) + count("CompanionPostEntity", null));
        kpi.put("postWeek", count("FreeBoardEntity", weekAgo) + count("CompanionPostEntity", weekAgo));
        kpi.put("tripPlanTotal", count("TripPlanEntity", null));
        kpi.put("tripPlanShare", em.createQuery(
                "select coalesce(sum(t.tripplanShareCount), 0) from TripPlanEntity t", Long.class)
                .getSingleResult());
        kpi.put("travelPlanTotal", count("TravelPlansEntity", null));
        return kpi;
    }

    // 최근 7일 신규 가입자 (요일 라벨 -> 인원)
    public List<Map<String, Object>> weeklySignups() {
        LocalDate today = LocalDate.now();
        List<LocalDateTime> createdList = em.createQuery(
                "select m.createdAt from MemberEntity m where m.createdAt >= :since", LocalDateTime.class)
                .setParameter("since", today.minusDays(6).atStartOfDay())
                .getResultList();

        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            byDay.put(today.minusDays(i), 0L);
        }
        for (LocalDateTime created : createdList) {
            byDay.computeIfPresent(created.toLocalDate(), (d, c) -> c + 1);
        }

        long max = byDay.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<Map<String, Object>> rows = new ArrayList<>();
        byDay.forEach((day, cnt) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("label", day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN));
            row.put("count", cnt);
            row.put("percent", max == 0 ? 0 : cnt * 100 / max);
            row.put("today", day.equals(today));
            rows.add(row);
        });
        return rows;
    }

    // 많이 쓰인 해시태그 상위 4개
    public List<Map<String, Object>> topHashTags() {
        List<Object[]> found = em.createQuery(
                "select h.hashTagName, coalesce(h.hashTagUseCnt, 0) from HashTagEntity h " +
                "order by coalesce(h.hashTagUseCnt, 0) desc", Object[].class)
                .setMaxResults(4)
                .getResultList();

        long max = found.isEmpty() ? 0 : ((Number) found.get(0)[1]).longValue();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] f : found) {
            long cnt = ((Number) f[1]).longValue();
            Map<String, Object> row = new HashMap<>();
            row.put("name", f[0]);
            row.put("count", cnt);
            row.put("percent", max == 0 ? 0 : cnt * 100 / max);
            rows.add(row);
        }
        return rows;
    }

    private long count(String entity, LocalDateTime since) {
        if (since == null) {
            return em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
        }
        return em.createQuery("select count(e) from " + entity + " e where e.createdAt >= :since", Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }
}
