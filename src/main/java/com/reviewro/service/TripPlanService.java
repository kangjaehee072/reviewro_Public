package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.FreeBoardDTO;
import com.reviewro.dto.TripDayDTO;
import com.reviewro.dto.TripPlaceDTO;
import com.reviewro.dto.TripPlanDTO;
import com.reviewro.entity.*;
import com.reviewro.repository.LocationRepository;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.TravelPlansRepository;
import com.reviewro.repository.TripDayRepository;
import com.reviewro.repository.TripPlaceRepository;
import com.reviewro.repository.TripPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripPlanService {

    private static final int TITLE_MAX = 100;
    private static final int CONTENT_MAX = 5000;
    private static final int PAGE_SIZE = 12;

    private final TripPlanRepository tripPlanRepository;
    private final TripDayRepository tripDayRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final MemberRepository memberRepository;
    private final TravelPlansRepository travelPlansRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public Long insert(Long memberId, TripPlanDTO tripPlanDto) {
        if (tripPlanDto.getTripplanTitle() == null || tripPlanDto.getTripplanTitle().isBlank()) {
            throw new IllegalArgumentException("여행 계획 제목을 입력해 주세요.");
        }
        if (tripPlanDto.getTripplanContent() == null || tripPlanDto.getTripplanContent().isBlank()) {
            throw new IllegalArgumentException("여행 계획 내용을 입력해 주세요.");
        }

        // 1. TripPlan 엔티티 생성 및 기본값 설정 (퍼가기 수는 클라이언트가 정하지 못한다)
        TripPlanEntity tripPlanEntity = new TripPlanEntity();
        tripPlanEntity.setTripplanTitle(tripPlanDto.getTripplanTitle());
        tripPlanEntity.setTripplanContent(tripPlanDto.getTripplanContent());
        tripPlanEntity.setTripplanShareCount(0);
        tripPlanEntity.setTripplanIsPublic(tripPlanDto.getTripplanIsPublic() != null ? tripPlanDto.getTripplanIsPublic() : true);
        tripPlanEntity.setMember(memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다.")));

        // 2. 하위 Day 및 Place 리스트 수동 매핑 및 연호관관계 편의 설정외
        if (tripPlanDto.getTripDays() != null) {
            for (TripDayDTO dayDto : tripPlanDto.getTripDays()) {
                TripDayEntity dayEntity = new TripDayEntity();
                dayEntity.setTripDayNumber(dayDto.getTripDayNumber());
                dayEntity.setTripPlan(tripPlanEntity); // 연관관계 설정

                if (dayDto.getTripPlaces() != null) {
                    for (TripPlaceDTO placeDto : dayDto.getTripPlaces()) {
                        TripPlaceEntity placeEntity = new TripPlaceEntity();
                        placeEntity.setLocationId(placeDto.getLocationId());
                        placeEntity.setTripPlaceSequence(placeDto.getTripPlaceSequence());
                        placeEntity.setTripPlaceMemo(placeDto.getTripPlaceMemo());
                        placeEntity.setTripDay(dayEntity); // 연관관계 설정

                        dayEntity.getTripPlaces().add(placeEntity);
                    }
                }
                tripPlanEntity.getTripDays().add(dayEntity);
            }
        }

        // 3. 저장 (Cascade.ALL에 의해 Day와 Place도 함께 영속화됨)
        return tripPlanRepository.save(tripPlanEntity).getTripplanId();
    }

    public Page<TripPlanDTO> list(Integer page, Long viewerId) {
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "tripplanId"));
        Page<TripPlanEntity> found = tripPlanRepository.findVisible(viewerId, request);
        return new PageImpl<>(AutoMapper.mapList(found.getContent(), TripPlanDTO.class),
                request, found.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TripPlanDTO read(Long id, Long viewerId) {
        TripPlanEntity entity = tripPlanRepository.findByIdWithMember(id)
                .filter(found -> isVisible(found, viewerId))
                .orElseThrow(() -> new IllegalArgumentException("여행 계획을 찾을 수 없습니다."));

        // Entity -> DTO 수동 매핑 (중첩 리스트 누락 방지)
        TripPlanDTO dto = new TripPlanDTO();
        dto.setTripplanId(entity.getTripplanId());
        dto.setTripplanTitle(entity.getTripplanTitle());
        dto.setTripplanContent(entity.getTripplanContent());
        dto.setTripplanShareCount(entity.getTripplanShareCount());
        dto.setTripplanIsPublic(entity.getTripplanIsPublic());

        if (entity.getMember() != null) {
            dto.setMemberId(entity.getMember().getMemberId());
        }

        List<TripDayDTO> dayDtos = new ArrayList<>();
        if (entity.getTripDays() != null) {
            for (TripDayEntity dayEntity : entity.getTripDays()) {
                TripDayDTO dayDto = new TripDayDTO();
                dayDto.setTripDayId(dayEntity.getTripDayId());
                dayDto.setTripDayNumber(dayEntity.getTripDayNumber());

                List<TripPlaceDTO> placeDtos = new ArrayList<>();
                if (dayEntity.getTripPlaces() != null) {
                    for (TripPlaceEntity placeEntity : dayEntity.getTripPlaces()) {
                        TripPlaceDTO placeDto = new TripPlaceDTO();
                        placeDto.setTripPlaceId(placeEntity.getTripPlaceId());
                        placeDto.setLocationId(placeEntity.getLocationId());
                        placeDto.setTripPlaceSequence(placeEntity.getTripPlaceSequence());
                        placeDto.setTripPlaceMemo(placeEntity.getTripPlaceMemo());
                        placeDtos.add(placeDto);
                    }
                }
                dayDto.setTripPlaces(placeDtos);
                dayDtos.add(dayDto);
            }
        }
        dto.setTripDays(dayDtos);

        return dto;
    }

    @Transactional
    public void update(Long memberId, TripPlanDTO tripPlanDto) {
        if (tripPlanDto.getTripplanTitle() == null || tripPlanDto.getTripplanTitle().isBlank()) {
            throw new IllegalArgumentException("여행 계획 제목을 입력해 주세요.");
        }

        // 1. 기존 엔티티 조회 (작성자 본인만)
        TripPlanEntity entity = getOwned(tripPlanDto.getTripplanId(), memberId);

        // 2. 기본 정보 수정 (작성자와 퍼가기 수는 그대로 둔다)
        entity.setTripplanTitle(tripPlanDto.getTripplanTitle());
        entity.setTripplanContent(tripPlanDto.getTripplanContent());
        entity.setTripplanIsPublic(tripPlanDto.getTripplanIsPublic() != null ? tripPlanDto.getTripplanIsPublic() : true);

        // 3. 하위 일정(TripDays, TripPlaces) 동기화 (orphanRemoval = true 활용)
        entity.getTripDays().clear(); // 기존 일정 비우기 (고아 객체 자동 삭제)

        if (tripPlanDto.getTripDays() != null) {
            for (TripDayDTO dayDto : tripPlanDto.getTripDays()) {
                TripDayEntity dayEntity = new TripDayEntity();
                dayEntity.setTripDayNumber(dayDto.getTripDayNumber());
                dayEntity.setTripPlan(entity);

                if (dayDto.getTripPlaces() != null) {
                    for (TripPlaceDTO placeDto : dayDto.getTripPlaces()) {
                        TripPlaceEntity placeEntity = new TripPlaceEntity();
                        placeEntity.setLocationId(placeDto.getLocationId());
                        placeEntity.setTripPlaceSequence(placeDto.getTripPlaceSequence());
                        placeEntity.setTripPlaceMemo(placeDto.getTripPlaceMemo());
                        placeEntity.setTripDay(dayEntity);

                        dayEntity.getTripPlaces().add(placeEntity);
                    }
                }
                entity.getTripDays().add(dayEntity);
            }
        }
    }

    @Transactional
    public void delete(Long id, Long memberId) {
        tripPlanRepository.delete(getOwned(id, memberId));
    }

    // 마켓 일정을 내 여행계획(travel_plans)으로 퍼가기. 날짜는 오늘부터 일차 수만큼
    @Transactional
    public Long fork(Long originalTripPlanId, Long memberId, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("저장할 제목을 입력해 주세요.");
        }
        if (title.trim().length() > TITLE_MAX) {
            throw new IllegalArgumentException("제목은 " + TITLE_MAX + "자 이내로 입력해 주세요.");
        }

        TripPlanEntity originalEntity = tripPlanRepository.findById(originalTripPlanId)
                .filter(found -> isVisible(found, memberId))
                .orElseThrow(() -> new IllegalArgumentException("복사할 원본 여행 계획을 찾을 수 없습니다."));

        List<TripDayEntity> days = originalEntity.getTripDays();
        int dayCount = Math.max(days.size(), 1);
        LocalDate startDate = LocalDate.now();

        TravelPlansEntity travelPlan = new TravelPlansEntity();
        travelPlan.setMember(getMember(memberId));
        travelPlan.setPlanTitle(title.trim());
        travelPlan.setPlanDescription(originalEntity.getTripplanContent());
        travelPlan.setPlanStartDate(startDate.toString());
        travelPlan.setPlanEndDate(startDate.plusDays(dayCount - 1).toString());

        for (int i = 0; i < dayCount; i++) {
            DailyPlansEntity daily = DailyPlansEntity.builder()
                    .travelPlan(travelPlan)
                    .dailyNumber(i + 1)
                    .dailyDate(startDate.plusDays(i))
                    .schedulePlans(new ArrayList<>())
                    .build();

            if (i < days.size()) {
                int order = 1;
                for (TripPlaceEntity place : days.get(i).getTripPlaces()) {
                    // 장소가 지워졌거나 번호가 없으면 건너뛴다
                    LocationEntity location = place.getLocationId() == null ? null
                            : locationRepository.findById(place.getLocationId()).orElse(null);
                    if (location == null) {
                        continue;
                    }
                    daily.getSchedulePlans().add(SchedulePlansEntity.builder()
                            .dailyPlansEntity(daily)
                            .location(location)
                            .scheduleNumber(order++)
                            .scheduleDescription(place.getTripPlaceMemo())
                            .build());
                }
            }
            travelPlan.getDailyPlans().add(daily);
        }

        Integer shareCount = originalEntity.getTripplanShareCount();
        originalEntity.setTripplanShareCount((shareCount == null ? 0 : shareCount) + 1);

        return travelPlansRepository.save(travelPlan).getPlanId();
    }

    // ===== 공통 =====

    private void validate(TripPlanDTO dto) {
        String title = dto.getTripplanTitle();
        String content = dto.getTripplanContent();

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }
        if (title.trim().length() > TITLE_MAX) {
            throw new IllegalArgumentException("제목은 " + TITLE_MAX + "자 이내로 입력해 주세요.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해 주세요.");
        }
        if (content.trim().length() > CONTENT_MAX) {
            throw new IllegalArgumentException("내용은 " + CONTENT_MAX + "자 이내로 입력해 주세요.");
        }
    }

    private MemberEntity getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private TripPlanEntity getEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("게시글 번호가 없습니다.");
        }
        return tripPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("자유게시글을 찾을 수 없습니다."));
    }

    // 글을 찾고, 로그인한 사람이 작성자인지까지 확인한다
    private TripPlanEntity getOwned(Long id, Long memberId) {
        TripPlanEntity entity = getEntity(id);
        if (entity.getMember() == null
                || !entity.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인이 쓴 글만 수정하거나 삭제할 수 있습니다.");
        }
        return entity;
    }

    // 비공개 글은 작성자에게만 보인다
    private boolean isVisible(TripPlanEntity entity, Long viewerId) {
        if (!Boolean.FALSE.equals(entity.getTripplanIsPublic())) {
            return true;
        }
        return viewerId != null && entity.getMember() != null
                && entity.getMember().getMemberId().equals(viewerId);
    }

    //메인용 공유수많은 템플릿 빼가기
    // 메인 '추천 템플릿': 최근 1달 퍼감 Top5 중 랜덤 1건. 1달 안에 없으면 날짜 상관없이 최신 공개 템플릿 중 랜덤
    public TripPlanEntity getTopSharedTemplate() {
        List<TripPlanEntity> top = tripPlanRepository.findTopSharedSince(
                LocalDateTime.now().minusMonths(1), PageRequest.of(0, 5));
        if (top.isEmpty()) {
            top = tripPlanRepository.findRecentPublic(PageRequest.of(0, 5));
        }
        return top.isEmpty() ? null : top.get(ThreadLocalRandom.current().nextInt(top.size()));
    }
}