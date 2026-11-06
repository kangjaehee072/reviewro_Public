package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.FreeBoardDTO;
import com.reviewro.entity.FreeBoardEntity;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.PostImageEntity;
import com.reviewro.entity.TargetType;
import com.reviewro.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreeBoardService {

    private static final int TITLE_MAX = 100;
    private static final int CONTENT_MAX = 5000;

    private final FreeBoardRepository freeBoardRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final PostImageRepository postImageRepository;

    private static final int PAGE_SIZE = 10;

    // 페이징 + 썸네일 목록 조회 메서드로 변경
    public Page<FreeBoardDTO> list(String keyword, Integer page) {
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "freeBoardId"));

        // 페이징 및 작성자 JOIN FETCH 처리를 위한 리포지토리 호출
        Page<FreeBoardEntity> found;
        if (keyword == null || keyword.isBlank()) {
            found = freeBoardRepository.findPageWithMember(request);
        } else {
            found = freeBoardRepository.findByfreeBoardTitleContaining(keyword, request);
        }

        List<FreeBoardDTO> rows = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (FreeBoardEntity e : found.getContent()) {
            FreeBoardDTO dto = AutoMapper.map(e, FreeBoardDTO.class);
            if (e.getMember() != null) {
                dto.setFreeBoardMemberId(e.getMember().getMemberId());
            }
            rows.add(dto);
            ids.add(e.getFreeBoardId());
        }

        // 글이 한 건도 없으면 사진을 찾을 것도 없다.
        // 빈 목록으로 IN () 을 날리면 SQL 문법 오류가 난다
        if (ids.isEmpty()) {
            return new PageImpl<>(rows, request, found.getTotalElements());
        }

        // 이 쪽에 보이는 글들의 사진을 한 번에 가져온다 (2-4 의 findForPosts)
        List<PostImageEntity> images =
                postImageRepository.findForPosts(TargetType.FREE_BOARD, ids);

        // 글마다 자기 사진을 찾아 첫 장을 썸네일로 붙인다.
        // 번호순으로 가져왔으니 처음 만난 것이 첫 사진이다
        for (FreeBoardDTO dto : rows) {
            for (PostImageEntity image : images) {
                if (image.getTargetId().equals(dto.getFreeBoardId())) {
                    dto.setThumbnail(image.getImagePath());
                    break;              // 찾았으면 더 볼 필요 없다
                }
            }
        }

        return new PageImpl<>(rows, request, found.getTotalElements());
    }

    // 상세
    public FreeBoardDTO read(Long id) {
        FreeBoardEntity entity = getEntity(id);
        FreeBoardDTO dto = AutoMapper.map(entity, FreeBoardDTO.class);
        if (entity.getMember() != null) {
            dto.setFreeBoardMemberId(entity.getMember().getMemberId());
        }
        return dto;
    }

    @Transactional
    public Long insert(Long memberId, FreeBoardDTO dto) {
        validate(dto);
        MemberEntity member = getMember(memberId);

        FreeBoardEntity entity = new FreeBoardEntity();
        entity.setFreeBoardTitle(dto.getFreeBoardTitle().trim());
        entity.setFreeBoardContent(dto.getFreeBoardContent().trim());
        entity.setFreeBoardName(member.getMemberNickname());   // 작성자는 서버가 정한다
        entity.setFreeBoardDate(LocalDate.now());              // 작성일도 서버가 정한다
        entity.setMember(member);
        // freeBoardShareCount 는 엔티티 기본값 0 이 들어간다

        return freeBoardRepository.save(entity).getFreeBoardId();
    }

    @Transactional
    public void update(Long memberId, FreeBoardDTO dto) {
        validate(dto);
        FreeBoardEntity entity = getOwned(dto.getFreeBoardId(), memberId);

        // 바꿀 수 있는 건 제목과 내용뿐이다.
        // copyNonNull 을 쓰면 폼이 보낸 작성자/공유수까지 덮어쓸 수 있어 명시적으로 넣는다
        entity.setFreeBoardTitle(dto.getFreeBoardTitle().trim());
        entity.setFreeBoardContent(dto.getFreeBoardContent().trim());
    }

    @Transactional
    public void delete(Long id, Long memberId) {

        freeBoardRepository.delete(getOwned(id, memberId));

        commentRepository.deleteByTarget(TargetType.FREE_BOARD, id);
        reactionRepository.deleteByTarget(TargetType.FREE_BOARD, id);
    }

    // ===== 공통 =====

    private void validate(FreeBoardDTO dto) {
        String title = dto.getFreeBoardTitle();
        String content = dto.getFreeBoardContent();

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

    private FreeBoardEntity getEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("게시글 번호가 없습니다.");
        }
        return freeBoardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("자유게시글을 찾을 수 없습니다."));
    }

    // 글을 찾고, 로그인한 사람이 작성자인지까지 확인한다
    private FreeBoardEntity getOwned(Long id, Long memberId) {
        FreeBoardEntity entity = getEntity(id);
        if (entity.getMember() == null
                || !entity.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인이 쓴 글만 수정하거나 삭제할 수 있습니다.");
        }
        return entity;
    }

    //공유수 많은 글 찾기
    // 메인 '자유 핫토픽': 최근 1달 공유 Top5 중 랜덤 1건. 1달 안에 없으면 날짜 상관없이 최신 글 중 랜덤
    public FreeBoardEntity getTopViewedReview(){
        List<FreeBoardEntity> top = freeBoardRepository.findHotSince(
                LocalDateTime.now().minusMonths(1), PageRequest.of(0, 5));
        if (top.isEmpty()) {
            top = freeBoardRepository.findRecent(PageRequest.of(0, 5));
        }
        return top.isEmpty() ? null : top.get(ThreadLocalRandom.current().nextInt(top.size()));
    }

}
