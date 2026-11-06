package com.reviewro.controller;

import com.reviewro.dto.ChatMessageDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.ChatStreamService;
import com.reviewro.service.TravelGroupService;
import lombok.RequiredArgsConstructor;
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

// 여행 그룹. 전부 로그인한 회원만 (SecurityConfig 에서 /groups/** 를 막는다)
@Controller
@RequiredArgsConstructor
@RequestMapping("/groups")
public class TravelGroupController {

    private final TravelGroupService groupService;
    private final ChatStreamService chatStreamService;

    // 1. 내 그룹 + 받은 초대 + 그룹 만들기 -> /groups
    @GetMapping
    public String list(@AuthenticationPrincipal MemberDetails details, Model model) {
        Long me = details.getMember().getMemberId();
        model.addAttribute("groups", groupService.myGroups(me));
        model.addAttribute("invitations", groupService.myInvitations(me));
        model.addAttribute("pageCss", "chat.css");
        return "group/list";
    }

    @PostMapping
    public String create(@RequestParam String groupName,
                         @AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            Long id = groupService.create(details.getMember().getMemberId(), groupName);
            return "redirect:/groups/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups";
        }
    }

    // 2. 그룹 화면: 왼쪽 구성원·공유 계획, 오른쪽 그룹 채팅
    @GetMapping("/{id}")
    public String read(@PathVariable Long id, @AuthenticationPrincipal MemberDetails details,
                       Model model, RedirectAttributes ra) {
        Long me = details.getMember().getMemberId();
        try {
            model.addAttribute("group", groupService.read(id, me));
            model.addAttribute("members", groupService.members(id, me));
            model.addAttribute("plans", groupService.sharedPlans(id, me));
            model.addAttribute("myPlans", groupService.shareablePlans(id, me));
            model.addAttribute("messageList", groupService.messages(id, me));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups";
        }
        model.addAttribute("myId", me);
        model.addAttribute("onlineCount", chatStreamService.onlineCount(ChatStreamService.groupChannel(id)));
        model.addAttribute("pageCss", "chat.css");
        return "group/read";
    }

    // 3. 공유된 여행계획 보기 (오른쪽 채팅은 그대로)
    @GetMapping("/{id}/plans/{planId}")
    public String readPlan(@PathVariable Long id, @PathVariable Long planId,
                           @AuthenticationPrincipal MemberDetails details,
                           Model model, RedirectAttributes ra) {
        Long me = details.getMember().getMemberId();
        try {
            model.addAttribute("group", groupService.read(id, me));
            model.addAttribute("plan", groupService.readSharedPlan(id, me, planId));
            model.addAttribute("messageList", groupService.messages(id, me));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups/" + id;
        }
        model.addAttribute("myId", me);
        model.addAttribute("onlineCount", chatStreamService.onlineCount(ChatStreamService.groupChannel(id)));
        model.addAttribute("pageCss", "chat.css");
        return "group/plan";
    }

    // 4. 초대코드로 참여 (그룹 목록 화면의 입력칸)
    @PostMapping("/join")
    public String joinByCode(@RequestParam String code,
                             @AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            Long groupId = groupService.joinByCode(details.getMember().getMemberId(), code);
            refresh(groupId);
            return "redirect:/groups/" + groupId;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups";
        }
    }

    // 5. 받은 초대 수락·거절 (그룹 목록 화면)
    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id,
                         @AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            groupService.accept(id, details.getMember().getMemberId());
            refresh(id);
            return "redirect:/groups/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups";
        }
    }

    @PostMapping("/{id}/decline")
    public String decline(@PathVariable Long id,
                          @AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            groupService.decline(id, details.getMember().getMemberId());
            refresh(id);
            ra.addFlashAttribute("successMessage", "초대를 거절했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/groups";
    }

    // 6. 나가기·삭제 (그룹 화면을 떠나므로 페이지 이동)
    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id,
                        @AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            groupService.leave(id, details.getMember().getMemberId());
            // 나간 사람의 열린 연결로 그룹 채팅이 계속 가지 않게 끊는다
            chatStreamService.disconnect(ChatStreamService.groupChannel(id), details.getMember().getMemberId());
            refresh(id);
            ra.addFlashAttribute("successMessage", "그룹에서 나왔어요.");
            return "redirect:/groups";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            groupService.delete(id, details.getMember().getMemberId());
            // 보고 있던 구성원들은 그룹 목록으로 보낸다
            chatStreamService.broadcastEvent(ChatStreamService.groupChannel(id), "deleted");
            chatStreamService.disconnect(ChatStreamService.groupChannel(id), null);
            ra.addFlashAttribute("successMessage", "그룹을 삭제했어요.");
            return "redirect:/groups";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/groups/" + id;
        }
    }

    // 7. 그룹 화면 안의 동작은 새로고침 없이 fetch 로 한다 (JSON {message}).
    //    바뀌면 그룹 채널에 refresh 를 보내서 보고 있는 구성원 화면도 같이 다시 그린다
    @PostMapping("/{id}/invite")
    @ResponseBody
    public ResponseEntity<Map<String, String>> invite(@PathVariable Long id, @RequestParam String nickname,
                                                      @AuthenticationPrincipal MemberDetails details) {
        return act(id, nickname.trim() + " 님을 초대했어요.",
                () -> groupService.invite(id, details.getMember().getMemberId(), nickname));
    }

    @PostMapping("/{id}/plans")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sharePlan(@PathVariable Long id, @RequestParam Long planId,
                                                         @AuthenticationPrincipal MemberDetails details) {
        return act(id, "여행계획을 공유했어요.",
                () -> groupService.sharePlan(id, details.getMember().getMemberId(), planId));
    }

    @PostMapping("/{id}/plans/{planId}/unshare")
    @ResponseBody
    public ResponseEntity<Map<String, String>> unsharePlan(@PathVariable Long id, @PathVariable Long planId,
                                                           @AuthenticationPrincipal MemberDetails details) {
        return act(id, "공유를 내렸어요.",
                () -> groupService.unsharePlan(id, details.getMember().getMemberId(), planId));
    }

    @PostMapping("/{id}/invite-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> inviteCode(@PathVariable Long id,
                                                          @AuthenticationPrincipal MemberDetails details) {
        return act(id, "초대코드를 만들었어요.",
                () -> groupService.generateInviteCode(id, details.getMember().getMemberId()));
    }

    // 초대할 회원 찾기 (입력칸 위에 뜨는 목록)
    @GetMapping("/{id}/invitees")
    @ResponseBody
    public ResponseEntity<?> invitees(@PathVariable Long id, @RequestParam(defaultValue = "") String q,
                                      @AuthenticationPrincipal MemberDetails details) {
        try {
            return ResponseEntity.ok(groupService.searchInvitees(id, details.getMember().getMemberId(), q));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }


    // 6. 그룹 채팅 (톡방과 같은 SSE 방식)
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(@PathVariable Long id, @AuthenticationPrincipal MemberDetails details) {
        Long me = details.getMember().getMemberId();
        try {
            groupService.checkJoined(id, me);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return chatStreamService.subscribe(ChatStreamService.groupChannel(id), me);
    }

    @PostMapping("/{id}/messages")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@PathVariable Long id, @RequestBody ChatMessageDTO request,
                                         @AuthenticationPrincipal MemberDetails details) {
        try {
            ChatMessageDTO saved = groupService.saveMessage(
                    id, details.getMember().getMemberId(), request.getChatRoomContent());
            chatStreamService.broadcastMessage(ChatStreamService.groupChannel(id), saved);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // 성공하면 {message} + 구성원 화면 갱신 알림, 실패하면 400 {message}
    private ResponseEntity<Map<String, String>> act(Long groupId, String okMessage, Runnable action) {
        try {
            action.run();
            refresh(groupId);
            return ResponseEntity.ok(Map.of("message", okMessage));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private void refresh(Long groupId) {
        chatStreamService.broadcastEvent(ChatStreamService.groupChannel(groupId), "refresh");
    }
}
