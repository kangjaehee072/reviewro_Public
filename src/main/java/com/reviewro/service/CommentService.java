package com.reviewro.service;

import com.reviewro.dto.CommentDTO;
import com.reviewro.entity.*;
import com.reviewro.repository.CommentRepository;
import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    // 목록. memberId 는 로그인 안 했으면 null
    public List<CommentDTO> list(TargetType type, Long targetId, Long memberId) {
        return commentRepository.findForTarget(type, targetId).stream()
                .map(c -> toDto(c, memberId))
                .toList();
    }

    // 작성. parentId 가 있으면 대댓글
    @Transactional
    public void write(TargetType type, Long targetId, Long memberId,
                      String content, Long parentId) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해 주세요.");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("1000자까지 쓸 수 있습니다.");
        }

        CommentEntity comment = new CommentEntity();
        comment.setTargetType(type);
        comment.setTargetId(targetId);
        comment.setMember(memberRepository.getReferenceById(memberId));
        comment.setContent(content.trim());

        if (parentId != null) {
            CommentEntity parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("원댓글이 없습니다."));
            // 대대댓글은 안 받는다. 답글의 답글은 원댓글에 달리게 한다
            comment.setParent(parent.getParent() != null ? parent.getParent() : parent);
        }

        commentRepository.save(comment);
    }

    // 삭제.
    // 답글이 달린 원댓글만 "삭제된 댓글입니다"로 남기고(답글이 고아가 되지 않게), 나머지는 실제로 지운다
    @Transactional
    public void delete(Long commentId, Long memberId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("없는 댓글입니다."));

        if (comment.getMember() == null || !comment.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인 댓글만 지울 수 있습니다.");
        }

        if (commentRepository.existsByParent_CommentId(commentId)) {
            comment.setDeleted(true);
            comment.setContent("");
            return;
        }

        CommentEntity parent = comment.getParent();
        commentRepository.delete(comment);
        commentRepository.flush();   // 아래에서 "남은 답글이 있나" 를 물어보기 전에 DELETE 를 먼저 반영

        // 이미 지워진 원댓글에 마지막 답글까지 사라졌으면 빈 자리로 남길 이유가 없다
        if (parent != null && parent.getDeleted()
                && !commentRepository.existsByParent_CommentId(parent.getCommentId())) {
            commentRepository.delete(parent);
        }
    }

    private CommentDTO toDto(CommentEntity c, Long memberId) {
        // 탈퇴한 회원의 댓글은 작성자가 비어 있다
        boolean mine = memberId != null && c.getMember() != null
                && c.getMember().getMemberId().equals(memberId);
        return new CommentDTO(
                c.getCommentId(),
                c.getParent() == null ? null : c.getParent().getCommentId(),
                c.getDeleted() ? "" : (c.getMember() == null ? "탈퇴회원" : c.getMember().getMemberNickname()),
                c.getDeleted() ? "삭제된 댓글입니다." : c.getContent(),
                c.getCreatedAt(),
                c.getDeleted(),
                mine && !c.getDeleted());
    }
}