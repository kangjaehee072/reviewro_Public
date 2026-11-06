package com.reviewro.repository;

import com.reviewro.entity.TravelGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelGroupRepository extends JpaRepository<TravelGroupEntity, Long> {

    Optional<TravelGroupEntity> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
