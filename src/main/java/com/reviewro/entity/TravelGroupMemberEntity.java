package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

// 그룹 구성원. 초대하면 INVITED 로 생기고, 수락하면 JOINED 가 된다 (거절하면 지운다)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "travel_group_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "member_id"}))
public class TravelGroupMemberEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private TravelGroupMemberStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private MemberEntity invitedBy;       // 초대한 회원 (그룹을 만든 사람은 비어 있다)
}
