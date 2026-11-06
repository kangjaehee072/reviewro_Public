package com.reviewro.config;

import com.reviewro.security.AccountLinkService;
import com.reviewro.security.DeviceRememberMeServices;
import com.reviewro.security.LoginFailureHandler;
import com.reviewro.security.LoginThrottle;
import com.reviewro.security.LoginThrottleFilter;
import com.reviewro.security.LoginSuccessHandler;
import com.reviewro.security.MemberDetailsService;
import com.reviewro.security.RememberMeTokenStore;
import com.reviewro.security.SocialMemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Log4j2
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;

    @Bean
    // 소셜 로그인 쪽은 이 클래스의 PasswordEncoder 빈을 쓴다. 생성자가 아니라 여기 매개변수로 받아야 순환 참조가 안 생긴다
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SocialLoginRegistrations socialRegistrations,
                                                   SocialMemberService socialMemberService,
                                                   AccountLinkService accountLinkService,
                                                   MemberDetailsService memberDetailsService,
                                                   RememberMeTokenStore rememberMeTokenStore,
                                                   LoginThrottle loginThrottle,
                                                   @Value("${server.servlet.session.cookie.secure:}") String secureCookie)
            throws Exception {
        // 자동 로그인. 토큰은 DB 에 있어서 서버를 다시 켜도 유지된다.
        // key 는 같은 실행 안에서 '이 서버가 만든 인증' 인지 맞춰 보는 데만 쓰여서 켤 때마다 새로 만들어도 된다
        String rememberMeKey = UUID.randomUUID().toString();
        DeviceRememberMeServices rememberMe =
                new DeviceRememberMeServices(rememberMeKey, memberDetailsService, rememberMeTokenStore,
                        "true".equalsIgnoreCase(secureCookie.trim()));   // .env 에서 비워 두면 빈 글자로 온다
        http
                // 컨트롤러마다 검사를 넣다 보면 빠뜨린다. 경로 단위로 먼저 막고, 글 소유자 확인은 서비스에서 한다
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/post/list", "/admin/post/read/**").permitAll()
                        // 이용약관·개인정보 처리방침 수정은 슈퍼어드민만
                        .requestMatchers("/admin/terms/**").hasRole("SUPERADMIN")
                        // AI 모델 서버 고르기도 슈퍼어드민만 (유료 API 를 켜고 끄는 자리)
                        .requestMatchers("/admin/ai/**").hasRole("SUPERADMIN")
                        .requestMatchers("/admin/**",
                                "/challengeinsert", "/challengelist", "/challengeread/**",
                                "/challengeupdate", "/challengedelete/**",
                                "/hashtaginsert", "/hashtaglist", "/hashtagread/**",
                                "/hashtagupdate", "/hashtagdelete/**",
                                "/sajuinsert", "/sajulist", "/sajuread/**", "/sajuupdate/**", "/sajudelete/**")
                            .hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/locations").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/locations/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/locations/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/mypage/**", "/api/travels/**", "/api/bookmarks/**").authenticated()
                        .requestMatchers("/chat/rooms/**", "/groups/**", "/account/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/chat/**").authenticated()
                        // AI 채팅(파트라슈·사주), 포토맵, 글쓰기·수정 화면은 들어올 때 바로 로그인으로 보낸다
                        .requestMatchers("/contents/ai/**", "/contents/saju/**", "/api/ai/**",
                                "/contents/photomap/**",
                                "/review/insert", "/review/update/**",
                                "/freeboard/insert", "/freeboard/update/**",
                                "/companion/insert", "/companion/update/**",
                                "/balance/insert", "/balance/update/**",
                                "/tripplan/insert", "/tripplan/update/**").authenticated()
                        .anyRequest().permitAll())
                // 로그인이 필요한 곳에 로그인 없이 오면: 화면은 로그인 페이지로 보내고(다녀오면 원래 화면으로),
                // fetch·SSE 같은 데이터 요청은 401 로 끝낸다 (로그인 HTML 을 받아서 화면이 깨지지 않게)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) -> {
                    if (isDataRequest(request)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    String target = request.getRequestURI().substring(request.getContextPath().length())
                            + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
                    response.sendRedirect(request.getContextPath() + "/login?next="
                            + URLEncoder.encode(target, StandardCharsets.UTF_8));
                }))
                // 권한이 바뀌거나 탈퇴된 회원의 세션을 끊을 수 있도록 세션을 기록한다
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("memberUserId")
                        .passwordParameter("memberPassword")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                // 로그아웃하면 이 기기의 자동 로그인 토큰도 지운다 (DeviceRememberMeServices.logout)
                .rememberMe(remember -> remember
                        .rememberMeServices(rememberMe)
                        .key(rememberMeKey))
                // HTTPS 강제(HSTS), 외부 사이트로 보낼 때 주소 줄이기, 카메라·마이크·결제 기능 막기 (위치는 지도가 쓰므로 둔다)
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(15552000))
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), payment=()")))
                .addFilterBefore(new LoginThrottleFilter(loginThrottle), UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable());

        // 소셜 로그인 (키가 하나라도 있을 때만). /oauth2/authorization/{kakao|google|naver} 로 시작한다
        if (!socialRegistrations.list().isEmpty()) {
            SimpleUrlAuthenticationFailureHandler socialFailure = new SimpleUrlAuthenticationFailureHandler("/login?error=social");
            InMemoryClientRegistrationRepository clients = new InMemoryClientRegistrationRepository(socialRegistrations.list());
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .clientRegistrationRepository(clients)
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestResolver(new HttpsRedirectUriResolver(clients)))
                    .userInfoEndpoint(info -> info.userService(socialMemberService))
                    .successHandler(loginSuccessHandler)
                    // 사용자가 동의를 취소했거나 제공자 쪽 오류. 아이디·비밀번호 오류와 다른 문구를 띄운다.
                    // 계정 연동하러 갔다 실패한 거면 (로그인은 그대로라) 연동 화면으로 돌려보낸다
                    .failureHandler((request, response, exception) -> {
                        // 왜 실패했는지 남겨야 배포 서버에서 원인을 찾을 수 있다 (화면에는 ?error=social 만 간다)
                        log.warn("소셜 로그인 실패: {} (요청 주소 {})", exception.getMessage(),
                                request.getRequestURL(), exception);
                        if (accountLinkService.failIfPending()) {
                            response.sendRedirect(request.getContextPath() + "/account/link");
                        } else {
                            socialFailure.onAuthenticationFailure(request, response, exception);
                        }
                    }));
        }
        return http.build();
    }

    // 화면이 아니라 데이터를 달라는 요청인지 (/api, fetch, SSE)
    static boolean isDataRequest(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (accept != null && (accept.contains("application/json") || accept.contains("text/event-stream")));
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // 세션이 끝나면 SessionRegistry 에서도 지워지게 한다
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
