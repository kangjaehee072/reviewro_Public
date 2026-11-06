package com.reviewro.service;

import com.reviewro.entity.TravelPlansEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final EntityManager em;

    // 마이페이지 숫자 요약
    public Map<String, Object> summary(Long memberId) {
        Map<String, Object> s = new HashMap<>();
        s.put("travelPlans", count("TravelPlansEntity", memberId));
        s.put("bookmarks", count("LocationBookMarkEntity", memberId));
        s.put("photoMaps", count("PhotoMapEntity", memberId));
        s.put("reviews", count("ReviewEntity", memberId));
        s.put("posts", count("FreeBoardEntity", memberId) + count("CompanionPostEntity", memberId));
        s.put("tripPlans", count("TripPlanEntity", memberId));
        s.put("tripPlanShares", em.createQuery(
                "select coalesce(sum(t.tripplanShareCount), 0) from TripPlanEntity t where t.member.memberId = :id", Long.class)
                .setParameter("id", memberId).getSingleResult());
        s.put("recentBookmarks", em.createQuery(
                "select b.location.locationName from LocationBookMarkEntity b " +
                "where b.member.memberId = :id order by b.bookmarkId desc", String.class)
                .setParameter("id", memberId).setMaxResults(3).getResultList());
        return s;
    }

    // 끝나지 않은 여행 중 가장 가까운 것. 없으면 null
    public Map<String, Object> nextTrip(Long memberId) {
        LocalDate today = LocalDate.now();
        List<TravelPlansEntity> found = em.createQuery(
                "select t from TravelPlansEntity t where t.member.memberId = :id " +
                "and t.planStartDate is not null and t.planEndDate >= :today order by t.planStartDate asc", TravelPlansEntity.class)
                .setParameter("id", memberId)
                .setParameter("today", today.toString())
                .setMaxResults(1)
                .getResultList();
        if (found.isEmpty()) {
            return null;
        }

        TravelPlansEntity plan = found.get(0);
        LocalDate start = LocalDate.parse(plan.getPlanStartDate());
        LocalDate end = LocalDate.parse(plan.getPlanEndDate());
        long dDay = ChronoUnit.DAYS.between(today, start);

        Map<String, Object> trip = new HashMap<>();
        trip.put("planId", plan.getPlanId());
        trip.put("title", plan.getPlanTitle());
        trip.put("start", start);
        trip.put("end", end);
        trip.put("days", ChronoUnit.DAYS.between(start, end) + 1);
        trip.put("dDay", dDay <= 0 ? "D-DAY" : "D-" + dDay);
        trip.put("traveling", dDay <= 0);
        trip.put("placeCount", em.createQuery(
                "select count(s) from SchedulePlansEntity s where s.dailyPlansEntity.travelPlan.planId = :pid", Long.class)
                .setParameter("pid", plan.getPlanId()).getSingleResult());
        trip.put("places", em.createQuery(
                "select s.location.locationName from SchedulePlansEntity s " +
                "where s.dailyPlansEntity.travelPlan.planId = :pid " +
                "order by s.dailyPlansEntity.dailyNumber, s.scheduleNumber", String.class)
                .setParameter("pid", plan.getPlanId()).setMaxResults(4).getResultList());
        return trip;
    }

    private long count(String entity, Long memberId) {
        return em.createQuery("select count(e) from " + entity + " e where e.member.memberId = :id", Long.class)
                .setParameter("id", memberId)
                .getSingleResult();
    }
}
