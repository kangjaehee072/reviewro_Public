package com.reviewro.security;

import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberProvider;
import com.reviewro.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.Optional;

// 계정 연동: 아이디·비밀번호로 가입한 회원에 소셜 계정(지금은 카카오)을 붙인다.
//
// 흐름
//   1) 아이디 회원으로 로그인한 채 /account/link 에서 '연동' → start() 가 세션에 "연동 중" 표시를 남기고 소셜 로그인으로 보낸다
//   2) 소셜 로그인이 끝나면 SocialMemberService 가 onSocialLogin() 을 불러, 새로 가입·로그인하는 대신 지금 회원에 붙인다
//   3) 그 소셜 계정으로 이미 '소셜 전용' 회원이 따로 있으면 바로 붙이지 않고 확인을 받는다(confirm).
//      확인하면 그 소셜 전용 회원에서 연결을 떼어 지금 회원에 붙인다. 소셜 전용 회원은 비밀번호도 없어서
//      더 이상 로그인할 수 없게 되고, 쓴 글은 그 닉네임으로 그대로 남는다
//   4) 그 소셜 계정이 이미 다른 아이디 회원에 연동돼 있으면 막는다
// 어느 경우든 로그인한 사람은 바뀌지 않는다 (연동하던 아이디 회원 그대로)
@Service
@RequiredArgsConstructor
public class AccountLinkService {

    // 세션에 남기는 값
    private static final String PENDING = "accountLink.pending";   // 연동 시작함 (회원 번호, 제공자)
    private static final String CONFLICT = "accountLink.conflict"; // 소셜 전용 회원이 따로 있어 확인 필요
    private static final String RESULT = "accountLink.result";     // 화면에 한 번 띄울 결과 문구

    private final MemberRepository memberRepository;

    public record Pending(Long memberId, MemberProvider provider) implements Serializable {}

    public record Conflict(Long memberId, MemberProvider provider, String providerId,
                           Long otherMemberId, String otherNickname) implements Serializable {}

    public record Result(String message, boolean error) implements Serializable {}

    // 1) 연동 시작. 아이디 회원이고 아직 아무것도 연동하지 않았을 때만
    public void start(Long memberId, MemberProvider provider) {
        MemberEntity member = getMember(memberId);
        if (member.isSocial()) {
            throw new IllegalArgumentException("소셜로 가입한 계정에는 연동할 수 없어요. 아이디로 가입한 계정으로 로그인해 주세요.");
        }
        if (member.getMemberProvider() != null) {
            throw new IllegalArgumentException("이미 연동된 계정이 있어요. 먼저 연동을 해제해 주세요.");
        }
        HttpSession session = session();
        session.removeAttribute(CONFLICT);
        session.removeAttribute(RESULT);
        session.setAttribute(PENDING, new Pending(memberId, provider));
    }

    // 2) 소셜 로그인 직후. 연동 중이 아니면 비어 있는 값을 돌려줘서 평소대로 로그인·가입하게 한다
    @Transactional
    public Optional<MemberEntity> onSocialLogin(MemberProvider provider, String providerId) {
        HttpSession session = session();
        Object value = session.getAttribute(PENDING);
        if (!(value instanceof Pending pending) || pending.provider() != provider) {
            return Optional.empty();
        }
        session.removeAttribute(PENDING);
        MemberEntity me = getMember(pending.memberId());

        Optional<MemberEntity> owner = memberRepository.findByMemberProviderAndMemberProviderId(provider, providerId);
        if (owner.isEmpty()) {
            attach(me, provider, providerId);
            result(session, label(provider) + " 계정을 연동했어요. 이제 " + label(provider) + "로도 이 계정에 로그인할 수 있어요.", false);
        } else if (owner.get().getMemberId().equals(me.getMemberId())) {
            result(session, "이미 연동된 " + label(provider) + " 계정이에요.", false);
        } else if (owner.get().isSocial()) {
            MemberEntity other = owner.get();
            session.setAttribute(CONFLICT, new Conflict(me.getMemberId(), provider, providerId,
                    other.getMemberId(), other.getMemberNickname()));
        } else {
            result(session, "이 " + label(provider) + " 계정은 이미 다른 회원에 연동돼 있어요.", true);
        }
        return Optional.of(me);
    }

