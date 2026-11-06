package com.reviewro.service;

import com.reviewro.dto.ReactionDTO;
import com.reviewro.entity.*;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final MemberRepository memberRepository;

    public ReactionDTO read(TargetType type, Long targetId, Long memberId) {
        String myKind = null;
        if (memberId != null) {
            myKind = reactionRepository
                    .findByTargetTypeAndTargetIdAndMember_MemberId(type, targetId, memberId)
                    .map(r -> r.getKind().name())
                    .orElse(null);
        }
        return new ReactionDTO(
                reactionRepository.countByTargetTypeAndTargetIdAndKind(
                        type, targetId, ReactionKind.LIKE),
                reactionRepository.countByTargetTypeAndTargetIdAndKind(
                        type, targetId, ReactionKind.DISLIKE),
                myKind);
    }

    // 여러 글의 좋아요 수를 한 번에 센다. 없는 글은 0 으로 채운다 (목록에서 글마다 쿼리 나가는 것 방지)
    public Map<Long, Long> likeCounts(TargetType type, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : reactionRepository.countByTargetIds(type, ReactionKind.LIKE, ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    // 누르기. 같은 걸 또 누르면 취소, 다른 걸 누르면 변경
    @Transactional
    public ReactionDTO toggle(TargetType type, Long targetId,
                              Long memberId, ReactionKind kind) {

        Optional<ReactionEntity> found = reactionRepository
                .findByTargetTypeAndTargetIdAndMember_MemberId(type, targetId, memberId);

        if (found.isEmpty()) {
            ReactionEntity r = new ReactionEntity();
            r.setTargetType(type);
            r.setTargetId(targetId);
            r.setMember(memberRepository.getReferenceById(memberId));
            r.setKind(kind);
            reactionRepository.save(r);

        } else if (found.get().getKind() == kind) {
            reactionRepository.delete(found.get());      // 같은 것 -> 취소

        } else {
            found.get().setKind(kind);                   // 다른 것 -> 변경
        }

        return read(type, targetId, memberId);
    }
}
