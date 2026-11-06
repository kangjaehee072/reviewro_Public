package com.reviewro.service;


import com.reviewro.config.AutoMapper;
import com.reviewro.dto.DailyPlansDTO;
import com.reviewro.dto.SchedulePlansDTO;
import com.reviewro.dto.TravelPlansDTO;
import com.reviewro.entity.DailyPlansEntity;
import com.reviewro.entity.LocationEntity;
import com.reviewro.entity.SchedulePlansEntity;
import com.reviewro.entity.TravelPlansEntity;
import com.reviewro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Log4j2
public class TravelPlansService {

    private final TravelPlansRepository travelPlansRepository;
    private final DailyPlansRepository dailyPlansRepository;
    private final SchedulePlansRepository schedulePlansRepository;
    private final LocationRepository locationRepository;
    private final MemberRepository memberRepository;
    private final TravelGroupPlanRepository travelGroupPlanRepository;


    /* 내 여행 계획 목록 조회 */
    @Transactional(readOnly = true)
    public List<TravelPlansDTO> getTravelPlanList(Long memberUserId) {
        List<TravelPlansEntity> travelPlans = travelPlansRepository.findByIdList(memberUserId);

        return travelPlans.stream()
                .map(entity -> AutoMapper.map(entity, TravelPlansDTO.class))
                .toList();
    }

    /* 여행 계획 생성하기 */
    @Transactional
    public Long insertTravelPlan(TravelPlansDTO travelPlansDTO, String memberName) {
        TravelPlansEntity entity = AutoMapper.map(travelPlansDTO, TravelPlansEntity.class);

        // 1. 멤버 세팅
        entity.setMember(memberRepository.findByMemberUserId(memberName)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾지 못했습니다.")));

        // 2. 뷰에서 날짜가 안전하게 넘어왔는지 먼저 체크
        String startStr = travelPlansDTO.getPlanStartDate();
        String endStr = travelPlansDTO.getPlanEndDate();

        if (startStr != null && !startStr.isEmpty() && endStr != null && !endStr.isEmpty()) {
            LocalDate startDate = LocalDate.parse(startStr);
            LocalDate endDate = LocalDate.parse(endStr);

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("여행 시작일은 종료일보다 이후일 수 없습니다.");
            }

            // 만약 엔티티에 dailyPlans 리스트가 초기화되어 있지 않다면 방어 코드로 생성
            if (entity.getDailyPlans() == null) {
                entity.setDailyPlans(new ArrayList<>());
            }

            long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

            for (int i = 0; i < totalDays; i++) {
                LocalDate targetDate = startDate.plusDays(i);

                DailyPlansEntity dailyPlans = DailyPlansEntity.builder()
                        .travelPlan(entity)
                        .dailyNumber(i + 1)
                        .dailyDate(targetDate)
                        .build();

                entity.getDailyPlans().add(dailyPlans);
            }
        }

        return travelPlansRepository.save(entity).getPlanId();
    }


    //여행계획 조회
    @Transactional(readOnly = true)
    public TravelPlansDTO readTravelPlan(Long planId, Long memberId) {
        TravelPlansEntity entity = getOwned(planId, memberId);

        return AutoMapper.map(entity, TravelPlansDTO.class);
    }

    // 여행계획 수정
    @Transactional
    public void updateTravelPlan(Long planId, Long memberId, TravelPlansDTO travelPlansDTO) {

        TravelPlansEntity entity = getOwned(planId, memberId);

        // 폼에 planId 가 섞여 와도 다른 글을 가리키지 못하게 비운다
        travelPlansDTO.setPlanId(null);
        AutoMapper.copyNonNull(travelPlansDTO, entity);

    }


    //여행계획 삭제
    @Transactional
    public void deleteTravelPlan(Long planId, Long memberId) {
        TravelPlansEntity plan = getOwned(planId, memberId);
        // 그룹에 공유해 둔 기록이 계획을 참조하고 있어 먼저 지운다
        travelGroupPlanRepository.deleteByPlan_PlanId(planId);
        travelPlansRepository.delete(plan);
    }

