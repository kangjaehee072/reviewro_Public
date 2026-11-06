package com.reviewro.dto;

import com.reviewro.entity.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 회원관리 목록 한 줄. 비밀번호는 절대 담지 않는다
@Getter
@AllArgsConstructor
public class AdminMemberDTO {

    private Long memberId;
    private String memberUserId;
    private String memberNickname;
    private String memberEmail;
    private String memberPhone;
    private String memberProfileImagePath;
    private String memberRole;      // USER / ADMIN
    private LocalDateTime createdAt;

    public static AdminMemberDTO from(MemberEntity m) {
        return new AdminMemberDTO(
                m.getMemberId(),
                m.getMemberUserId(),
                m.getMemberNickname(),
                m.getMemberEmail(),
                m.getMemberPhone(),
                m.getMemberProfileImagePath(),
                m.getMemberRole().name(),
                m.getCreatedAt());
    }
}
