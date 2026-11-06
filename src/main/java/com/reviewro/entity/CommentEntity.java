package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "comment",
        indexes = @Index(name = "idx_comment_target",
                columnList = "target_type, target_id, comment_id"))
public class CommentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    // 어느 게시판의
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false,
            columnDefinition = "varchar(20)")
    private TargetType targetType;

    // 몇 번 글인지
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")      // 탈퇴한 회원의 댓글은 비어 있다 (댓글은 남긴다)
    private MemberEntity member;

    @Column(length = 1000, nullable = false)
    private String content;

    // 대댓글이면 원댓글, 원댓글이면 null
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "parent_id")
    private CommentEntity parent;

    @Column(nullable = false)
    private Boolean deleted = false;
}
