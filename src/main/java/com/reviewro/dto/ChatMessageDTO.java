package com.reviewro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Long chatMessageId;      // 메시지 PK
    private Long chatRoomId;         // 소속된 채팅방 PK (참조용)
    private Long memberId;           // 보낸 회원 PK (내 메시지 구분용)
    private String chatRoomMember;   // 메시지 보낸 회원 닉네임
    private String chatRoomContent;  // 채팅 내용
    private LocalDateTime chatRoomSentAt; // 보낸 시간
}
