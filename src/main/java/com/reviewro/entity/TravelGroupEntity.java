package com.reviewro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

// 회원끼리 만드는 여행 그룹. 여행계획을 공유하고 그룹 채팅을 한다
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "travel_group")
public class TravelGroupEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false, length = 50)
    private String groupName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private MemberEntity owner;           // 그룹을 만든 회원 (그룹 삭제 권한)

    // 이 코드를 아는 회원은 초대 수락 없이 바로 들어온다. 방장이 만들거나 새로 바꾼다 (없으면 코드 참여 불가)
    @Column(unique = true, length = 12)
    private String inviteCode;

    // 초대코드가 끝나는 시각. 지나면 그 코드로는 못 들어온다 (방장이 새로 만들면 다시 7일)
    private LocalDateTime inviteCodeExpiresAt;
}
