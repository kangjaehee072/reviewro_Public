package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.AdminPostDTO;
import com.reviewro.entity.AdminPostEntity;
import com.reviewro.entity.PostImageEntity;
import com.reviewro.entity.TargetType;
import com.reviewro.repository.AdminPostRepository;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.PostImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class AdminPostService {

    private static final int TITLE_MAX = 100;
    private static final int CONTENT_MAX = 5000;

    private final AdminPostRepository adminPostRepository;
    private final PostImageRepository postImageRepository;
    private final MemberRepository memberRepository;

    private static final int PAGE_SIZE = 10;

    // 관리자 게시글 등록
    public Long insert(Long id, AdminPostDTO adminPostDto) {
        if (adminPostDto.getAdminPostTitle() == null || adminPostDto.getAdminPostTitle().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }

        // 기본 조회수 및 상단고정 여부 초기화 설정
        if (adminPostDto.getAdminPostViewCount() == null) {
            adminPostDto.setAdminPostViewCount(0L);
        }
        if (adminPostDto.getAdminPostPinned() == null) {
            adminPostDto.setAdminPostPinned(false);
        }
        // 작성 폼에 구분 선택이 없다. 목록(findNotices)은 "공지"만 보여주므로 비어 있으면 공지로 둔다
        if (adminPostDto.getAdminPostCategory() == null || adminPostDto.getAdminPostCategory().isBlank()) {
            adminPostDto.setAdminPostCategory("공지");
        }

        AdminPostEntity entity = AutoMapper.map(adminPostDto, AdminPostEntity.class);
        // 작성자: 로그인한 관리자
        entity.setMember(memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")));
        entity.setAdminPostMemberId(id);
        return adminPostRepository.save(entity).getAdminPostId();
    }

    // 관리자 게시글 목록 조회 (페이징 + 썸네일 매핑 적용)
    public Page<AdminPostDTO> notices(int page) {
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "adminPostId"));

        Page<AdminPostEntity> found = adminPostRepository.findNotices("공지", request);

        List<AdminPostDTO> rows = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (AdminPostEntity e : found.getContent()) {
            AdminPostDTO dto = AutoMapper.map(e, AdminPostDTO.class);
            rows.add(dto);
            ids.add(e.getAdminPostId());
        }

        // 글이 한 건도 없으면 사진을 찾을 것도 없다.
        // 빈 목록으로 IN () 을 날리면 SQL 문법 오류가 난다
        if (ids.isEmpty()) {
            return new PageImpl<>(rows, request, found.getTotalElements());
        }

        // 이 쪽에 보이는 글들의 사진을 한 번에 가져온다
        List<PostImageEntity> images =
                postImageRepository.findForPosts(TargetType.ADMIN_POST, ids);

        // 글마다 자기 사진을 찾아 첫 장을 썸네일로 붙인다.
        // 번호순으로 가져왔으니 처음 만난 것이 첫 사진이다
        for (AdminPostDTO dto : rows) {
            for (PostImageEntity image : images) {
                if (image.getTargetId().equals(dto.getAdminPostId())) {
                    dto.setThumbnail(image.getImagePath());
                    break;              // 찾았으면 더 볼 필요 없다
                }
            }
        }

        return new PageImpl<>(rows, request, found.getTotalElements());
    }

    // 관리자 게시글 상세 조회 (조회수 증가 로직 포함)
    @Transactional
    public AdminPostDTO read(Long id) {
        AdminPostEntity entity = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 조회수 증가 처리 (setter 활용)
        if (entity.getAdminPostViewCount() == null) {
            entity.setAdminPostViewCount(1L);
        } else {
            entity.setAdminPostViewCount(entity.getAdminPostViewCount() + 1);
        }

        return AutoMapper.map(entity, AdminPostDTO.class);
    }

    // 관리자 게시글 수정
    @Transactional
    public void update(Long id, AdminPostDTO adminPostDto) {
        if (adminPostDto.getAdminPostTitle() == null || adminPostDto.getAdminPostTitle().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }

        AdminPostEntity entity = adminPostRepository.findById(adminPostDto.getAdminPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 작성자와 조회수는 폼에서 바꾸지 못한다
        adminPostDto.setAdminPostMemberId(null);
        adminPostDto.setAdminPostViewCount(null);
        AutoMapper.copyNonNull(adminPostDto, entity);
    }

    // 관리자 게시글 삭제
    public void delete(Long id, Long adminPostId) {
        if (!adminPostRepository.existsById(id)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        adminPostRepository.deleteById(id);
    }

    @Transactional
    public void togglePin(Long id) {
        AdminPostEntity post = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("없는 글입니다."));
        post.setAdminPostPinned(!Boolean.TRUE.equals(post.getAdminPostPinned()));
    }

    //한건만 가져오기 (메인용)
    public AdminPostEntity getLatestNotice() {
        List<AdminPostEntity> notices = adminPostRepository.findNoticeList("공지", PageRequest.of(0, 1));
        return notices.isEmpty() ? null : notices.get(0);
    }

}