package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.CompanionPostDTO;
import com.reviewro.entity.CompanionPostEntity;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.PostImageEntity;
import com.reviewro.entity.TargetType;
import com.reviewro.repository.CompanionPostRepository;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.PostImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CompanionService {

    private final CompanionPostRepository companionPostRepository;
    private final PostImageRepository postImageRepository;
    private final MemberRepository memberRepository;

    private static final int PAGE_SIZE = 10;

    // ==================== [게시글 관련 로직] ====================

    // 동행 모집글 등록
    public Long insertPost(Long memberId, CompanionPostDTO postDto) {
        if (postDto.getCompanionPostTitle() == null || postDto.getCompanionPostTitle().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }
        if (postDto.getCompanionPostStartDate() != null && postDto.getCompanionPostEndDate() != null
                && postDto.getCompanionPostEndDate().isBefore(postDto.getCompanionPostStartDate())) {
            throw new IllegalArgumentException("여행 종료일이 시작일보다 빠릅니다.");
        }

        CompanionPostEntity postEntity = AutoMapper.map(postDto, CompanionPostEntity.class);

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        postEntity.setMember(member);
        // 작성자 이름은 폼 값이 아니라 로그인한 회원 닉네임으로 정한다
        postEntity.setCompanionPostMember(member.getMemberNickname());

        return companionPostRepository.save(postEntity).getCompanionPostId();
    }

    // 동행 모집글 페이징 및 검색/썸네일 목록 조회 (프리보드/리뷰 서비스와 동일한 구조)
    public Page<CompanionPostDTO> list(String keyword, Integer page) {
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "companionPostId"));

        // 검색어 유무에 따른 레포지토리 분기 호출
        Page<CompanionPostEntity> found;
        if (keyword == null || keyword.isBlank()) {
            found = companionPostRepository.findPageWithMember(request);
        } else {
            found = companionPostRepository.findByCompanionPostTitleContaining(keyword, request);
        }

        List<CompanionPostDTO> rows = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (CompanionPostEntity e : found.getContent()) {
            CompanionPostDTO dto = AutoMapper.map(e, CompanionPostDTO.class);
            if (e.getMember() != null) {
                dto.setCompanionPostMemberId(e.getMember().getMemberId());
            }
            rows.add(dto);
            ids.add(e.getCompanionPostId());
        }

        // 글이 한 건도 없으면 사진 조회 생략
        if (ids.isEmpty()) {
            return new PageImpl<>(rows, request, found.getTotalElements());
        }

        // 첨부 이미지 일괄 조회 후 썸네일 매핑
        List<PostImageEntity> images =
                postImageRepository.findForPosts(TargetType.COMPANION, ids);

        for (CompanionPostDTO dto : rows) {
            for (PostImageEntity image : images) {
                if (image.getTargetId().equals(dto.getCompanionPostId())) {
                    dto.setThumbnail(image.getImagePath());
                    break;
                }
            }
        }

        return new PageImpl<>(rows, request, found.getTotalElements());
    }

    // 동행 모집글 상세 조회
    public CompanionPostDTO readPost(Long id) {
        CompanionPostEntity entity = companionPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("동행 모집글을 찾을 수 없습니다."));

        CompanionPostDTO dto = AutoMapper.map(entity, CompanionPostDTO.class);
        if (entity.getMember() != null) {
            dto.setCompanionPostMemberId(entity.getMember().getMemberId());
        }
        return dto;
    }

    // 동행 모집글 수정
    @Transactional
    public void updatePost(Long memberId, CompanionPostDTO postDto) {
        if (postDto.getCompanionPostTitle() == null || postDto.getCompanionPostTitle().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }
        if (postDto.getCompanionPostStartDate() != null && postDto.getCompanionPostEndDate() != null
                && postDto.getCompanionPostEndDate().isBefore(postDto.getCompanionPostStartDate())) {
            throw new IllegalArgumentException("여행 종료일이 시작일보다 빠릅니다.");
        }

        // 본인이 쓴 글인지 검증하는 로직 추가
        CompanionPostEntity entity = getOwned(postDto.getCompanionPostId(), memberId);

        // 작성자 이름은 수정 폼에서 바꾸지 못한다
        postDto.setCompanionPostMember(null);
        AutoMapper.copyNonNull(postDto, entity);
    }

    // 동행 모집글 삭제
    @Transactional
    public void deletePost(Long id, Long memberId) {
        CompanionPostEntity entity = getOwned(id, memberId);
        companionPostRepository.delete(entity);
    }

    // [공통] 본인 소유 확인 메서드 추가
    private CompanionPostEntity getOwned(Long id, Long memberId) {
        CompanionPostEntity entity = companionPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("동행 모집글을 찾을 수 없습니다."));
        if (entity.getMember() == null || !entity.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인이 쓴 글만 수정하거나 삭제할 수 있습니다.");
        }
        return entity;
    }


    //메인용 게시글조회 (가장빠른 출발일 순)
    // 메인 '동행 모집': 여행 시작일이 아직 안 지난 모집 중 랜덤 1건 (여러 개면 매번 다른 글이 뜨게)
    public CompanionPostEntity getTopUpcomingCompanion() {
        List<CompanionPostEntity> upcoming = companionPostRepository.findTopByUpcoming(PageRequest.of(0, 20));
        return upcoming.isEmpty() ? null : upcoming.get(ThreadLocalRandom.current().nextInt(upcoming.size()));
    }



}