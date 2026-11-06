package com.reviewro.repository;

import com.reviewro.entity.PhotoMapEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoMapRepository extends JpaRepository<PhotoMapEntity, Long> {

    @EntityGraph(attributePaths = "photoMapImages")
    List<PhotoMapEntity> findByMember_MemberIdOrderByPhotoMapIdDesc(Long memberId);

    @EntityGraph(attributePaths = "photoMapImages")
    Optional<PhotoMapEntity> findByPhotoMapIdAndMember_MemberId(Long id, Long memberId);
}
