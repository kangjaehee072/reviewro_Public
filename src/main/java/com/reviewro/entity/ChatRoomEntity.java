package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 지역별 목록·지역별 개수·같은 이름 확인이 모두 지역으로 거른다. 같은 지역 안 이름 중복도 DB 에서 막는다
@Table(name = "chat_room",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_room_region_name",
                columnNames = {"chat_room_region", "chat_room_name"}))
public class ChatRoomEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;              // PK (long)

    // 예전 톡방에는 지역이 없어 비워 둘 수 있게 한다 (지역이 없는 방은 목록에 나오지 않는다)
    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "varchar(20)")
    private ChatRegion chatRoomRegion;    // 지역 (예: BUSAN)

    // 예전 컬럼이 NOT NULL 이라 남겨 둔다. 새 방은 지역 이름을 넣는다
    @Column(nullable = false, length = 100)
    private String chatRoomPlace;         // 장소명 (예: 부산)

    @Column(nullable = false, length = 100)
    private String chatRoomName;          // 채팅방 이름 (예: 해운대 야경 투어 톡방)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;          // 방을 만든 회원
}
