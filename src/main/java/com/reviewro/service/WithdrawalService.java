package com.reviewro.service;

import com.reviewro.dto.PhotoMapDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberRole;
import com.reviewro.entity.TravelGroupMemberEntity;
import com.reviewro.entity.TravelGroupMemberStatus;
import com.reviewro.entity.TravelPlansEntity;
import com.reviewro.repository.LocationBookMarkRepository;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.RememberMeTokenRepository;
import com.reviewro.repository.TravelGroupMemberRepository;
import com.reviewro.repository.TravelPlansRepository;
import com.reviewro.security.MemberDetails;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 회원 탈퇴. 회원 행을 실제로 지운다.
// 혼자 쓰던 데이터(여행계획·포토맵·저장한 장소·만든 그룹·좋아요·투표)는 같이 지우고,
// 남과 얽힌 글(게시글·리뷰·댓글·톡방 메시지)은 남기되 작성자 연결을 끊고 이름을 '탈퇴회원' 으로 바꾼다
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class WithdrawalService {

    // 소셜로만 가입해 비밀번호가 없는 회원은 이 문장을 그대로 입력해서 확인한다
    public static final String CONFIRM_TEXT = "탈퇴합니다";

    private static final String WITHDRAWN_NAME = "탈퇴회원";

    // 남기는 글: 작성자 이름을 바꾸고 연결을 끊는다
    private static final List<String> DETACH = List.of(
            "UPDATE FreeBoardEntity e SET e.freeBoardName = :name, e.member = null WHERE e.member.memberId = :id",
            "UPDATE ReviewEntity e SET e.reviewUser = :name, e.member = null WHERE e.member.memberId = :id",
            "UPDATE CompanionPostEntity e SET e.companionPostMember = :name, e.member = null WHERE e.member.memberId = :id",
            "UPDATE ChatMessageEntity e SET e.chatRoomMember = :name, e.member = null WHERE e.member.memberId = :id",
            "UPDATE TravelGroupMessageEntity e SET e.senderNickname = :name, e.member = null WHERE e.member.memberId = :id");

    // 남기는 글 중 이름 칸이 따로 없는 것 (댓글은 화면에서 '탈퇴회원' 으로 보인다)
    private static final List<String> UNLINK = List.of(
            "UPDATE CommentEntity e SET e.member = null WHERE e.member.memberId = :id",
            "UPDATE AdminPostEntity e SET e.member = null, e.adminPostMemberId = null WHERE e.member.memberId = :id",
            "UPDATE BalanceGameEntity e SET e.member = null WHERE e.member.memberId = :id",
            "UPDATE ChatRoomEntity e SET e.member = null WHERE e.member.memberId = :id",
            "UPDATE LocationEntity e SET e.member = null WHERE e.member.memberId = :id",
            "UPDATE TripPlanEntity e SET e.member = null WHERE e.member.memberId = :id",
            "UPDATE TravelGroupMemberEntity e SET e.invitedBy = null WHERE e.invitedBy.memberId = :id");

    // 그 사람 개인의 기록이라 지운다 (투표 수는 게임에 따로 쌓여 있어 그대로다)
    private static final List<String> REMOVE = List.of(
            "DELETE FROM ReactionEntity e WHERE e.member.memberId = :id",
            "DELETE FROM GameVoteEntity e WHERE e.member.memberId = :id");

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final TravelPlansRepository travelPlansRepository;
    private final TravelPlansService travelPlansService;
    private final PhotoMapService photoMapService;
    private final LocationBookMarkRepository bookmarkRepository;
    private final TravelGroupMemberRepository groupMemberRepository;
    private final TravelGroupService travelGroupService;
    private final SessionRegistry sessionRegistry;
    private final RememberMeTokenRepository rememberMeTokenRepository;
    private final EntityManager em;

    public void withdraw(Long memberId, String password, String confirmText) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        // 사이트를 관리할 사람이 없어지지 않게 막는다
        if (member.getMemberRole() == MemberRole.SUPERADMIN) {
            throw new IllegalArgumentException("슈퍼어드민 계정은 탈퇴할 수 없어요. 다른 슈퍼어드민에게 권한을 넘긴 뒤 진행해 주세요.");
        }
        checkIdentity(member, password, confirmText);
        erase(member);
    }

    // 회원과 딸린 데이터를 지운다. 본인 탈퇴와 관리자의 '탈퇴시키기' 가 같이 쓴다 (권한·본인 확인은 부르는 쪽에서)
    public void erase(MemberEntity member) {
        Long memberId = member.getMemberId();
        String userId = member.getMemberUserId();
        removeOwnData(memberId);
        run(DETACH, memberId, true);
        run(UNLINK, memberId, false);
        run(REMOVE, memberId, false);
        memberService.deleteProfileImage(member.getMemberProfileImagePath());
        memberRepository.delete(member);
        // 다른 기기에 남은 자동 로그인 토큰도 지운다
        rememberMeTokenRepository.deleteAllOfUser(userId);
        expireSessions(userId);
        log.info("회원 탈퇴 처리: memberId={}", memberId);
    }

    // 본인 확인: 비밀번호가 있는 회원은 비밀번호, 소셜로만 가입한 회원은 확인 문장
    private void checkIdentity(MemberEntity member, String password, String confirmText) {
        if (member.isSocial()) {
            if (!CONFIRM_TEXT.equals(confirmText == null ? null : confirmText.trim())) {
                throw new IllegalArgumentException("확인 문장 '" + CONFIRM_TEXT + "' 를 정확히 입력해 주세요.");
            }
            return;
        }
        if (password == null || !passwordEncoder.matches(password, member.getMemberPassword())) {
            throw new IllegalArgumentException("비밀번호가 맞지 않아요.");
        }
    }

    // 혼자만 쓰던 데이터를 지운다
    private void removeOwnData(Long memberId) {
        // 그룹: 방장이면 그룹째 삭제, 아니면 나가기 (내가 공유한 계획도 같이 내려간다). 받은 초대는 거절
        for (TravelGroupMemberEntity gm : groupMemberRepository.findMine(memberId, TravelGroupMemberStatus.JOINED)) {
            Long groupId = gm.getGroup().getGroupId();
            if (gm.getGroup().getOwner().getMemberId().equals(memberId)) {
                travelGroupService.delete(groupId, memberId);
            } else {
                travelGroupService.leave(groupId, memberId);
            }
        }
        for (TravelGroupMemberEntity gm : groupMemberRepository.findMine(memberId, TravelGroupMemberStatus.INVITED)) {
            travelGroupService.decline(gm.getGroup().getGroupId(), memberId);
        }

        // 여행계획 (일차·일정·그룹 공유 기록까지)
        for (TravelPlansEntity plan : travelPlansRepository.findByIdList(memberId)) {
            travelPlansService.deleteTravelPlan(plan.getPlanId(), memberId);
        }

        // 포토맵 (사진 파일까지)
        for (PhotoMapDTO map : photoMapService.findMine(memberId)) {
            photoMapService.delete(map.getPhotoMapId(), memberId);
        }

        bookmarkRepository.deleteAllOfMember(memberId);
    }

    private void run(List<String> queries, Long memberId, boolean withName) {
        for (String jpql : queries) {
            var query = em.createQuery(jpql).setParameter("id", memberId);
            if (withName) {
                query.setParameter("name", WITHDRAWN_NAME);
            }
            query.executeUpdate();
        }
    }

    // 다른 기기에 로그인해 둔 세션까지 끊는다
    private void expireSessions(String memberUserId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof MemberDetails details && details.getUsername().equals(memberUserId)) {
                sessionRegistry.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
            }
        }
    }

    // 댓글·그룹 메시지의 작성자 칸은 처음에 NOT NULL 로 만들어졌다. ddl-auto=update 는 이걸 풀어주지 않아서
    // 기동할 때 한 번 확인하고 풀어 둔다 (탈퇴한 회원의 글을 남기려면 비어 있을 수 있어야 한다)
    @EventListener(ApplicationReadyEvent.class)
    public void allowWithdrawnAuthors() {
        for (String table : List.of("comment", "travel_group_message")) {
            Object nullable = em.createNativeQuery("SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?1 AND COLUMN_NAME = 'member_id'")
                    .setParameter(1, table)
                    .getResultStream().findFirst().orElse(null);
            if ("NO".equals(nullable)) {
                em.createNativeQuery("ALTER TABLE " + table + " MODIFY member_id BIGINT NULL").executeUpdate();
                log.info("{}.member_id 를 NULL 허용으로 바꿨습니다 (탈퇴 회원 글 보존)", table);
            }
        }
    }
}