    // 3) 소셜 전용 회원이 따로 있을 때 '연동하기' 확인
    @Transactional
    public MemberEntity confirm(Long memberId) {
        HttpSession session = session();
        if (!(session.getAttribute(CONFLICT) instanceof Conflict conflict) || !conflict.memberId().equals(memberId)) {
            throw new IllegalArgumentException("확인할 연동 요청이 없어요. 처음부터 다시 해 주세요.");
        }
        session.removeAttribute(CONFLICT);
        MemberEntity other = getMember(conflict.otherMemberId());
        // 사이에 바뀌었으면 다시 확인한다
        if (other.getMemberProvider() != conflict.provider() || !conflict.providerId().equals(other.getMemberProviderId())) {
            throw new IllegalArgumentException("연동 상태가 바뀌었어요. 처음부터 다시 해 주세요.");
        }
        other.setMemberProvider(null);
        other.setMemberProviderId(null);
        memberRepository.flush(); // 같은 (제공자, 번호) 유니크 제약에 걸리지 않게 먼저 뗀다

        MemberEntity me = getMember(memberId);
        attach(me, conflict.provider(), conflict.providerId());
        result(session, label(conflict.provider()) + " 계정을 연동했어요. 예전 " + label(conflict.provider())
                + " 전용 계정(" + other.getMemberNickname() + ")으로는 더 이상 로그인되지 않아요.", false);
        return me;
    }

    // 연동하러 갔다가 소셜 쪽에서 취소·실패했을 때. 연동 중이었으면 true (연동 화면으로 돌려보낸다)
    public boolean failIfPending() {
        HttpSession session = session();
        if (session.getAttribute(PENDING) == null) {
            return false;
        }
        session.removeAttribute(PENDING);
        result(session, "연동하지 못했어요. 다시 시도해 주세요.", true);
        return true;
    }

    public void cancel() {
        HttpSession session = session();
        session.removeAttribute(CONFLICT);
        session.removeAttribute(PENDING);
    }

    // 연동 해제 (아이디 회원만. 소셜 전용 회원은 떼면 로그인할 방법이 없어진다)
    @Transactional
    public MemberEntity unlink(Long memberId) {
        MemberEntity me = getMember(memberId);
        if (me.isSocial()) {
            throw new IllegalArgumentException("소셜로 가입한 계정은 연동을 해제할 수 없어요.");
        }
        if (me.getMemberProvider() == null) {
            throw new IllegalArgumentException("연동된 계정이 없어요.");
        }
        String name = label(me.getMemberProvider());
        me.setMemberProvider(null);
        me.setMemberProviderId(null);
        result(session(), name + " 연동을 해제했어요.", false);
        return me;
    }

    // 소셜 로그인이 끝난 뒤 연동 화면으로 돌려보낼지 (LoginSuccessHandler)
    public boolean hasPendingScreen() {
        HttpSession session = session();
        return session.getAttribute(CONFLICT) != null || session.getAttribute(RESULT) != null;
    }

    public Optional<Conflict> conflict() {
        return session().getAttribute(CONFLICT) instanceof Conflict c ? Optional.of(c) : Optional.empty();
    }

    // 결과 문구는 한 번 보여주고 지운다
    public Optional<Result> takeResult() {
        HttpSession session = session();
        Object value = session.getAttribute(RESULT);
        session.removeAttribute(RESULT);
        return value instanceof Result r ? Optional.of(r) : Optional.empty();
    }

    public static String label(MemberProvider provider) {
        return switch (provider) {
            case KAKAO -> "카카오";
            case GOOGLE -> "구글";
            case NAVER -> "네이버";
            case LOCAL -> "아이디";
        };
    }

    private void attach(MemberEntity me, MemberProvider provider, String providerId) {
        me.setMemberProvider(provider);
        me.setMemberProviderId(providerId);
        me.setMemberSocialOnly(false);
    }

    private void result(HttpSession session, String message, boolean error) {
        session.setAttribute(RESULT, new Result(message, error));
    }

    private MemberEntity getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없어요."));
    }

    private static HttpSession session() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    }
}
