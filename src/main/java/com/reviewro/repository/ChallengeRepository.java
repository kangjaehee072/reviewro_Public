package com.reviewro.repository;

import com.reviewro.entity.ChallengeEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public interface ChallengeRepository extends JpaRepository<ChallengeEntity, Long> {

    List<ChallengeEntity> findAllByOrderByChallengeIdAsc(Pageable pageable);

    // 👉 DB에 등록된 챌린지 중 랜덤으로 1건 조회.
    // ORDER BY RAND() 는 표 전체를 읽어서 섞는다. 개수를 센 뒤 무작위 위치의 1건만 읽는다
    default Optional<ChallengeEntity> findRandomOne() {
        long count = count();
        if (count == 0) {
            return Optional.empty();
        }
        int offset = ThreadLocalRandom.current().nextInt((int) Math.min(count, Integer.MAX_VALUE));
        return findAllByOrderByChallengeIdAsc(PageRequest.of(offset, 1)).stream().findFirst();
    }
}
