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
public class TravelGroupDTO {
    private Long groupId;
    private String groupName;
    private Long ownerId;
    private String ownerNickname;
    private long memberCount;        // 수락한 구성원 수
    private String invitedByNickname; // 받은 초대 목록에서만 쓴다
    private String inviteCode;       // 구성원에게만 보인다 (기간이 지났으면 비운다)
    private LocalDateTime inviteCodeExpiresAt;
    private LocalDateTime createdAt;
}
