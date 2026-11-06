package com.reviewro.controller;

import com.reviewro.ai.AiClient;
import com.reviewro.ai.AiException;
import com.reviewro.ai.AiMessageDTO;
import com.reviewro.ai.AiProvider;
import com.reviewro.ai.AiSettingService;
import com.reviewro.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// AI 모델 서버 고르기 (/admin/ai, SecurityConfig 에서 SUPERADMIN 만)
@Controller
@RequiredArgsConstructor
public class AdminAiController {

    private final AiSettingService aiSettingService;

    @GetMapping("/admin/ai")
    public String form(Model model) {
        model.addAttribute("setting", aiSettingService.current());
        model.addAttribute("claudeModels", AiSettingService.CLAUDE_MODELS);
        model.addAttribute("hasClaudeKey", aiSettingService.hasClaudeKey());
        model.addAttribute("ollamaModel", aiSettingService.ollamaModel());
        return "admin/ai";
    }

    @PostMapping("/admin/ai")
    public String save(@RequestParam AiProvider provider,
                       @RequestParam String claudeModel,
                       @AuthenticationPrincipal MemberDetails details,
                       RedirectAttributes ra) {
        try {
            aiSettingService.save(provider, claudeModel, details.getMember().getMemberUserId());
            ra.addFlashAttribute("successMessage",
                    (provider == AiProvider.CLAUDE ? "Claude (" + claudeModel + ")" : "Ollama")
                            + " 로 바꿨어요. 다음 질문부터 바로 적용돼요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/ai";
    }

    // 저장하지 않고, 고른 모델 서버에 짧은 질문을 한 번 보내 본다
    @PostMapping("/admin/ai/test")
    public String test(@RequestParam AiProvider provider,
                       @RequestParam String claudeModel,
                       RedirectAttributes ra) {
        AiClient client = aiSettingService.clientFor(provider, claudeModel);
        StringBuilder answer = new StringBuilder();
        long started = System.currentTimeMillis();
        try {
            client.stream("한 문장으로만 짧게 답한다.",
                    List.of(new AiMessageDTO("user", "안녕하세요. 연결 확인 중이에요.")),
                    answer::append);
            long ms = System.currentTimeMillis() - started;
            ra.addFlashAttribute("testResult", "응답 " + String.format("%.1f", ms / 1000.0) + "초 · " + answer.toString().trim());
        } catch (AiException e) {
            ra.addFlashAttribute("errorMessage", "연결 실패: " + e.getMessage());
        }
        ra.addFlashAttribute("testProvider", provider);
        ra.addFlashAttribute("testModel", claudeModel);
        return "redirect:/admin/ai";
    }
}
