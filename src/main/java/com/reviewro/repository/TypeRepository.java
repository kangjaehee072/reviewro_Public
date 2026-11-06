package com.reviewro.repository;

import com.reviewro.entity.TypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypeRepository extends JpaRepository<TypeEntity, Long> {

    // 활성 타입만, 관리자가 정한 순서대로
    List<TypeEntity> findByTypeActiveTrueOrderByTypeSortOrder();

    // AI 가 보내온 타입명으로 찾기 (AI 는 id 를 모르고 이름만 안다)
    Optional<TypeEntity> findByTypeName(String typeName);
}
