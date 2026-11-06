package com.reviewro.controller;

import com.reviewro.dto.ChatMessageDTO;
import com.reviewro.dto.ChatRoomDTO;
import com.reviewro.entity.ChatRegion;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.ChatRoomService;
import com.reviewro.service.ChatStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

// 지역별 톡방. 지역 고르기 -> 그 지역 톡방 목록 -> 톡방 입장(SSE 로 실시간 수신)
// 목록은 누구나 보고, 방 만들기·입장·말하기는 로그인한 회원만 (SecurityConfig 에서 막는다)
@Controller
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatStreamService chatStreamService;

    // 1. 지역 고르기 -> /chat
    @GetMapping
    public String regions(Model model) {
        model.addAttribute("regions", ChatRegion.values());
        model.addAttribute("roomCounts", chatRoomService.countByRegion());
        model.addAttribute("pageCss", "chat.css");
        return "chat/regions";
    }

    // 2. 지역 안의 톡방 목록 + 방 만들기 -> /chat/region/BUSAN
    @GetMapping("/region/{region}")
    public String regionRooms(@PathVariable String region, Model model) {
        ChatRegion chatRegion = parseRegion(region);
        if (chatRegion == null) {
            return "redirect:/chat";
        }
        model.addAttribute("region", chatRegion);
        model.addAttribute("roomList", chatRoomService.findByRegion(chatRegion));
        model.addAttribute("pageCss", "chat.css");
        return "chat/list";
    }

    // 3. 톡방 만들기 -> 만든 방으로 바로 들어간다
    @PostMapping("/rooms")
    public String createChatRoom(@RequestParam String region,
                                 @RequestParam String chatRoomName,
                                 @AuthenticationPrincipal MemberDetails details,
                                 RedirectAttributes ra) {
        ChatRegion chatRegion = parseRegion(region);
        try {
            Long id = chatRoomService.createChatRoom(details.getMember().getMemberId(), chatRegion, chatRoomName);
            return "redirect:/chat/rooms/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return chatRegion == null ? "redirect:/chat" : "redirect:/chat/region/" + chatRegion.name();
        }
    }

    // 4. 톡방 입장 (지난 대화를 그려 두고, 새 메시지는 SSE 로 받는다)
    @GetMapping("/rooms/{id}")
    public String enterChatRoom(@PathVariable Long id,
                                @AuthenticationPrincipal MemberDetails details,
                                Model model, RedirectAttributes ra) {
        try {
            ChatRoomDTO room = chatRoomService.findChatRoomById(id);
            model.addAttribute("room", room);
            model.addAttribute("messageList", chatRoomService.getChatMessages(id));
            model.addAttribute("myId", details.getMember().getMemberId());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/chat";
        }
        model.addAttribute("pageCss", "chat.css");
        return "chat/room";
    }

    // 5. 실시간 수신 연결 (EventSource)
    @GetMapping(value = "/rooms/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(@PathVariable Long id, @AuthenticationPrincipal MemberDetails details) {
        try {
            chatRoomService.findChatRoomById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return chatStreamService.subscribe(ChatStreamService.roomChannel(id), details.getMember().getMemberId());
    }

    // 6. 메시지 보내기 (fetch). 저장이 끝난 뒤 방에 뿌린다
    @PostMapping("/rooms/{id}/messages")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@PathVariable Long id,
                                         @RequestBody ChatMessageDTO request,
                                         @AuthenticationPrincipal MemberDetails details) {
        try {
            ChatMessageDTO saved = chatRoomService.saveMessage(
                    id, details.getMember().getMemberId(), request.getChatRoomContent());
            chatStreamService.broadcastMessage(ChatStreamService.roomChannel(id), saved);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // 7. 실시간 연결 끊기 (방 나가기 버튼, 페이지 떠날 때 sendBeacon). 톡방·그룹 채팅 공용
    @PostMapping("/streams/{connId}/close")
    @ResponseBody
    public ResponseEntity<Void> closeStream(@PathVariable String connId,
                                            @AuthenticationPrincipal MemberDetails details) {
        chatStreamService.close(connId, details.getMember().getMemberId());
        return ResponseEntity.noContent().build();
    }

    private ChatRegion parseRegion(String region) {
        try {
            return ChatRegion.valueOf(region.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
