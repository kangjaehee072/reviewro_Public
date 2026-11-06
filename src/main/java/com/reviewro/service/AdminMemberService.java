package com.reviewro.service;

import com.reviewro.dto.AdminMemberDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberRole;
import com.reviewro.repository.MemberRepository;
import com.reviewro.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    // 한 페이지에 보여줄 회원 수
    public static final int PAGE_SIZE = 10;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;
    private final WithdrawalService withdrawalService;

    // 회원 목록 (최근 가입 순)
    public Page<AdminMemberDTO> list(String keyword, int page) {
        String key = (keyword == null) ? "" : keyword.trim();
        PageRequest request = PageRequest.of(
                Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "memberId"));

        return memberRepository.search(key, request).map(AdminMemberDTO::from);
    }

    // 회원 탈퇴시키기.
    // 지우기 전에 본인 비밀번호를 다시 확인한다 (자리를 비운 사이 눌리는 사고 방지)
    @Transactional
    public void remove(Long targetId, Long actorId, String password) {

        MemberEntity actor = loadActor(actorId, password);
        MemberEntity target = loadTarget(targetId, actorId);

        // 관리자를 지울 수 있는 건 슈퍼어드민뿐
        if (target.getMemberRole() != MemberRole.USER
                && actor.getMemberRole() != MemberRole.SUPERADMIN) {
            throw new IllegalArgumentException("관리자는 슈퍼어드민만 탈퇴시킬 수 있습니다.");
        }
        if (target.getMemberRole() == MemberRole.SUPERADMIN) {
            throw new IllegalArgumentException("슈퍼어드민은 탈퇴시킬 수 없습니다.");
        }

        // 글·댓글 등이 회원을 가리키고 있어 그냥 지우면 막힌다. 본인 탈퇴와 같은 순서로 정리하고 지운다
        withdrawalService.erase(target);
    }

    // 권한 올리기·내리기. 슈퍼어드민만 할 수 있다
    @Transactional
    public void changeRole(Long targetId, MemberRole newRole, Long actorId, String password) {

        MemberEntity actor = loadActor(actorId, password);

        if (actor.getMemberRole() != MemberRole.SUPERADMIN) {
            throw new IllegalArgumentException("권한 변경은 슈퍼어드민만 할 수 있습니다.");
        }
        // 슈퍼어드민을 코드로 찍어내지 않는다. 여기서 만들 수 있는 건 USER 와 ADMIN 뿐이다
        if (newRole != MemberRole.USER && newRole != MemberRole.ADMIN) {
            throw new IllegalArgumentException("줄 수 없는 권한입니다.");
        }

        MemberEntity target = loadTarget(targetId, actorId);

        if (target.getMemberRole() == MemberRole.SUPERADMIN) {
            throw new IllegalArgumentException("슈퍼어드민의 권한은 바꿀 수 없습니다.");
        }
        if (target.getMemberRole() == newRole) {
            throw new IllegalArgumentException("이미 그 권한입니다.");
        }

        target.setMemberRole(newRole);
        expireSessions(target.getMemberUserId());
    }

    // 로그인 세션에는 예전 권한이 남아 있다. 바뀐 권한이 바로 적용되도록 그 회원의 세션을 끊는다
    private void expireSessions(String memberUserId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof MemberDetails details
                    && details.getUsername().equals(memberUserId)) {
                sessionRegistry.getAllSessions(principal, false)
                        .forEach(SessionInformation::expireNow);
            }
        }
    }

    // 작업하는 사람 확인 + 비밀번호 재확인
    private MemberEntity loadActor(Long actorId, String password) {
        MemberEntity actor = memberRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보를 찾을 수 없습니다."));

        if (actor.getMemberRole() == MemberRole.USER) {
            throw new IllegalArgumentException("관리자만 할 수 있습니다.");
        }
        if (password == null || password.isBlank()
                || !passwordEncoder.matches(password, actor.getMemberPassword())) {
            throw new IllegalArgumentException("비밀번호가 맞지 않습니다.");
        }
        return actor;
    }

    // 대상 확인 + 자기 자신 차단
    private MemberEntity loadTarget(Long targetId, Long actorId) {
        if (targetId.equals(actorId)) {
            throw new IllegalArgumentException("본인 계정은 여기서 바꿀 수 없습니다.");
        }
        return memberRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("없는 회원입니다."));
    }
}
