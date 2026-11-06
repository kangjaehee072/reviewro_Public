package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_provider", "member_provider_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MemberEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 30)
    private String memberUserId;

    @Column(nullable = false)
    private String memberPassword;

    @Column(nullable = false, unique = true, length = 30)
    private String memberNickname;

    private String memberProfileImagePath;

    @Column(nullable = false, unique = true, length = 100)
    private String memberEmail;

    @Column(length = 20)
    private String memberPhone;

    private String memberAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private MemberRole memberRole = MemberRole.USER;

    // 소셜 로그인 제공자와 제공자 쪽 회원 번호. 둘이 합쳐 한 사람을 가리킨다.
    // 소셜로 가입한 회원이거나, 아이디 회원이 소셜 계정을 연동했으면 채워진다
    @Enumerated(EnumType.STRING)
    @Column(name = "member_provider", length = 20, columnDefinition = "varchar(20)")
    private MemberProvider memberProvider;

    @Column(name = "member_provider_id", length = 100)
    private String memberProviderId;

    // 소셜로만 가입해서 비밀번호가 없는 회원이면 true. 아이디 회원이 연동한 경우는 false.
    // 이 칸이 생기기 전 소셜 가입 회원은 비어 있어서 제공자 유무로 판단한다
    @Column(name = "member_social_only")
    private Boolean memberSocialOnly;

    // 가입할 때 필수 약관에 동의한 시각 (이 칸이 생기기 전 가입자는 비어 있다)
    private LocalDateTime memberTermsAgreedAt;

    public boolean isSocial() {
        if (memberSocialOnly != null) {
            return memberSocialOnly;
        }
        return memberProvider != null && memberProvider != MemberProvider.LOCAL;
    }
}
