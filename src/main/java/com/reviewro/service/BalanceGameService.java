package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.BalanceGameCommentDTO;
import com.reviewro.dto.BalanceGameDTO;
import com.reviewro.entity.BalanceGameEntity;
import com.reviewro.entity.GameVoteEntity;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberRole;
import com.reviewro.repository.BalanceGameRepository;
import com.reviewro.repository.GameVoteRepository;
import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceGameService {

    private final BalanceGameRepository balanceGameRepository;
    private final GameVoteRepository gameVoteRepository;
    private final MemberRepository memberRepository;

    private static final int PAGE_SIZE = 10;

    // 1. 밸런스 게임 생성 (로그인한 회원의 ID를 받아 저장)
    @Transactional
    public Long insert(Long memberId, BalanceGameDTO dto) {
        if (dto.getBalanceGameTitle() == null || dto.getBalanceGameTitle().isBlank()) {
            throw new IllegalArgumentException("게임 제목을 입력해 주세요.");
        }
        if (dto.getBalanceGameOptionA() == null || dto.getBalanceGameOptionA().isBlank() ||
                dto.getBalanceGameOptionB() == null || dto.getBalanceGameOptionB().isBlank()) {
            throw new IllegalArgumentException("옵션 A와 B를 모두 입력해 주세요.");
        }

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        BalanceGameEntity entity = new BalanceGameEntity();
        entity.setBalanceGameTitle(dto.getBalanceGameTitle().trim());
        entity.setBalanceGameOptionA(dto.getBalanceGameOptionA().trim());
        entity.setBalanceGameOptionB(dto.getBalanceGameOptionB().trim());
        entity.setBalanceGameCountA(0L);
        entity.setBalanceGameCountB(0L);
        entity.setBalanceGameStatus(dto.getBalanceGameStatus() != null ? dto.getBalanceGameStatus() : true);
        entity.setMember(member); // 작성자 맵핑

        return balanceGameRepository.save(entity).getBalanceGameId();
    }

    // 2. 전체 게임 목록 조회
    public Page<BalanceGameDTO> list(Integer page) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "balanceGameId")
        );

        Page<BalanceGameEntity> entityPage = balanceGameRepository.findAll(pageRequest);
        List<BalanceGameDTO> dtoList = new ArrayList<>();

        for (BalanceGameEntity entity : entityPage.getContent()) {
            BalanceGameDTO dto = new BalanceGameDTO();
            dto.setBalanceGameId(entity.getBalanceGameId());
            dto.setBalanceGameTitle(entity.getBalanceGameTitle());
            dto.setBalanceGameOptionA(entity.getBalanceGameOptionA());
            dto.setBalanceGameOptionB(entity.getBalanceGameOptionB());
            dto.setBalanceGameCountA(entity.getBalanceGameCountA() != null ? entity.getBalanceGameCountA() : 0L);
            dto.setBalanceGameCountB(entity.getBalanceGameCountB() != null ? entity.getBalanceGameCountB() : 0L);
            dto.setBalanceGameStatus(entity.getBalanceGameStatus());
            dtoList.add(dto);
        }


        return new PageImpl<>(dtoList, pageRequest, entityPage.getTotalElements());
    }

    // 3. 게임 상세 조회 (작성자 ID 세팅 추가)
    public BalanceGameDTO read(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("게임 번호가 올바르지 않습니다.");
        }

        BalanceGameEntity entity = balanceGameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 밸런스 게임입니다."));

        BalanceGameDTO dto = new BalanceGameDTO();
        dto.setBalanceGameId(entity.getBalanceGameId());
        dto.setBalanceGameTitle(entity.getBalanceGameTitle());
        dto.setBalanceGameOptionA(entity.getBalanceGameOptionA());
        dto.setBalanceGameOptionB(entity.getBalanceGameOptionB());
        dto.setBalanceGameCountA(entity.getBalanceGameCountA() != null ? entity.getBalanceGameCountA() : 0);
        dto.setBalanceGameCountB(entity.getBalanceGameCountB() != null ? entity.getBalanceGameCountB() : 0);
        dto.setBalanceGameStatus(entity.getBalanceGameStatus());

        // 작성자 ID 세팅 (DTO에 balanceGameMemberId 필드가 있다고 가정)
        if (entity.getMember() != null) {
            dto.setBalanceGameMemberId(entity.getMember().getMemberId());
        }

        return dto;
    }

