package com.reviewro.dto;

import com.reviewro.entity.TravelGroupMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelGroupMemberDTO {
    private Long memberId;
    private String nickname;
    private String profileImagePath;
    private TravelGroupMemberStatus status;
    private boolean owner;
    private String maskedUserId;      // 초대 검색 결과에서만. 아이디는 로그인에 쓰여서 앞 3글자만 보여준다
}
