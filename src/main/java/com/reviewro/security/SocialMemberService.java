package com.reviewro.security;

import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberProvider;
import com.reviewro.entity.MemberRole;
import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

// 소셜 로그인 (카카오·구글·네이버).
// 제공자 + 제공자 쪽 회원 번호로 회원을 찾고, 처음이면 약관 동의 화면을 거쳐 가입시킨다 (SocialSignupGate).
// 같은 이메일의 일반 회원이 있어도 자동으로 합치지 않는다 (남의 계정에 붙는 사고 방지)
@Service
@RequiredArgsConstructor
public class SocialMemberService extends DefaultOAuth2UserService {

    private static final int NICKNAME_MAX = 30;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountLinkService accountLinkService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(request);
        SocialProfile profile = SocialProfile.of(request.getClientRegistration().getRegistrationId(), user.getAttributes());
        if (profile.id() == null || profile.id().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), "회원 번호를 받지 못했습니다.");
        }

        // 아이디 회원이 '계정 연동' 중이면 새로 로그인·가입하지 않고 그 회원에 붙인다 (AccountLinkService).
        // 처음 온 사람은 아직 저장하지 않는다. 약관 동의 화면(/signup/social)에서 동의해야 completeSignup 으로 저장된다
        MemberEntity member = accountLinkService.onSocialLogin(profile.provider(), profile.id())
                .orElseGet(() -> memberRepository
                        .findByMemberProviderAndMemberProviderId(profile.provider(), profile.id())
                        .orElseGet(() -> signup(profile)));
        return new MemberDetails(member, user.getAttributes());
    }

    // 소셜로 처음 와서 아직 약관에 동의하지 않은(저장 전) 회원인지
    public static boolean isPending(MemberEntity member) {
        return member != null && member.getMemberId() == null;
    }

    // 약관 동의 후 가입 완료. 동의 화면에 머무는 동안 같은 닉네임·이메일이 생겼을 수 있어 다시 확인한다
    @Transactional
    public MemberEntity completeSignup(MemberEntity pending) {
        // 다른 탭에서 먼저 가입을 마쳤으면 그 회원으로
        var existing = memberRepository.findByMemberProviderAndMemberProviderId(
                pending.getMemberProvider(), pending.getMemberProviderId());
        if (existing.isPresent()) {
            return existing.get();
        }
        pending.setMemberNickname(uniqueNickname(pending.getMemberNickname()));
        if (memberRepository.existsByMemberEmail(pending.getMemberEmail())) {
            pending.setMemberEmail(fallbackEmail(pending.getMemberProvider(), pending.getMemberProviderId()));
        }
        pending.setMemberTermsAgreedAt(LocalDateTime.now());
        return memberRepository.save(pending);
    }

    // 처음 들어온 소셜 회원 (아직 저장 안 함). 비밀번호는 아무도 모르는 값이라 아이디·비밀번호 로그인은 안 된다
    private MemberEntity signup(SocialProfile profile) {
        String key = profile.provider().name().toLowerCase();
        MemberEntity member = new MemberEntity();
        member.setMemberProvider(profile.provider());
        member.setMemberProviderId(profile.id());
        member.setMemberSocialOnly(true);
        member.setMemberUserId(key + "_" + shortHash(profile.id()));
        member.setMemberPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        member.setMemberNickname(uniqueNickname(profile.nickname()));
        member.setMemberEmail(profile.email() != null && !memberRepository.existsByMemberEmail(profile.email())
                ? profile.email()
                : fallbackEmail(profile.provider(), profile.id()));
        member.setMemberProfileImagePath(profile.imageUrl());
        member.setMemberRole(MemberRole.USER);
        return member;
    }

    // 제공자가 이메일을 안 줬거나 이미 쓰이는 이메일일 때 넣는 자리값
    private static String fallbackEmail(MemberProvider provider, String providerId) {
        return provider.name().toLowerCase() + "_" + shortHash(providerId) + "@social.reviewro.local";
    }

    // 닉네임이 겹치면 뒤에 숫자를 붙인다 (컬럼 길이 30자)
    private String uniqueNickname(String nickname) {
        String base = nickname == null || nickname.isBlank() ? "여행자" : nickname.trim();
        if (base.length() > NICKNAME_MAX - 4) {
            base = base.substring(0, NICKNAME_MAX - 4);
        }
        String candidate = base;
        for (int n = 2; memberRepository.existsByMemberNickname(candidate); n++) {
            candidate = base + n;
        }
        return candidate;
    }

    // 제공자 회원 번호는 길이가 제각각이라(네이버는 40자 넘음) 아이디에는 해시 앞 16자만 쓴다
    private static String shortHash(String id) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // 제공자마다 다른 사용자 정보 모양을 하나로 맞춘다
    private record SocialProfile(MemberProvider provider, String id, String nickname, String email, String imageUrl) {

        @SuppressWarnings("unchecked")
        static SocialProfile of(String registrationId, Map<String, Object> attributes) {
            switch (registrationId) {
                case "kakao" -> {
                    // { id, kakao_account: { email?, profile: { nickname, profile_image_url } } }
                    Map<String, Object> account = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
                    Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
                    return new SocialProfile(MemberProvider.KAKAO, String.valueOf(attributes.get("id")),
                            str(profile.get("nickname")), str(account.get("email")),
                            https(profile.get("profile_image_url")));
                }
                case "google" -> {
                    // { sub, name, email, email_verified, picture }
                    String email = Boolean.TRUE.equals(attributes.get("email_verified")) ? str(attributes.get("email")) : null;
                    return new SocialProfile(MemberProvider.GOOGLE, str(attributes.get("sub")),
                            str(attributes.get("name")), email, https(attributes.get("picture")));
                }
                case "naver" -> {
                    // { resultcode, message, response: { id, nickname, name, email, profile_image } }
                    Map<String, Object> response = (Map<String, Object>) attributes.getOrDefault("response", Map.of());
                    String nickname = str(response.get("nickname"));
                    return new SocialProfile(MemberProvider.NAVER, str(response.get("id")),
                            nickname != null ? nickname : str(response.get("name")),
                            str(response.get("email")), https(response.get("profile_image")));
                }
                default -> throw new OAuth2AuthenticationException(
                        new OAuth2Error("unsupported_provider"), "지원하지 않는 로그인 방식입니다: " + registrationId);
            }
        }

        private static String str(Object value) {
            return value == null ? null : value.toString();
        }

        // 사진 주소는 http 로 오기도 해서(카카오) https 로 바꿔 둔다. 그 외 형식은 버린다
        private static String https(Object value) {
            String url = str(value);
            if (url == null) {
                return null;
            }
            if (url.startsWith("http://")) {
                url = "https://" + url.substring("http://".length());
            }
            return url.startsWith("https://") && url.length() <= 255 ? url : null;
        }
    }
}
