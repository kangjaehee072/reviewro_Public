package com.reviewro.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// 소셜 로그인(카카오·구글·네이버) 앱 등록 정보. 키는 .env 에서 온다 (application.properties 의 social.*).
// 키가 비어 있는 제공자는 빼서, 키가 없는 팀원도 서버를 띄울 수 있고 화면에도 그 버튼이 안 나온다
@Component
@Log4j2
public class SocialLoginRegistrations {

    private static final String CALLBACK_PATH = "/login/oauth2/code/{registrationId}";

    private final List<ClientRegistration> registrations = new ArrayList<>();

    // 로그인 후 돌아올 주소. 보통은 요청 주소({baseUrl})로 만들지만,
    // 포트 릴레이처럼 앞단이 https 를 알려 주지 않으면 http 로 만들어져서 제공자가 거절한다(KOE006).
    // 그럴 땐 .env 의 SOCIAL_REDIRECT_BASE 에 실제 주소(예: https://reviewro.mrpark.pro)를 넣는다
    private final String redirectUri;

    // 심사 중인 제공자 (social.pending). 버튼만 보이고 로그인은 막는다
    private final List<String> pending;

    public SocialLoginRegistrations(@Value("${social.redirect-base:}") String redirectBase,
                                    @Value("${social.pending:}") List<String> pending,
                                    @Value("${social.kakao.client-id:}") String kakaoId,
                                    @Value("${social.kakao.client-secret:}") String kakaoSecret,
                                    @Value("${social.google.client-id:}") String googleId,
                                    @Value("${social.google.client-secret:}") String googleSecret,
                                    @Value("${social.naver.client-id:}") String naverId,
                                    @Value("${social.naver.client-secret:}") String naverSecret) {
        String base = redirectBase.isBlank() ? "{baseUrl}" : redirectBase.trim().replaceAll("/+$", "");
        this.redirectUri = base + CALLBACK_PATH;
        if (kakaoId.isBlank()) {
            // 버튼이 조용히 사라지면 원인을 찾기 어려워서, 키가 비면 띄울 때 한 줄 알린다
            log.warn("KAKAO_REST_KEY 가 비어 있어 카카오 로그인 버튼을 숨깁니다. 읽은 .env 위치: {}/.env 또는 ./.env",
                    System.getProperty("app.home"));
        } else {
            // 어느 앱 키로 도는지 알 수 있게 앞 6자리만 남긴다 (키 전체는 찍지 않는다)
            log.info("카카오 로그인 사용: REST 키 {}… / Client Secret {} / 돌아올 주소 {}",
                    kakaoId.substring(0, Math.min(6, kakaoId.length())),
                    kakaoSecret.isBlank() ? "없음" : "있음", redirectUri);
            // 이메일 동의항목은 비즈 앱 심사가 있어야 받을 수 있어 닉네임·사진만 받는다
            registrations.add(ClientRegistration.withRegistrationId("kakao")
                    .clientName("카카오")
                    .clientId(kakaoId)
                    .clientSecret(kakaoSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(redirectUri)
                    .scope("profile_nickname", "profile_image")
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .tokenUri("https://kauth.kakao.com/oauth/token")
                    .userInfoUri("https://kapi.kakao.com/v2/user/me")
                    .userNameAttributeName("id")
                    .build());
        }
        if (!googleId.isBlank()) {
            // openid 를 빼야 OIDC 가 아닌 일반 OAuth2 흐름(SocialMemberService)으로 들어온다
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientName("구글")
                    .clientId(googleId)
                    .clientSecret(googleSecret)
                    .redirectUri(redirectUri)
                    .scope("profile", "email")
                    .build());
        }
        if (!naverId.isBlank()) {
            registrations.add(ClientRegistration.withRegistrationId("naver")
                    .clientName("네이버")
                    .clientId(naverId)
                    .clientSecret(naverSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(redirectUri)
                    .scope("name", "email", "profile_image", "nickname")
                    .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                    .tokenUri("https://nid.naver.com/oauth2.0/token")
                    .userInfoUri("https://openapi.naver.com/v1/nid/me")
                    .userNameAttributeName("response")
                    .build());
        }

        // 심사 중이면 키가 있어도 등록하지 않는다. /oauth2/authorization/{id} 를 직접 쳐도 안 열린다
        this.pending = pending.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        registrations.removeIf(r -> this.pending.contains(r.getRegistrationId()));
    }

    public List<ClientRegistration> list() {
        return registrations;
    }

    // 화면에 버튼을 띄울 제공자 (kakao, google, naver 중 키가 있고 심사 중이 아닌 것)
    public List<String> ids() {
        return registrations.stream().map(ClientRegistration::getRegistrationId).toList();
    }

    // 심사 중이라 눌리지 않는 버튼으로 띄울 제공자 (키가 없어도 보인다)
    public List<String> pending() {
        return pending;
    }
}
