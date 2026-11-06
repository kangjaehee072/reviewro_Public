package com.reviewro.controller;

import com.reviewro.dto.AdminMemberDTO;
import com.reviewro.entity.MemberRole;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/members")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    // 회원 목록
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal MemberDetails details,
                       Model model) {

        if (notAdmin(details)) {
            return "redirect:/login";
        }

        Page<AdminMemberDTO> members = adminMemberService.list(keyword, page);

        model.addAttribute("members", members);
        model.addAttribute("keyword", keyword);
        model.addAttribute("myId", details.getMember().getMemberId());
        // 화면에서 권한 변경 칸을 보여줄지 정하는 값
        model.addAttribute("isSuper",
                details.getMember().getMemberRole() == MemberRole.SUPERADMIN);
        model.addAttribute("pageCss", "adminmember.css");
        return "admin/members";
    }

    // 회원 탈퇴시키기 (본인 비밀번호 재확인)
    @PostMapping("/{id}/delete")
    public String remove(@PathVariable Long id,
                         @RequestParam String password,
                         @RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "0") int page,
                         @AuthenticationPrincipal MemberDetails details,
                         RedirectAttributes ra) {

        if (notAdmin(details)) {
            return "redirect:/login";
        }

        try {
            adminMemberService.remove(id, details.getMember().getMemberId(), password);
            ra.addFlashAttribute("message", "탈퇴 처리했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return back(ra, keyword, page);
    }

    // 권한 올리기·내리기 (슈퍼어드민 전용)
    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam MemberRole role,
                             @RequestParam String password,
                             @RequestParam(defaultValue = "") String keyword,
                             @RequestParam(defaultValue = "0") int page,
                             @AuthenticationPrincipal MemberDetails details,
                             RedirectAttributes ra) {

        if (notAdmin(details)) {
            return "redirect:/login";
        }

        try {
            adminMemberService.changeRole(id, role, details.getMember().getMemberId(), password);
            ra.addFlashAttribute("message",
                    role == MemberRole.ADMIN ? "관리자로 올렸습니다." : "일반 회원으로 내렸습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return back(ra, keyword, page);
    }

    // 보던 검색어와 페이지를 그대로 유지한다
    private String back(RedirectAttributes ra, String keyword, int page) {
        ra.addAttribute("keyword", keyword);
        ra.addAttribute("page", page);
        return "redirect:/admin/members";
    }

    private boolean notAdmin(MemberDetails details) {
        return details == null || details.getMember().getMemberRole() == MemberRole.USER;
    }
}