    // 본인 여행계획만 꺼낸다. 남의 것이면 없는 것처럼 대한다
    private TravelPlansEntity getOwned(Long planId, Long memberId) {
        return travelPlansRepository.findByPlanIdAndMemberId(planId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행 계획을 찾을 수 없습니다. id=" + planId));
    }

    // 마이페이지 여행계획 상세조회
    @Transactional(readOnly = true)
    public TravelPlansDTO readSchedule(Long id, Long memberUserId) {

        TravelPlansEntity travelPlans = travelPlansRepository.findByPlanIdAndMemberId(id, memberUserId)
                .orElseThrow(() -> new IllegalArgumentException("여행계획을 찾을 수 없습니다."));

        return buildSchedule(id, travelPlans);
    }

    // 그룹에 공유된 여행계획 상세조회 (그룹원인지, 공유된 계획인지는 호출하는 쪽에서 확인한다)
    @Transactional(readOnly = true)
    public TravelPlansDTO readSharedSchedule(Long id) {
        TravelPlansEntity travelPlans = travelPlansRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("여행계획을 찾을 수 없습니다."));

        return buildSchedule(id, travelPlans);
    }

    // 계획 + 일차 + 일차별 세부일정을 한 DTO 로 묶는다
    private TravelPlansDTO buildSchedule(Long id, TravelPlansEntity travelPlans) {
        List<DailyPlansEntity> dailyPlans = dailyPlansRepository.findByTravelPlan_PlanId(id);

        List<DailyPlansDTO>  dailyPlansDTO = new ArrayList<>();
        List<SchedulePlansDTO> schedulePlansDTO = new ArrayList<>();

        for (DailyPlansEntity dailyPlan : dailyPlans) {
            dailyPlansDTO.add(AutoMapper.map(dailyPlan, DailyPlansDTO.class));
        }

        // 일차가 몇 개든 일정은 한 번에 가져온다 (일차마다 조회하면 일수만큼 쿼리가 나간다)
        List<Long> dailyIds = dailyPlans.stream().map(DailyPlansEntity::getDailyId).toList();
        List<SchedulePlansEntity> schedulePlans = dailyIds.isEmpty()
                ? List.of()
                : schedulePlansRepository.findWithLocationByDailyIdIn(dailyIds);

        for (SchedulePlansEntity schedule : schedulePlans) {
            SchedulePlansDTO dto = AutoMapper.map(schedule, SchedulePlansDTO.class);

            // Entity의 연관관계로부터 dailyId를 가져와서 DTO에 직접 꽂아줍니다.
            if (schedule.getDailyPlansEntity() != null) {
                dto.setDailyId(schedule.getDailyPlansEntity().getDailyId());
            }

            schedulePlansDTO.add(dto);
        }


        TravelPlansDTO travelPlansDTO = AutoMapper.map(travelPlans, TravelPlansDTO.class);
        travelPlansDTO.setDailyPlans(dailyPlansDTO);
        travelPlansDTO.setSchedulePlans(schedulePlansDTO);

        log.info(travelPlansDTO.toString());

        return travelPlansDTO;
    }


    // 특정 일차에 세부 일정(장소) 추가
    @Transactional
    public Long addSchedule(Long dailyId, Long memberId, SchedulePlansDTO schedulePlansDTO) {
        DailyPlansEntity daily = dailyPlansRepository.findById(dailyId)
                .orElseThrow(() -> new IllegalArgumentException("일자정보를 찾을 수 없습니다."));
        if (!daily.getTravelPlan().getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("일자정보를 찾을 수 없습니다.");
        }

        // 현재 일차의 마지막 방문 순서 번호 조회 후 +1
        Integer nextOrder = schedulePlansRepository.findMaxScheduleNumberByDailyId(dailyId).orElse(0) + 1;
        schedulePlansDTO.setScheduleNumber(nextOrder);

        SchedulePlansEntity schedule = AutoMapper.map(schedulePlansDTO, SchedulePlansEntity.class);

        schedule.setDailyPlansEntity(daily);

        schedule.setLocation(locationRepository.findById(schedulePlansDTO.getLocation().getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("장소정보를 찾을 수 없습니다.")));

        SchedulePlansEntity savedSchedule = schedulePlansRepository.save(schedule);
        return savedSchedule.getScheduleId();
    }

