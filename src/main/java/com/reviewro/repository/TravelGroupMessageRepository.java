package com.reviewro.repository;

import com.reviewro.entity.TravelGroupMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelGroupMessageRepository extends JpaRepository<TravelGroupMessageEntity, Long> {

    // 최근 메시지 100개 (최신순. 화면에 그릴 땐 뒤집는다)
    List<TravelGroupMessageEntity> findTop100ByGroup_GroupIdOrderByGroupMessageIdDesc(Long groupId);

    void deleteByGroup_GroupId(Long groupId);

}
