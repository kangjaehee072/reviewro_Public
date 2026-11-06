package com.reviewro.repository;

import com.reviewro.entity.RememberMeTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RememberMeTokenRepository extends JpaRepository<RememberMeTokenEntity, String> {

    // 그 회원의 모든 기기 (탈퇴, 쿠키 도용이 의심될 때)
    @Modifying
    @Query("DELETE FROM RememberMeTokenEntity t WHERE t.username = :username")
    void deleteAllOfUser(@Param("username") String username);
}
