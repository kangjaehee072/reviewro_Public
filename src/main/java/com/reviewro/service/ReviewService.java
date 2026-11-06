package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.ReviewDTO;
import com.reviewro.entity.*;
import com.reviewro.repository.PostImageRepository;
import com.reviewro.repository.ReviewRepository;
import com.reviewro.repository.MemberRepository;
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
public class ReviewService {

    private static final int TITLE_MAX = 100;
    private static final int CONTENT_MAX = 5000;

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final PostImageRepository postImageRepository;

    private static final int PAGE_SIZE = 10;

    // 페이징 + 썸네일 목록 조회
    public Page<ReviewDTO> list(String keyword, Integer page) {
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "reviewId"));

        // 리뷰 엔티티를 페이징과 함께 조회 (리포지토리에 페이징용 쿼리가 있다고 가정)
        Page<ReviewEntity> found;
        if (keyword == null || keyword.isBlank()) {
            found = reviewRepository.findPageWithMember(request);
        }else{
            found = reviewRepository.findByReviewTitleContaining(keyword, request);
        }

        List<ReviewDTO> rows = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (ReviewEntity e : found.getContent()) {
            ReviewDTO dto = AutoMapper.map(e, ReviewDTO.class);
            if (e.getMember() != null) {
                dto.setReviewMemberId(e.getMember().getMemberId());
            }
            rows.add(dto);
            ids.add(e.getReviewId());
        }

        // 글이 한 건도 없으면 사진을 찾을 필요 없음
        if (ids.isEmpty()) {
            return new PageImpl<>(rows, request, found.getTotalElements());
        }

        // 사진 한 번에 가져오기
        List<PostImageEntity> images =
                postImageRepository.findForPosts(TargetType.REVIEW, ids);

        // 첫 장을 썸네일로 매핑
        for (ReviewDTO dto : rows) {
            for (PostImageEntity image : images) {
                if (image.getTargetId().equals(dto.getReviewId())) {
                    dto.setThumbnail(image.getImagePath());
                    break;
                }
            }
        }

        return new PageImpl<>(rows, request, found.getTotalElements());
    }

    // 상세
    public ReviewDTO read(Long id) {
        ReviewEntity entity = getEntity(id);
        ReviewDTO dto = AutoMapper.map(entity, ReviewDTO.class);
        if (entity.getMember() != null) {
            dto.setReviewMemberId(entity.getMember().getMemberId());
        }
        return dto;
    }

    // 등록
    @Transactional
    public Long insert(ReviewDTO dto, Long memberId) {
        validate(dto);
        MemberEntity member = getMember(memberId);

        ReviewEntity entity = AutoMapper.map(dto, ReviewEntity.class);

        // 제목 및 내용 trim 처리
        if (entity.getReviewTitle() != null) {
            entity.setReviewTitle(entity.getReviewTitle().trim());
        }
        if (entity.getReviewContent() != null) {
            entity.setReviewContent(entity.getReviewContent().trim());
        }

        // 작성자와 작성일은 서버가 정한다 (화면 값 무시)
        entity.setReviewUser(member.getMemberNickname());
        entity.setReviewDate(LocalDate.now());
        entity.setMember(member);

        Long reviewId = reviewRepository.save(entity).getReviewId();


        return reviewRepository.save(entity).getReviewId();
    }

    // 수정
    @Transactional
    public void update(Long memberId, ReviewDTO dto) {
        validate(dto);
        ReviewEntity entity = getOwned(dto.getReviewId(), memberId);

        // 수정 가능한 항목 반영 및 trim 처리
        if (dto.getReviewTitle() != null) {
            entity.setReviewTitle(dto.getReviewTitle().trim());
        }
        if (dto.getReviewContent() != null) {
            entity.setReviewContent(dto.getReviewContent().trim());
        }

        // 필요한 다른 수정 가능 필드들도 여기서 세팅합니다.
        entity.setReviewArea(dto.getReviewArea());
        entity.setReviewLocation(dto.getReviewLocation());
        entity.setReviewCategory(dto.getReviewCategory());
        entity.setReviewStartDate(dto.getReviewStartDate());
        entity.setReviewEndDate(dto.getReviewEndDate());
        entity.setReviewPeopleCount(dto.getReviewPeopleCount());
        entity.setReviewVisitPurpose(dto.getReviewVisitPurpose());
        entity.setReviewCompanionType(dto.getReviewCompanionType());
        entity.setReviewStarRating(dto.getReviewStarRating());
    }

    // 삭제
    @Transactional
    public void delete(Long id, Long memberId) {
        reviewRepository.delete(getOwned(id, memberId));
    }

    // ===== 공통 검증 및 조회 메서드 =====

    private void validate(ReviewDTO dto) {
        String title = dto.getReviewTitle();
        String content = dto.getReviewContent();

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
        if (memberId == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private ReviewEntity getEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("리뷰 번호가 없습니다.");
        }
        return reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
    }

    // 글 존재 여부 확인 + 로그인한 사람이 작성자인지 권한 검증
    private ReviewEntity getOwned(Long id, Long memberId) {
        ReviewEntity entity = getEntity(id);
        if (entity.getMember() == null
                || !entity.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인이 쓴 글만 수정하거나 삭제할 수 있습니다.");
        }
        return entity;
    }

    // 메인 '베스트 평점': 최근 1달 별점 Top5 중 랜덤 1건. 1달 안에 없으면 날짜 상관없이 최신 글 중 랜덤
    public ReviewEntity getTopBestReview() {
        List<ReviewEntity> top = reviewRepository.findBestSince(
                LocalDateTime.now().minusMonths(1), PageRequest.of(0, 5));
        if (top.isEmpty()) {
            top = reviewRepository.findRecent(PageRequest.of(0, 5));
        }
        return top.isEmpty() ? null : top.get(ThreadLocalRandom.current().nextInt(top.size()));
    }
}
