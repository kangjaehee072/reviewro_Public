package com.reviewro.security;

import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberProvider;
import com.reviewro.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 계정 연동 흐름 (DB 없이 MemberRepository 를 흉내 낸다)
class AccountLinkServiceTest {

    private MemberRepository repo;
    private AccountLinkService service;
    private MemberEntity local;     // 아이디 회원 (연동하려는 사람)
    private MemberEntity kakaoOnly; // 같은 카카오 계정으로 따로 가입한 소셜 전용 회원

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        repo = mock(MemberRepository.class);
        service = new AccountLinkService(repo);

        local = member(1L, "local", null, null, null);
        kakaoOnly = member(2L, "kakao_only", MemberProvider.KAKAO, "K1", true);
        when(repo.findById(1L)).thenReturn(Optional.of(local));
        when(repo.findById(2L)).thenReturn(Optional.of(kakaoOnly));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 연동중이_아니면_평소_로그인() {
        assertTrue(service.onSocialLogin(MemberProvider.KAKAO, "K9").isEmpty());
    }

    @Test
    void 처음보는_카카오면_바로_연동() {
        when(repo.findByMemberProviderAndMemberProviderId(MemberProvider.KAKAO, "K9")).thenReturn(Optional.empty());
        service.start(1L, MemberProvider.KAKAO);

        assertSame(local, service.onSocialLogin(MemberProvider.KAKAO, "K9").orElseThrow());
        assertEquals(MemberProvider.KAKAO, local.getMemberProvider());
        assertEquals("K9", local.getMemberProviderId());
        assertFalse(local.isSocial(), "연동한 아이디 회원은 여전히 비밀번호 회원");
        assertFalse(service.takeResult().orElseThrow().error());
        // 연동 중 표시는 한 번 쓰면 사라진다
        assertTrue(service.onSocialLogin(MemberProvider.KAKAO, "K9").isEmpty());
    }

    @Test
    void 소셜전용_계정이_있으면_확인뒤_옮긴다() {
        when(repo.findByMemberProviderAndMemberProviderId(MemberProvider.KAKAO, "K1")).thenReturn(Optional.of(kakaoOnly));
        service.start(1L, MemberProvider.KAKAO);

        // 로그인한 사람은 그대로, 아직 아무것도 안 바뀜
        assertSame(local, service.onSocialLogin(MemberProvider.KAKAO, "K1").orElseThrow());
        assertNull(local.getMemberProvider());
        assertEquals("K1", kakaoOnly.getMemberProviderId());
        assertTrue(service.hasPendingScreen());
        assertEquals("kakao_only", service.conflict().orElseThrow().otherNickname());

        service.confirm(1L);
        assertNull(kakaoOnly.getMemberProvider());
        assertNull(kakaoOnly.getMemberProviderId());
        assertEquals("K1", local.getMemberProviderId());
        assertTrue(service.conflict().isEmpty());
    }

    @Test
    void 다른_회원이_확인을_가로채지_못한다() {
        when(repo.findByMemberProviderAndMemberProviderId(MemberProvider.KAKAO, "K1")).thenReturn(Optional.of(kakaoOnly));
        service.start(1L, MemberProvider.KAKAO);
        service.onSocialLogin(MemberProvider.KAKAO, "K1");

        assertThrows(IllegalArgumentException.class, () -> service.confirm(3L));
        assertEquals("K1", kakaoOnly.getMemberProviderId());
    }

    @Test
    void 다른_아이디회원에_연동된_카카오면_막는다() {
        MemberEntity otherLocal = member(4L, "other", MemberProvider.KAKAO, "K5", false);
        when(repo.findByMemberProviderAndMemberProviderId(MemberProvider.KAKAO, "K5")).thenReturn(Optional.of(otherLocal));
        service.start(1L, MemberProvider.KAKAO);

        assertSame(local, service.onSocialLogin(MemberProvider.KAKAO, "K5").orElseThrow());
        assertNull(local.getMemberProvider());
        assertEquals("K5", otherLocal.getMemberProviderId());
        assertTrue(service.takeResult().orElseThrow().error());
    }

    @Test
    void 소셜전용_회원은_연동을_시작할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> service.start(2L, MemberProvider.KAKAO));
    }

    @Test
    void 연동_해제() {
        local.setMemberProvider(MemberProvider.KAKAO);
        local.setMemberProviderId("K9");
        local.setMemberSocialOnly(false);

        service.unlink(1L);
        assertNull(local.getMemberProvider());
        assertThrows(IllegalArgumentException.class, () -> service.unlink(2L), "소셜 전용은 해제 불가");
    }

    private static MemberEntity member(Long id, String nickname, MemberProvider provider, String providerId, Boolean socialOnly) {
        MemberEntity m = new MemberEntity();
        m.setMemberId(id);
        m.setMemberUserId(nickname);
        m.setMemberNickname(nickname);
        m.setMemberProvider(provider);
        m.setMemberProviderId(providerId);
        m.setMemberSocialOnly(socialOnly);
        return m;
    }
}
