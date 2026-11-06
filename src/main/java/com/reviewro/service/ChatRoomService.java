package com.reviewro.service;

import com.reviewro.dto.ChatMessageDTO;
import com.reviewro.dto.ChatRoomDTO;
import com.reviewro.entity.ChatMessageEntity;
import com.reviewro.entity.ChatRegion;
import com.reviewro.entity.ChatRoomEntity;
import com.reviewro.entity.MemberEntity;
import com.reviewro.repository.ChatMessageRepository;
import com.reviewro.repository.ChatRoomRepository;
import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class ChatRoomService {

    private static final int NAME_MAX = 50;
    private static final int CONTENT_MAX = 500;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatStreamService chatStreamService;

    // 1. 지역별 톡방 수 (방이 없는 지역은 0)
    public Map<ChatRegion, Long> countByRegion() {
        Map<ChatRegion, Long> counts = new EnumMap<>(ChatRegion.class);
        for (ChatRegion region : ChatRegion.values()) {
            counts.put(region, 0L);
        }
        for (Object[] row : chatRoomRepository.countByRegion()) {
            counts.put((ChatRegion) row[0], (Long) row[1]);
        }
        return counts;
    }

    // 2. 지역 안의 톡방 목록
    public List<ChatRoomDTO> findByRegion(ChatRegion region) {
        return chatRoomRepository.findByRegion(region).stream()
                .map(this::toDto)
                .toList();
    }

    // 3. 새 톡방 만들기 (같은 지역에 같은 이름은 안 된다)
    @Transactional
    public Long createChatRoom(Long memberId, ChatRegion region, String name) {
        if (region == null) {
            throw new IllegalArgumentException("지역을 골라 주세요.");
        }
        String roomName = name == null ? "" : name.trim();
        if (roomName.isEmpty()) {
            throw new IllegalArgumentException("톡방 이름을 입력해 주세요.");
        }
        if (roomName.length() > NAME_MAX) {
            throw new IllegalArgumentException("톡방 이름은 " + NAME_MAX + "자 이내로 입력해 주세요.");
        }
        chatRoomRepository.findByChatRoomRegionAndChatRoomName(region, roomName).ifPresent(room -> {
            throw new IllegalArgumentException("이 지역에 같은 이름의 톡방이 이미 있습니다.");
        });

        ChatRoomEntity room = ChatRoomEntity.builder()
                .chatRoomRegion(region)
                .chatRoomPlace(region.getLabel())
                .chatRoomName(roomName)
                .member(getMember(memberId))
                .build();
        return chatRoomRepository.save(room).getChatRoomId();
    }

    // 4. 톡방 단건 조회 (지역이 없는 예전 방은 들어갈 수 없다)
    public ChatRoomDTO findChatRoomById(Long chatRoomId) {
        ChatRoomEntity room = chatRoomRepository.findById(chatRoomId)
                .filter(found -> found.getChatRoomRegion() != null)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 톡방입니다."));
        return toDto(room);
    }

    // 5. 메시지 저장 (방에 뿌리는 건 커밋이 끝난 뒤 컨트롤러가 한다)
    @Transactional
    public ChatMessageDTO saveMessage(Long chatRoomId, Long memberId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("메시지를 입력해 주세요.");
        }
        if (text.length() > CONTENT_MAX) {
            throw new IllegalArgumentException("메시지는 " + CONTENT_MAX + "자 이내로 입력해 주세요.");
        }
        ChatRoomEntity room = chatRoomRepository.findById(chatRoomId)
                .filter(found -> found.getChatRoomRegion() != null)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 톡방입니다."));
        MemberEntity member = getMember(memberId);

        ChatMessageEntity saved = chatMessageRepository.save(ChatMessageEntity.builder()
                .chatRoom(room)
                .member(member)
                .chatRoomMember(member.getMemberNickname())
                .chatRoomContent(text)
                .build());

        return toDto(saved);
    }

    // 6. 지난 대화 (최근 100개, 시간순)
    public List<ChatMessageDTO> getChatMessages(Long chatRoomId) {
        List<ChatMessageDTO> messages = new ArrayList<>(
                chatMessageRepository.findTop100ByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(chatRoomId).stream()
                        .map(this::toDto)
                        .toList());
        Collections.reverse(messages);
        return messages;
    }

    // 톡방에서 마지막 한 명이 나가 방이 비면(ChatStreamService 가 알림) 방과 대화를 지운다.
    // 접속이 끊긴 뒤에 불리므로 읽기 전용 클래스지만 이 메서드만 쓰기로 연다
    @Transactional
    @EventListener
    public void onRoomEmptied(ChatStreamService.ChatRoomEmptiedEvent event) {
        if (!chatRoomRepository.existsById(event.roomId())) {
            return;
        }
        chatMessageRepository.deleteByChatRoomId(event.roomId());
        chatRoomRepository.deleteById(event.roomId());
    }

    private MemberEntity getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private ChatRoomDTO toDto(ChatRoomEntity room) {
        return ChatRoomDTO.builder()
                .chatRoomId(room.getChatRoomId())
                .chatRoomRegion(room.getChatRoomRegion())
                .chatRoomName(room.getChatRoomName())
                .creatorNickname(room.getMember() == null ? null : room.getMember().getMemberNickname())
                .onlineCount(chatStreamService.onlineCount(ChatStreamService.roomChannel(room.getChatRoomId())))
                .createdAt(room.getCreatedAt())
                .build();
    }

    private ChatMessageDTO toDto(ChatMessageEntity msg) {
        return ChatMessageDTO.builder()
                .chatMessageId(msg.getChatMessageId())
                .chatRoomId(msg.getChatRoom().getChatRoomId())
                .memberId(msg.getMember() == null ? null : msg.getMember().getMemberId())
                .chatRoomMember(msg.getChatRoomMember())
                .chatRoomContent(msg.getChatRoomContent())
                .chatRoomSentAt(msg.getChatRoomSentAt())
                .build();
    }
}
