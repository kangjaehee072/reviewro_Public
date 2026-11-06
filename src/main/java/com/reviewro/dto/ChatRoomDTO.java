package com.reviewro.dto;

import com.reviewro.entity.ChatRegion;
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
public class ChatRoomDTO {
    private Long chatRoomId;         // 채팅방 PK
    private ChatRegion chatRoomRegion; // 지역
    private String chatRoomName;     // 채팅방 이름
    private String creatorNickname;  // 방을 만든 회원 닉네임
    private int onlineCount;         // 지금 접속해 있는 인원
    private LocalDateTime createdAt; // 만든 시간
}
