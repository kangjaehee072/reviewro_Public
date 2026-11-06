package com.reviewro.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContentsController {

    @Value("${key.kakaoJavaScript}")
    private String kakaoJavaScriptKey;

    @GetMapping("/contents")
    public String index() {
        return "contents/index";
    }

    @GetMapping("/contents/contentDashboard")
    public String dashboard() {
        return "contents/contentDashboard";
    }

    // 사주팔자 (계산은 전부 프론트)
    @GetMapping("/contents/saju")
    public String saju(Model model) {
        model.addAttribute("kakaoKey", kakaoJavaScriptKey);
        model.addAttribute("pageCss", "saju.css");
        return "contents/saju/saju";
    }

    // AI 추천 — 파트라슈와 대화하며 장소를 좁혀간다
    @GetMapping("/contents/ai")
    public String ai(Model model) {
        model.addAttribute("kakaoKey", kakaoJavaScriptKey);
        model.addAttribute("pageCss", "ai.css");
        return "contents/ai/ai";
    }
}
