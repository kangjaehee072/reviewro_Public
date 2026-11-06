package com.reviewro.repository;

import com.reviewro.entity.PostImageEntity;
import com.reviewro.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository  extends JpaRepository<PostImageEntity, Long> {


    List<PostImageEntity> findByTargetTypeAndTargetIdOrderByPostImageIdAsc(
            TargetType type, Long targetId);

    @Modifying
    @Query("delete from PostImageEntity p " +
            " where p.targetType = :type and p.targetId = :id")
    void deleteByTarget(@Param("type") TargetType type, @Param("id") Long id);

    // 목록 화면용. 글 여러 개의 사진을 번호순으로 한 번에.
    // GROUP BY 로 한 장만 고르지 않고 통째로 받아서 서비스에서 포문으로 고른다.
    // 한 쪽에 글 10개면 사진은 많아야 수십 개라 이게 더 간단하고 읽기 쉽다
    @Query("SELECT p FROM PostImageEntity p "
            + " WHERE p.targetType = :type AND p.targetId IN :ids "
            + " ORDER BY p.postImageId ASC")
    List<PostImageEntity> findForPosts(@Param("type") TargetType type,
                                       @Param("ids") List<Long> ids);
}

