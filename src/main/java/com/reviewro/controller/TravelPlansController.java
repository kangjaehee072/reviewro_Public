package com.reviewro.controller;


import com.reviewro.dto.SchedulePlansDTO;
import com.reviewro.dto.TravelPlansDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.LocationBookMarkService;
import com.reviewro.service.TravelPlansService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;


@RestController
@RequiredArgsConstructor
@Log4j2
public class TravelPlansController {

    private final TravelPlansService travelPlansService;


    //내 여행 계획 목록 조회
    @GetMapping("/api/travels")
    public ResponseEntity<List<TravelPlansDTO>> getTravelPlan(@AuthenticationPrincipal MemberDetails memberDetails) {
        List<TravelPlansDTO> response = travelPlansService.getTravelPlanList(memberDetails.getMember().getMemberId());
        log.info("Response: " + response);
        return ResponseEntity.ok(response);
    }

    //새 여행 계획 만들기 (지도 '여행 계획 짜기'에서). 만든 계획 번호를 돌려줘서 화면이 바로 고를 수 있게 한다
    @PostMapping("/api/travels")
    public ResponseEntity<?> createTravelPlan(@RequestBody TravelPlansDTO travelPlansDTO,
                                              @AuthenticationPrincipal MemberDetails memberDetails) {
        String title = travelPlansDTO.getPlanTitle() == null ? "" : travelPlansDTO.getPlanTitle().trim();
        if (title.isEmpty() || title.length() > 100) {
            return ResponseEntity.badRequest().body("여행 제목을 100자 이내로 적어 주세요.");
        }
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(travelPlansDTO.getPlanStartDate());
            end = LocalDate.parse(travelPlansDTO.getPlanEndDate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("출발일과 도착일을 골라 주세요.");
        }
        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().body("도착일이 출발일보다 빠를 수 없어요.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= 30) {
            return ResponseEntity.badRequest().body("여행 기간은 30일까지 만들 수 있어요.");
        }

        // 사용자가 보낸 번호·회원 값은 쓰지 않는다. 회원은 로그인한 사람으로 정해진다
        travelPlansDTO.setPlanId(null);
        travelPlansDTO.setMemberId(null);
        travelPlansDTO.setDailyPlans(null);       // 일차는 서버가 날짜로 만든다
        travelPlansDTO.setSchedulePlans(null);
        travelPlansDTO.setPlanTitle(title);
        Long planId = travelPlansService.insertTravelPlan(travelPlansDTO, memberDetails.getMember().getMemberUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(planId);
    }

    //특정 여행 계획 상세 조회
    @GetMapping("/api/travels/{planId}")
    public ResponseEntity<TravelPlansDTO> getTravelPlanById(@PathVariable("planId") Long planId,
                                                            @AuthenticationPrincipal MemberDetails memberDetails) {
        TravelPlansDTO response = travelPlansService.readSchedule(planId, memberDetails.getMember().getMemberId());
        return ResponseEntity.ok(response);
    }

    //특정 일차에 세부 일정(장소) 추가
    @PostMapping("/api/travels/dailies/{dailyId}/schedules")
    public ResponseEntity<Long> addSchedule(@PathVariable("dailyId") Long dailyId,
                                            @AuthenticationPrincipal MemberDetails details,
                                            @RequestBody SchedulePlansDTO schedulePlansDTO) {
        Long scheduleId = travelPlansService.addSchedule(dailyId, details.getMember().getMemberId(), schedulePlansDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleId);
    }


    //세부 일정 순서 변경 (드래그 앤 드롭 대응)
    @PutMapping("/api/travels/{planId}/schedules")
    public ResponseEntity<Void> updateSchedule(@PathVariable("planId") Long planId,
                                               @AuthenticationPrincipal MemberDetails details,
                                               @RequestBody List<SchedulePlansDTO> schedulePlansDTO) {
        log.info("~~~~~~~~컨트롤러 도달~~~~~~~~", planId);
        travelPlansService.updateAllSchedule(planId,details.getMember().getMemberId(), schedulePlansDTO);
        return ResponseEntity.ok().build();
    }

    //세부 일정 삭제
    //세부 일정 시간 바꾸기 (본인 일정만). body: {"scheduleStartTime": "10:00", "scheduleEndTime": "12:00"}
    @PatchMapping("/api/travels/schedules/{scheduleId}/time")
    public ResponseEntity<String> updateScheduleTime(@PathVariable("scheduleId") Long scheduleId,
                                                     @RequestBody SchedulePlansDTO body,
                                                     @AuthenticationPrincipal MemberDetails details) {
        try {
            travelPlansService.updateScheduleTime(scheduleId, details.getMember().getMemberId(),
                    body.getScheduleStartTime(), body.getScheduleEndTime());
            return ResponseEntity.ok("시간을 바꿨어요.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/travels/schedules/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable("scheduleId") Long scheduleId,
                                               @AuthenticationPrincipal MemberDetails details){
        travelPlansService.deleteSchedule(scheduleId, details.getMember().getMemberId());
        return ResponseEntity.ok().build();
    }




}
