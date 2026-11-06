package com.reviewro.dto;

import com.reviewro.entity.MemberRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MemberDTO {

    private Long memberid;
    private String memberUserId;
    private String memberPassword;
    private String memberNewPassword;
    private String memberNickname;
    private MultipartFile memberProfileImage;
    private String memberProfileImagePath;
    private String memberEmail;
    private String memberPhone;
    private String memberAddress;
    private MemberRole memberRole;
    private LocalDateTime createdAt;

}
