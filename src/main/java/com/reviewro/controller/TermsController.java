package com.reviewro.controller;

import com.reviewro.entity.SiteInfoEntity;
import com.reviewro.service.TermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 이용약관·개인정보 처리방침 (누구나 보기) + 슈퍼어드민 수정 화면 (/admin/terms, SecurityConfig 에서 SUPERADMIN 만)
@Controller
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("sections", termsService.sections());
        model.addAttribute("siteInfo", termsService.siteInfo());
        return "terms/index";
    }

    // 푸터 '약관보기' 모달에 끼워 넣을 본문만 (js/terms-modal.js 가 불러간다)
    @GetMapping("/terms/modal")
    public String termsModal(Model model) {
        model.addAttribute("sections", termsService.sections());
        model.addAttribute("siteInfo", termsService.siteInfo());
        return "terms/modal";
    }

    @GetMapping("/admin/terms")
    public String edit(Model model) {
        model.addAttribute("sections", termsService.sections());
        model.addAttribute("siteInfo", termsService.siteInfo());
        return "admin/terms";
    }

    // 항목 저장 (id 가 없으면 새로 추가)
    @PostMapping("/admin/terms/sections")
    public String saveSection(@RequestParam(required = false) Long id,
                              @RequestParam String title,
                              @RequestParam String content,
                              @RequestParam(defaultValue = "0") int sortOrder,
                              @RequestParam(defaultValue = "false") boolean requiredOnSignup,
                              RedirectAttributes ra) {
        try {
            termsService.saveSection(id, title, content, sortOrder, requiredOnSignup);
            ra.addFlashAttribute("successMessage", "'" + title.trim() + "' 을(를) 저장했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/terms";
    }

    @PostMapping("/admin/terms/sections/{id}/delete")
    public String deleteSection(@PathVariable Long id, RedirectAttributes ra) {
        termsService.deleteSection(id);
        ra.addFlashAttribute("successMessage", "항목을 지웠어요.");
        return "redirect:/admin/terms";
    }

    @PostMapping("/admin/terms/info")
    public String saveInfo(@ModelAttribute SiteInfoEntity form, RedirectAttributes ra) {
        termsService.saveSiteInfo(form);
        ra.addFlashAttribute("successMessage", "운영 정보를 저장했어요.");
        return "redirect:/admin/terms";
    }
}