    // 세부 일정 순서 변경 (드래그 앤 드롭 대응)
    @Transactional
    public void updateSchedule(Long dailyId, List<SchedulePlansDTO> schedulePlansDTOList) {
        // 1. 이동할 대상 일차(DailyPlansEntity) 조회
        DailyPlansEntity targetDaily = dailyPlansRepository.findById(dailyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일차입니다."));

        for (SchedulePlansDTO dto : schedulePlansDTOList) {
            SchedulePlansEntity schedule = schedulePlansRepository.findById(dto.getScheduleId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세부 일정입니다."));

            // 2. 다른 일차에서 끌어온 경우, 소속 일차(dailyPlansEntity)를 새로운 일차로 변경
            if (!schedule.getDailyPlansEntity().getDailyId().equals(dailyId)) {
                schedule.setDailyPlansEntity(targetDaily);
            }

            // 3. 새로운 순서 번호(scheduleNumber) 반영
            if (dto.getScheduleNumber() != null) {
                schedule.setScheduleNumber(dto.getScheduleNumber());
            }
        }
    }

    /* 세부 일정 삭제 */
    @Transactional
    public void deleteSchedule(Long scheduleId, Long memberId) {
        SchedulePlansEntity schedule = schedulePlansRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세부 일정입니다."));
        if (!schedule.getDailyPlansEntity().getTravelPlan().getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("존재하지 않는 세부 일정입니다.");
        }
        schedulePlansRepository.delete(schedule);
    }


    /* 세부 일정 시간만 바꾸기 (지도 '여행 계획 짜기'의 시계 버튼). 빈 값이면 시간을 지운다 */
    @Transactional
    public void updateScheduleTime(Long scheduleId, Long memberId, String startTime, String endTime) {
        SchedulePlansEntity schedule = schedulePlansRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세부 일정입니다."));
        if (!schedule.getDailyPlansEntity().getTravelPlan().getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("존재하지 않는 세부 일정입니다.");
        }
        String start = timeOrNull(startTime);
        String end = timeOrNull(endTime);
        if (start != null && end != null && end.compareTo(start) < 0) {
            throw new IllegalArgumentException("종료 시간이 시작 시간보다 빨라요.");
        }
        schedule.setScheduleStartTime(start);
        schedule.setScheduleEndTime(end);
    }

    // "10:00" 모양만 받는다 (AI 가이드가 운영시간과 비교하므로 모양을 맞춘다)
    private String timeOrNull(String time) {
        if (time == null || time.isBlank()) return null;
        String t = time.trim();
        if (!t.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            throw new IllegalArgumentException("시간은 10:00 처럼 골라 주세요.");
        }
        return t;
    }

    // 세부일정 전체저장
    @Transactional
    public void updateAllSchedule(Long planId, Long memberId, List<SchedulePlansDTO> newSchedule) {
        TravelPlansEntity travelPlans = travelPlansRepository.findByPlanIdAndMemberId(planId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("여행계획을 찾을 수 없습니다."));

        //여행 계획에 속한 일자 추출
        List<Long> dailyiIds = travelPlans.getDailyPlans().stream()
                .map(DailyPlansEntity::getDailyId)
                .collect(Collectors.toList());

        // 기존 스케줄 추출
        List<SchedulePlansEntity> initSchedule = schedulePlansRepository.findByDailyIdIn(dailyiIds);

        //빠른 매핑을 위한 룩업 맵 생성
        Map<Long, SchedulePlansEntity> initMap = initSchedule.stream()
                .collect(Collectors.toMap(SchedulePlansEntity :: getScheduleId,
                        s -> s));
        Map<Long, DailyPlansEntity> dailyMap = travelPlans.getDailyPlans().stream()
                .collect(Collectors.toMap(DailyPlansEntity::getDailyId, d -> d));

        // 새로운 스케줄
        Set<Long> newIds = newSchedule.stream()
                .map(SchedulePlansDTO::getScheduleId)
                .collect(Collectors.toSet());

        // 새로운 스케줄에 없는 스케줄 DB에서 제거
        List<SchedulePlansEntity> toDelete = initSchedule.stream()
                .filter(schedule -> !newIds.contains(schedule.getScheduleId()))
                .collect(Collectors.toList());

        if(!toDelete.isEmpty()) {
            schedulePlansRepository.deleteAll(toDelete);
        }

        // 순서변경
        for (SchedulePlansDTO dto : newSchedule) {
            SchedulePlansEntity schedule = initMap.get(dto.getScheduleId());
            if (schedule != null) {
                schedule.setScheduleNumber(dto.getScheduleNumber());
                //변경되었다면 일차변경
               DailyPlansEntity newDaily = dailyMap.get(dto.getDailyId());
               if (newDaily != null) {
                   schedule.setDailyPlansEntity(newDaily);
               }
            }
        }

    }



        //문자열 유효성 검사
        private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }



}
