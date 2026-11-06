package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_message")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long chatMessageId;           // 메시지 PK

    // 어떤 채팅방의 메시지인지 연결 (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoomEntity chatRoom;      // 채팅방 FK

    @Column(nullable = false, length = 50)
    private String chatRoomMember;        // 메시지보낸회원 (아이디 또는 닉네임)

    // 내 말풍선 구분용. 닉네임은 바뀔 수 있어 회원으로 잇는다 (예전 메시지는 비어 있다)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String chatRoomContent;       // 채팅내용

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime chatRoomSentAt = LocalDateTime.now(); // 보낸시간
}