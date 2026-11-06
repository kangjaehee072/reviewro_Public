package com.reviewro.repository;

import com.reviewro.entity.TermsSectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsSectionRepository extends JpaRepository<TermsSectionEntity, Long> {

    List<TermsSectionEntity> findAllByOrderBySortOrderAscTermsSectionIdAsc();

    List<TermsSectionEntity> findByRequiredOnSignupTrueOrderBySortOrderAscTermsSectionIdAsc();
}
