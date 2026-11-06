package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 그룹 채팅 메시지
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "travel_group_message")
public class TravelGroupMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")      // 탈퇴한 회원의 메시지는 비어 있다 (메시지는 남긴다)
    private MemberEntity member;

    @Column(nullable = false, length = 50)
    private String senderNickname;        // 보낸 시점의 닉네임

    @Column(nullable = false, length = 500)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
}
