package com.reviewro.config;

import com.reviewro.security.DeviceRememberMeServices;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

// 소셜 로그인 '돌아올 주소(redirect_uri)'를 실제 도메인이면 https 로 바꾼다.
// 포트 릴레이처럼 https 를 앞에서 받고 앱엔 http 로 넘기면 앱이 http 주소를 만들어서
// 카카오가 KOE006(등록 안 된 주소)으로 거절한다. .env 의 SOCIAL_REDIRECT_BASE 를 비워 둬도 되게 한다.
// localhost 같은 로컬 주소는 그대로 http 로 둔다. SOCIAL_REDIRECT_BASE 를 넣었으면 그 값이 이미 https 라 손댈 게 없다
class HttpsRedirectUriResolver implements OAuth2AuthorizationRequestResolver {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    HttpsRedirectUriResolver(ClientRegistrationRepository clients) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest resolved = delegate.resolve(request);
        // 소셜 로그인을 시작하는 순간이면 '자동 로그인' 체크 여부를 세션에 적어 둔다 (돌아왔을 때 쓴다)
        if (resolved != null) {
            DeviceRememberMeServices.rememberSocialChoice(request);
        }
        return toHttps(resolved);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return toHttps(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest toHttps(OAuth2AuthorizationRequest original) {
        if (original == null || original.getRedirectUri() == null) {
            return original;
        }
        UriComponents uri = UriComponentsBuilder.fromUriString(original.getRedirectUri()).build();
        if (!"http".equals(uri.getScheme()) || uri.getHost() == null || LOCAL_HOSTS.contains(uri.getHost())) {
            return original;
        }
        // 기본 포트(80)는 https 에서 맞지 않으니 뺀다. 다른 포트가 붙어 있으면 그대로 둔다
        int port = uri.getPort() == 80 ? -1 : uri.getPort();
        String https = UriComponentsBuilder.newInstance().uriComponents(uri).scheme("https").port(port).toUriString();
        return OAuth2AuthorizationRequest.from(original).redirectUri(https).build();
    }
}
