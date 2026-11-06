package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "reaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reaction",
                columnNames = {"target_type", "target_id", "member_id"}))
public class ReactionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false,
            columnDefinition = "varchar(20)")
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false, columnDefinition = "varchar(10)")
    private ReactionKind kind;
}