// 4. 투표 처리 (로그인 필수 및 중복 투표 방지)
@Transactional
public String vote(Long gameId, Long memberId, String option) {

    if (!"A".equals(option) && !"B".equals(option)) {
        throw new IllegalArgumentException("올바르지 않은 선택지입니다.");
    }
    if (!balanceGameRepository.existsById(gameId)) {
        throw new IllegalArgumentException("존재하지 않는 밸런스 게임입니다.");
    }

    Optional<GameVoteEntity> found = gameVoteRepository
            .findByBalanceGame_BalanceGameIdAndMember_MemberId(gameId, memberId);

    // 1) 처음 투표
    if (found.isEmpty()) {
        GameVoteEntity vote = new GameVoteEntity();
        vote.setBalanceGame(balanceGameRepository.getReferenceById(gameId));
        vote.setMember(memberRepository.getReferenceById(memberId));
        vote.setGameVoteSelectedOption(option);
        gameVoteRepository.save(vote);
        addCount(gameId, option, 1);
        return "투표했습니다!";
    }

    GameVoteEntity vote = found.get();
    String before = vote.getGameVoteSelectedOption();

    // 2) 같은 걸 또 눌렀다 -> 취소
    if (option.equals(before)) {
        gameVoteRepository.delete(vote);
        addCount(gameId, option, -1);
        return "투표를 취소했습니다.";
    }

    // 3) 다른 걸 눌렀다 -> 변경
    vote.setGameVoteSelectedOption(option);
    addCount(gameId, before, -1);
    addCount(gameId, option, 1);
    return "투표를 바꿨습니다.";
}

private void addCount(Long gameId, String option, int delta) {
    if ("A".equals(option)) {
        balanceGameRepository.addCountA(gameId, delta);
    } else {
        balanceGameRepository.addCountB(gameId, delta);
    }
}

    // 1. 작성자 본인 또는 관리자 권한 검증용 메서드 수정
    private MemberEntity getOwnedOrAdmin(Long memberId, BalanceGameEntity entity) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 관리자이거나 글 작성자 본인일 경우 통과
        boolean isAdmin = (member.getMemberRole() != MemberRole.USER);
        boolean isWriter = (entity.getMember() != null && entity.getMember().getMemberId().equals(memberId));

        if (isAdmin || isWriter) {
            return member;
        }

        throw new IllegalArgumentException("본인이 쓴 글만 수정하거나 삭제할 수 있습니다.");
    }

    // 2. 밸런스 게임 수정 메서드
    @Transactional
    public void update(Long memberId, BalanceGameDTO dto) {
        if (dto.getBalanceGameId() == null) {
            throw new IllegalArgumentException("밸런스 게임 번호가 없습니다.");
        }

        BalanceGameEntity entity = balanceGameRepository.findById(dto.getBalanceGameId())
                .orElseThrow(() -> new IllegalArgumentException("해당 밸런스 게임을 찾을 수 없습니다."));

        // 작성자 본인 혹은 관리자 체크
        getOwnedOrAdmin(memberId, entity);

        if (dto.getBalanceGameTitle() != null) {
            entity.setBalanceGameTitle(dto.getBalanceGameTitle().trim());
        }
        if (dto.getBalanceGameOptionA() != null) {
            entity.setBalanceGameOptionA(dto.getBalanceGameOptionA().trim());
        }
        if (dto.getBalanceGameOptionB() != null) {
            entity.setBalanceGameOptionB(dto.getBalanceGameOptionB().trim());
        }
    }

    // 3. 밸런스 게임 삭제 메서드
    @Transactional
    public void delete(Long id, Long memberId) {
        if (id == null) {
            throw new IllegalArgumentException("밸런스 게임 번호가 없습니다.");
        }

        BalanceGameEntity entity = balanceGameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 밸런스 게임을 찾을 수 없습니다."));

        // 작성자 본인 혹은 관리자 체크
        getOwnedOrAdmin(memberId, entity);

        balanceGameRepository.delete(entity);
    }

    // 🎲 메인용 랜덤 밸런스 게임 1개 조회
    public BalanceGameDTO getRandomBalance() {
        BalanceGameEntity entity = balanceGameRepository.findRandomOne().orElse(null);

        if (entity == null) {
            return null;
        }

        BalanceGameDTO dto = new BalanceGameDTO();
        dto.setBalanceGameId(entity.getBalanceGameId());
        dto.setBalanceGameTitle(entity.getBalanceGameTitle());
        dto.setBalanceGameOptionA(entity.getBalanceGameOptionA());
        dto.setBalanceGameOptionB(entity.getBalanceGameOptionB());
        dto.setBalanceGameCountA(entity.getBalanceGameCountA() != null ? entity.getBalanceGameCountA() : 0L);
        dto.setBalanceGameCountB(entity.getBalanceGameCountB() != null ? entity.getBalanceGameCountB() : 0L);
        dto.setBalanceGameStatus(entity.getBalanceGameStatus());

        return dto;
    }


}