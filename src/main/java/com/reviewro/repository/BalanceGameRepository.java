package com.reviewro.repository;

import com.reviewro.entity.BalanceGameEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public interface BalanceGameRepository extends JpaRepository<BalanceGameEntity, Long> {

    @Modifying
    @Query("update BalanceGameEntity g " +
            "   set g.balanceGameCountA = g.balanceGameCountA + :delta " +
            " where g.balanceGameId = :id")
    void addCountA(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("update BalanceGameEntity g " +
            "   set g.balanceGameCountB = g.balanceGameCountB + :delta " +
            " where g.balanceGameId = :id")
    void addCountB(@Param("id") Long id, @Param("delta") int delta);

    List<BalanceGameEntity> findAllByOrderByBalanceGameIdDesc();

    long countByBalanceGameStatusTrue();

    @Query("SELECT g FROM BalanceGameEntity g WHERE g.balanceGameStatus = true ORDER BY g.balanceGameId")
    List<BalanceGameEntity> findActive(Pageable pageable);

    // 👉 활성화된 밸런스 게임 중 랜덤 1건 조회.
    // ORDER BY RAND() 는 표 전체를 읽어서 섞는다. 개수를 센 뒤 무작위 위치의 1건만 읽는다
    default Optional<BalanceGameEntity> findRandomOne() {
        long count = countByBalanceGameStatusTrue();
        if (count == 0) {
            return Optional.empty();
        }
        int offset = ThreadLocalRandom.current().nextInt((int) Math.min(count, Integer.MAX_VALUE));
        return findActive(PageRequest.of(offset, 1)).stream().findFirst();
    }


}
