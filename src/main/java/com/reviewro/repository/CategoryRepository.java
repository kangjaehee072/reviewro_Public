package com.reviewro.repository;

import com.reviewro.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    // 활성 카테고리만, 관리자가 정한 순서대로
    List<CategoryEntity> findByCategoryActiveTrueOrderByCategorySortOrder();

    // AI 가 보내온 카테고리명들로 찾기
    List<CategoryEntity> findByCategoryNameIn(List<String> categoryNames);
}
