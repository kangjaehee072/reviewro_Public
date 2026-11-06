package com.reviewro.security;

import com.reviewro.entity.MemberEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// 아이디·비밀번호 로그인(UserDetails)과 소셜 로그인(OAuth2User) 둘 다 이 객체를 principal 로 쓴다.
// 그래서 컨트롤러는 어느 쪽으로 로그인했든 @AuthenticationPrincipal MemberDetails 로 받는다
@Getter
public class MemberDetails implements UserDetails, OAuth2User {

    private final MemberEntity member;

    // 소셜 제공자가 준 원래 정보 (아이디·비밀번호 로그인이면 비어 있다)
    private final Map<String, Object> attributes;

    public MemberDetails(MemberEntity member) {
        this(member, Map.of());
    }

    public MemberDetails(MemberEntity member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getMemberRole().name()));
    }

    @Override
    public String getPassword() {
        return member.getMemberPassword();
    }

    @Override
    public String getUsername() {
        return member.getMemberUserId();
    }

    // OAuth2User 의 이름. 세션 기록(SessionRegistry)도 아이디로 맞춘다
    @Override
    public String getName() {
        return member.getMemberUserId();
    }
}
