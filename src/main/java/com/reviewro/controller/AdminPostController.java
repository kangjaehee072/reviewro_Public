package com.reviewro.controller;

import com.reviewro.dto.AdminPostDTO;
import com.reviewro.entity.MemberRole;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.AdminPostService;
import com.reviewro.service.PostImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/admin/post")
public class AdminPostController {

    private final AdminPostService adminPostService;
    private final PostImageService postImageService;

    // 글 작성 페이지 이동
    @GetMapping("/insert")
    public String adminPostInsertForm(@AuthenticationPrincipal MemberDetails details,
                                      Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        if (details.getMember().getMemberRole() == MemberRole.USER) {
            return "redirect:/admin/post/list";
        }
        model.addAttribute("adminPostDTO", new AdminPostDTO());
        return "admin/post/insert";
    }

    // 글 작성 처리
    @PostMapping("/insert")
    public String adminPostInsert(@ModelAttribute AdminPostDTO adminPostDto,
                                  @RequestParam(value = "images", required = false)
                                  List<MultipartFile> images,
                                  @AuthenticationPrincipal MemberDetails details,
                                  Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        if (details.getMember().getMemberRole() == MemberRole.USER) {
            return "redirect:/admin/post/list";
        }
        try {
            Long id = adminPostService.insert(details.getMember().getMemberId(),adminPostDto);
            postImageService.save(TargetType.ADMIN_POST, id, images);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("adminPostDTO", adminPostDto);
            return "admin/post/insert";
        }
        return "redirect:/admin/post/list";
    }

    // 목록 조회
    @GetMapping("/list")
    public String adminPostList(Model model,
                                @RequestParam(defaultValue = "0") Integer page) {
        Page<AdminPostDTO> postList = adminPostService.notices(page);
        model.addAttribute("postList", postList);
        return "admin/post/list";
    }

    // 상세 조회
    @GetMapping("/read/{id}")
    public String adminPostRead(@PathVariable(name = "id") Long id,
                                @AuthenticationPrincipal MemberDetails details,
                                Model model, RedirectAttributes ra) {
        try {
            AdminPostDTO postDto = adminPostService.read(id);
            model.addAttribute("postDto", postDto);
            model.addAttribute("images",
                    postImageService.paths(TargetType.ADMIN_POST, id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/post/list";
        }
        if (details != null) {
            model.addAttribute("loginMemberId", details.getMember().getMemberId());
            model.addAttribute("role",details.getMember().getMemberRole());
        }
        return "admin/post/read";
    }

    // 수정 페이지 이동
    @GetMapping("/update/{id}")
    public String adminPostUpdateForm(@PathVariable(name = "id") Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("adminPostDTO", adminPostService.read(id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/post/list";
        }
        return "admin/post/update";
    }

    // 수정 처리
    @PostMapping("/update")
    public String adminPostUpdate(@ModelAttribute AdminPostDTO adminPostDto,
                                  @AuthenticationPrincipal MemberDetails details,
                                  Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        if (details.getMember().getMemberRole() == MemberRole.USER) {
            return "redirect:/admin/post/read";
        }
        try {
            adminPostService.update(details.getMember().getMemberId(),adminPostDto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/post/list";
        }
        ra.addFlashAttribute("successMessage", "게시글을 수정했습니다.");
        return "redirect:/admin/post/read/" + adminPostDto.getAdminPostId();
    }

    // 삭제 처리
    @PostMapping("/delete")
    public String adminPostDelete(@RequestParam Long adminPostId,
                                  @AuthenticationPrincipal MemberDetails details,
                                  RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        if (details.getMember().getMemberRole() == MemberRole.USER) {
            return "redirect:/admin/post/read";
        }
        try {
            adminPostService.delete(adminPostId, details.getMember().getMemberId());
            postImageService.deleteAll(TargetType.ADMIN_POST, adminPostId);
            ra.addFlashAttribute("successMessage", "게시글을 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/post/list";
    }

    @PostMapping("/pin/{id}")
    public String pin(@PathVariable Long id,
                      @AuthenticationPrincipal MemberDetails details) {
        // 관리자가 아니면 막는다. 화면에서 버튼을 숨기는 것만으로는 못 막는다
        if (details == null || !isAdmin(details)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        adminPostService.togglePin(id);
        return "redirect:/admin/post/list";
    }

    private boolean isAdmin(MemberDetails details) {
        // 사용자의 권한(Role)이 ADMIN인지 확인하는 로직
        return details.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")
                        || auth.getAuthority().equals("ROLE_SUPERADMIN"));
    }



}