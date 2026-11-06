package com.reviewro.repository;

import com.reviewro.entity.ChatRegion;
import com.reviewro.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    // 지역 안의 톡방 목록 (최근에 만든 순)
    @Query("SELECT r FROM ChatRoomEntity r LEFT JOIN FETCH r.member"
            + " WHERE r.chatRoomRegion = :region ORDER BY r.chatRoomId DESC")
    List<ChatRoomEntity> findByRegion(@Param("region") ChatRegion region);

    // 지역별 톡방 수 [지역, 개수]
    @Query("SELECT r.chatRoomRegion, COUNT(r) FROM ChatRoomEntity r"
            + " WHERE r.chatRoomRegion IS NOT NULL GROUP BY r.chatRoomRegion")
    List<Object[]> countByRegion();

    // 같은 지역에 같은 이름의 방이 있는지 (중복 방지용)
    Optional<ChatRoomEntity> findByChatRoomRegionAndChatRoomName(ChatRegion chatRoomRegion, String chatRoomName);
}
