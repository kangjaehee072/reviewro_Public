package com.reviewro.repository;

import com.reviewro.entity.CommentEntity;
import com.reviewro.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    // 이 글의 댓글을 번호순으로. 작성자를 같이 가져온다
    @Query("select c from CommentEntity c " +
            "  left join fetch c.member " +
            " where c.targetType = :type and c.targetId = :id " +
            " order by c.commentId asc")
    List<CommentEntity> findForTarget(@Param("type") TargetType type,
                                      @Param("id") Long id);

    // 이 댓글에 달린 답글이 남아 있는지
    boolean existsByParent_CommentId(Long parentId);

    // 글이 삭제될 때 그 글의 댓글을 한 번에 지운다 (A-3)
    @Modifying
    @Query("delete from CommentEntity c " +
            " where c.targetType = :type and c.targetId = :id")
    void deleteByTarget(@Param("type") TargetType type, @Param("id") Long id);
}
