package com.reviewro.repository;

import com.reviewro.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // 특정 채팅방의 최근 메시지 100개 (최신순. 화면에 그릴 땐 뒤집는다)
    List<ChatMessageEntity> findTop100ByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(Long chatRoomId);

    // 방을 지우기 전에 그 방의 대화를 한 번에 지운다 (메시지가 방을 FK 로 걸고 있어 방보다 먼저 지워야 한다)
    @Modifying
    @Query("DELETE FROM ChatMessageEntity m WHERE m.chatRoom.chatRoomId = :roomId")
    void deleteByChatRoomId(@Param("roomId") Long roomId);
}
