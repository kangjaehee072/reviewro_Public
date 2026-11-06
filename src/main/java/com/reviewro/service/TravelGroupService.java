package com.reviewro.service;

import com.reviewro.dto.ChatMessageDTO;
import com.reviewro.dto.TravelGroupDTO;
import com.reviewro.dto.TravelGroupMemberDTO;
import com.reviewro.dto.TravelGroupPlanDTO;
import com.reviewro.dto.TravelPlansDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.TravelGroupEntity;
import com.reviewro.entity.TravelGroupMemberEntity;
import com.reviewro.entity.TravelGroupMemberStatus;
import com.reviewro.entity.TravelGroupMessageEntity;
import com.reviewro.entity.TravelGroupPlanEntity;
import com.reviewro.entity.TravelPlansEntity;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.TravelGroupMemberRepository;
import com.reviewro.repository.TravelGroupMessageRepository;
import com.reviewro.repository.TravelGroupPlanRepository;
import com.reviewro.repository.TravelGroupRepository;
import com.reviewro.repository.TravelPlansRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 여행 그룹: 만들기, 닉네임 초대 -> 수락/거절, 나가기·삭제, 여행계획 공유, 그룹 채팅.
// 그룹 안의 것은 수락한(JOINED) 구성원만 보고 쓸 수 있다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelGroupService {

    private static final int NAME_MAX = 50;
    private static final int CONTENT_MAX = 500;
    private static final int SEARCH_LIMIT = 5;
    private static final int CODE_LENGTH = 8;
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_VALID_DAYS = 7;          // 초대코드 유효 기간
    private static final int CODE_FAIL_LIMIT = 10;         // 틀린 코드 시도 허용 횟수
    private static final long CODE_FAIL_WINDOW_MS = 10 * 60 * 1000L;

    // 회원별 틀린 초대코드 시도 시각 (서버 메모리)
    private final Map<Long, Deque<Long>> codeFailures = new ConcurrentHashMap<>();

    private final TravelGroupRepository groupRepository;
    private final TravelGroupMemberRepository groupMemberRepository;
    private final TravelGroupPlanRepository groupPlanRepository;
    private final TravelGroupMessageRepository groupMessageRepository;
    private final MemberRepository memberRepository;
    private final TravelPlansRepository travelPlansRepository;
    private final TravelPlansService travelPlansService;

    // ===== 그룹 =====

    // 내가 들어가 있는 그룹 목록
    public List<TravelGroupDTO> myGroups(Long memberId) {
        return groupMemberRepository.findMine(memberId, TravelGroupMemberStatus.JOINED).stream()
                .map(gm -> toDto(gm.getGroup(), null))
                .toList();
    }

    // 나에게 온 초대 목록
    public List<TravelGroupDTO> myInvitations(Long memberId) {
        return groupMemberRepository.findMine(memberId, TravelGroupMemberStatus.INVITED).stream()
                .map(gm -> toDto(gm.getGroup(), gm.getInvitedBy()))
                .toList();
    }

    @Transactional
    public Long create(Long memberId, String name) {
        String groupName = name == null ? "" : name.trim();
        if (groupName.isEmpty()) {
            throw new IllegalArgumentException("그룹 이름을 입력해 주세요.");
        }
        if (groupName.length() > NAME_MAX) {
            throw new IllegalArgumentException("그룹 이름은 " + NAME_MAX + "자 이내로 입력해 주세요.");
        }
        MemberEntity owner = getMember(memberId);
        TravelGroupEntity group = groupRepository.save(TravelGroupEntity.builder()
                .groupName(groupName)
                .owner(owner)
                .build());
        groupMemberRepository.save(TravelGroupMemberEntity.builder()
                .group(group)
                .member(owner)
                .status(TravelGroupMemberStatus.JOINED)
                .build());
        return group.getGroupId();
    }

    // 그룹 화면 머리 정보 (구성원만). 초대코드는 구성원에게만 보여준다
    public TravelGroupDTO read(Long groupId, Long memberId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        TravelGroupDTO dto = toDto(group, null);
        if (group.getInviteCode() != null && !isExpired(group)) {
            dto.setInviteCode(group.getInviteCode());
            dto.setInviteCodeExpiresAt(group.getInviteCodeExpiresAt());
        }
        return dto;
    }

    // 초대코드 만들기·새로 바꾸기 (방장만). 바꾸면 예전 코드로는 못 들어온다
    @Transactional
    public String generateInviteCode(Long groupId, Long memberId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        if (!group.getOwner().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("초대코드는 그룹을 만든 사람만 만들 수 있습니다.");
        }
        String code;
        do {
            code = randomCode();
        } while (groupRepository.existsByInviteCode(code));
        group.setInviteCode(code);
        group.setInviteCodeExpiresAt(LocalDateTime.now().plusDays(CODE_VALID_DAYS));
        return code;
    }

    // 초대코드로 바로 참여. 이미 초대받은 상태였으면 수락한 것으로 친다.
    // 코드를 마구 넣어 보는 걸 막으려고 틀린 시도는 회원마다 10분에 10번까지만 받는다
    @Transactional
    public Long joinByCode(Long memberId, String code) {
        if (!allowCodeAttempt(memberId)) {
            throw new IllegalArgumentException("초대코드를 너무 많이 틀렸어요. 10분 뒤에 다시 시도해 주세요.");
        }
        String key = code == null ? "" : code.trim().toUpperCase();
        TravelGroupEntity group = groupRepository.findByInviteCode(key)
                .filter(found -> !isExpired(found))
                .orElse(null);
        if (group == null) {
            recordCodeFailure(memberId);
            throw new IllegalArgumentException("맞는 초대코드가 없거나 기간이 지났어요. 코드를 다시 확인해 주세요.");
        }
        groupMemberRepository.findByGroup_GroupIdAndMember_MemberId(group.getGroupId(), memberId)
                .ifPresentOrElse(
                        gm -> gm.setStatus(TravelGroupMemberStatus.JOINED),
                        () -> groupMemberRepository.save(TravelGroupMemberEntity.builder()
                                .group(group)
                                .member(getMember(memberId))
                                .status(TravelGroupMemberStatus.JOINED)
                                .build()));
        return group.getGroupId();
    }

    // 초대할 회원 찾기 (닉네임 일부 또는 아이디 완전 일치, 최대 5명, 이미 그룹에 있거나 초대한 사람은 뺀다)
    public List<TravelGroupMemberDTO> searchInvitees(Long groupId, Long memberId, String query) {
        getJoinedGroup(groupId, memberId);
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }
        String escaped = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return memberRepository.searchForInvite(escaped, q, groupId, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(m -> TravelGroupMemberDTO.builder()
                        .memberId(m.getMemberId())
                        .nickname(m.getMemberNickname())
                        .profileImagePath(m.getMemberProfileImagePath())
                        .maskedUserId(mask(m.getMemberUserId()))
                        .build())
                .toList();
    }

    // 구성원 + 수락 대기 중인 초대
    public List<TravelGroupMemberDTO> members(Long groupId, Long memberId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        Long ownerId = group.getOwner().getMemberId();
        return groupMemberRepository.findByGroup(groupId).stream()
                .map(gm -> TravelGroupMemberDTO.builder()
                        .memberId(gm.getMember().getMemberId())
                        .nickname(gm.getMember().getMemberNickname())
                        .profileImagePath(gm.getMember().getMemberProfileImagePath())
                        .status(gm.getStatus())
                        .owner(gm.getMember().getMemberId().equals(ownerId))
                        .build())
                .toList();
    }

    // 닉네임(또는 아이디)으로 초대. 구성원이면 누구나 초대할 수 있다
    @Transactional
    public void invite(Long groupId, Long memberId, String nickname) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        String name = nickname == null ? "" : nickname.trim();
        MemberEntity target = memberRepository.findByMemberNickname(name)
                .or(() -> memberRepository.findByMemberUserId(name))
                .orElseThrow(() -> new IllegalArgumentException("'" + name + "' 회원을 찾을 수 없습니다."));
        groupMemberRepository.findByGroup_GroupIdAndMember_MemberId(groupId, target.getMemberId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(existing.getStatus() == TravelGroupMemberStatus.JOINED
                            ? "이미 그룹에 있는 회원입니다." : "이미 초대한 회원입니다.");
                });
        groupMemberRepository.save(TravelGroupMemberEntity.builder()
                .group(group)
                .member(target)
                .status(TravelGroupMemberStatus.INVITED)
                .invitedBy(getMember(memberId))
                .build());
    }

    @Transactional
    public void accept(Long groupId, Long memberId) {
        getInvitation(groupId, memberId).setStatus(TravelGroupMemberStatus.JOINED);
    }

    @Transactional
    public void decline(Long groupId, Long memberId) {
        groupMemberRepository.delete(getInvitation(groupId, memberId));
    }

    // 나가기. 내가 공유한 계획도 같이 내린다. 만든 사람은 나가지 말고 그룹을 지운다
    @Transactional
    public void leave(Long groupId, Long memberId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        if (group.getOwner().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("그룹을 만든 사람은 나갈 수 없습니다. 그룹을 삭제해 주세요.");
        }
        groupPlanRepository.deleteByGroup_GroupIdAndSharedBy_MemberId(groupId, memberId);
        groupMemberRepository.findByGroup_GroupIdAndMember_MemberId(groupId, memberId)
                .ifPresent(groupMemberRepository::delete);
    }

    // 그룹 삭제 (만든 사람만). 공유 기록·채팅·구성원을 먼저 지운다
    @Transactional
    public void delete(Long groupId, Long memberId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        if (!group.getOwner().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("그룹을 만든 사람만 삭제할 수 있습니다.");
        }
        groupPlanRepository.deleteByGroup_GroupId(groupId);
        groupMessageRepository.deleteByGroup_GroupId(groupId);
        groupMemberRepository.deleteByGroup_GroupId(groupId);
        groupRepository.delete(group);
    }

    // ===== 여행계획 공유 =====

    public List<TravelGroupPlanDTO> sharedPlans(Long groupId, Long memberId) {
        getJoinedGroup(groupId, memberId);
        return groupPlanRepository.findByGroup(groupId).stream()
                .map(gp -> TravelGroupPlanDTO.builder()
                        .planId(gp.getPlan().getPlanId())
                        .planTitle(gp.getPlan().getPlanTitle())
                        .planStartDate(gp.getPlan().getPlanStartDate())
                        .planEndDate(gp.getPlan().getPlanEndDate())
                        .sharedById(gp.getSharedBy().getMemberId())
                        .sharedByNickname(gp.getSharedBy().getMemberNickname())
                        .build())
                .toList();
    }

    // 공유 고르기 칸에 띄울 내 계획 (이미 공유한 건 뺀다)
    public List<TravelPlansDTO> shareablePlans(Long groupId, Long memberId) {
        Set<Long> shared = groupPlanRepository.findByGroup(groupId).stream()
                .map(gp -> gp.getPlan().getPlanId())
                .collect(Collectors.toSet());
        return travelPlansService.getTravelPlanList(memberId).stream()
                .filter(plan -> !shared.contains(plan.getPlanId()))
                .toList();
    }

    // 내 계획만 공유할 수 있다
    @Transactional
    public void sharePlan(Long groupId, Long memberId, Long planId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        TravelPlansEntity plan = travelPlansRepository.findByPlanIdAndMemberId(planId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("공유할 내 여행계획을 찾을 수 없습니다."));
        if (groupPlanRepository.existsByGroup_GroupIdAndPlan_PlanId(groupId, planId)) {
            throw new IllegalArgumentException("이미 공유한 여행계획입니다.");
        }
        groupPlanRepository.save(TravelGroupPlanEntity.builder()
                .group(group)
                .plan(plan)
                .sharedBy(getMember(memberId))
                .build());
    }

    // 공유 내리기 (공유한 사람이나 그룹을 만든 사람)
    @Transactional
    public void unsharePlan(Long groupId, Long memberId, Long planId) {
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        TravelGroupPlanEntity shared = groupPlanRepository.findByGroup_GroupIdAndPlan_PlanId(groupId, planId)
                .orElseThrow(() -> new IllegalArgumentException("공유된 여행계획이 아닙니다."));
        if (!shared.getSharedBy().getMemberId().equals(memberId)
                && !group.getOwner().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("공유한 사람만 내릴 수 있습니다.");
        }
        groupPlanRepository.delete(shared);
    }

    // 공유된 계획 상세 (구성원만, 이 그룹에 공유된 것만)
    public TravelPlansDTO readSharedPlan(Long groupId, Long memberId, Long planId) {
        getJoinedGroup(groupId, memberId);
        if (!groupPlanRepository.existsByGroup_GroupIdAndPlan_PlanId(groupId, planId)) {
            throw new IllegalArgumentException("이 그룹에 공유된 여행계획이 아닙니다.");
        }
        return travelPlansService.readSharedSchedule(planId);
    }

    // ===== 그룹 채팅 =====

    // 구성원인지 확인만 한다 (SSE 연결 전에)
    public void checkJoined(Long groupId, Long memberId) {
        getJoinedGroup(groupId, memberId);
    }

    // 지난 대화 (최근 100개, 시간순). 톡방과 같은 화면 코드를 쓰려고 ChatMessageDTO 로 돌려준다
    public List<ChatMessageDTO> messages(Long groupId, Long memberId) {
        getJoinedGroup(groupId, memberId);
        List<ChatMessageDTO> list = new ArrayList<>(
                groupMessageRepository.findTop100ByGroup_GroupIdOrderByGroupMessageIdDesc(groupId).stream()
                        .map(this::toDto)
                        .toList());
        Collections.reverse(list);
        return list;
    }

    @Transactional
    public ChatMessageDTO saveMessage(Long groupId, Long memberId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("메시지를 입력해 주세요.");
        }
        if (text.length() > CONTENT_MAX) {
            throw new IllegalArgumentException("메시지는 " + CONTENT_MAX + "자 이내로 입력해 주세요.");
        }
        TravelGroupEntity group = getJoinedGroup(groupId, memberId);
        MemberEntity member = getMember(memberId);
        return toDto(groupMessageRepository.save(TravelGroupMessageEntity.builder()
                .group(group)
                .member(member)
                .senderNickname(member.getMemberNickname())
                .content(text)
                .build()));
    }

    // ===== 공통 =====

    // 수락한 구성원일 때만 그룹을 돌려준다. 아니면 없는 그룹처럼 대한다
    private TravelGroupEntity getJoinedGroup(Long groupId, Long memberId) {
        return groupMemberRepository.findByGroup_GroupIdAndMember_MemberId(groupId, memberId)
                .filter(gm -> gm.getStatus() == TravelGroupMemberStatus.JOINED)
                .map(TravelGroupMemberEntity::getGroup)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
    }

    private TravelGroupMemberEntity getInvitation(Long groupId, Long memberId) {
        return groupMemberRepository.findByGroup_GroupIdAndMember_MemberId(groupId, memberId)
                .filter(gm -> gm.getStatus() == TravelGroupMemberStatus.INVITED)
                .orElseThrow(() -> new IllegalArgumentException("받은 초대가 없습니다."));
    }

    // 만료 시각이 없는 예전 코드는 만료된 것으로 본다 (방장이 새로 만들면 된다)
    private boolean isExpired(TravelGroupEntity group) {
        return group.getInviteCodeExpiresAt() == null || group.getInviteCodeExpiresAt().isBefore(LocalDateTime.now());
    }

    private boolean allowCodeAttempt(Long memberId) {
        Deque<Long> fails = codeFailures.get(memberId);
        if (fails == null) {
            return true;
        }
        synchronized (fails) {
            long now = System.currentTimeMillis();
            while (!fails.isEmpty() && now - fails.peekFirst() > CODE_FAIL_WINDOW_MS) {
                fails.pollFirst();
            }
            return fails.size() < CODE_FAIL_LIMIT;
        }
    }

    private void recordCodeFailure(Long memberId) {
        Deque<Long> fails = codeFailures.computeIfAbsent(memberId, k -> new ArrayDeque<>());
        synchronized (fails) {
            fails.addLast(System.currentTimeMillis());
        }
    }

    // 헷갈리는 글자(0 O 1 I L)를 뺀 8자리
    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String mask(String userId) {
        int keep = Math.min(3, userId.length());
        return userId.substring(0, keep) + "****";
    }

    private MemberEntity getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private TravelGroupDTO toDto(TravelGroupEntity group, MemberEntity invitedBy) {
        return TravelGroupDTO.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .ownerId(group.getOwner().getMemberId())
                .ownerNickname(group.getOwner().getMemberNickname())
                .memberCount(groupMemberRepository.countByGroup_GroupIdAndStatus(
                        group.getGroupId(), TravelGroupMemberStatus.JOINED))
                .invitedByNickname(invitedBy == null ? null : invitedBy.getMemberNickname())
                .createdAt(group.getCreatedAt())
                .build();
    }

    private ChatMessageDTO toDto(TravelGroupMessageEntity msg) {
        return ChatMessageDTO.builder()
                .chatMessageId(msg.getGroupMessageId())
                .chatRoomId(msg.getGroup().getGroupId())
                .memberId(msg.getMember() == null ? null : msg.getMember().getMemberId())   // 탈퇴한 회원이면 비어 있다
                .chatRoomMember(msg.getSenderNickname())
                .chatRoomContent(msg.getContent())
                .chatRoomSentAt(msg.getSentAt())
                .build();
    }
}
