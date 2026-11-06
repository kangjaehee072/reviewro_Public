package com.reviewro.controller;

import com.reviewro.dto.BalanceGameDTO;
import com.reviewro.dto.ChallengeDTO;

import com.reviewro.entity.AdminPostEntity;
import com.reviewro.entity.CompanionPostEntity;
import com.reviewro.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    @Value("${key.kakaoJavaScript}")
    private String kakaoJavaScriptKey;
    private final ChallengeService challengeService;
    private final BalanceGameService balanceGameService;
    private final AdminPostService adminPostService;
    private  final FreeBoardService freeBoardService;
    private  final ReviewService reviewService;
    private  final CompanionService companionService;
    private final TripPlanService tripPlanService;

    @GetMapping("/")
    public String index(Model model) {
        Map<String, String> key = new HashMap<>();
        key.put("kakaoJavaScript", kakaoJavaScriptKey);

        ChallengeDTO randomChallenge = challengeService.getRandomChallenge();
        model.addAttribute("randomChallenge", randomChallenge);

        BalanceGameDTO randomBalance = balanceGameService.getRandomBalance();
        model.addAttribute("randomBalance", randomBalance);

        AdminPostEntity latestNotice = adminPostService.getLatestNotice();
        model.addAttribute("latestNotice", latestNotice);

        model.addAttribute("topFreeBoard", freeBoardService.getTopViewedReview());
        model.addAttribute("topReview", reviewService.getTopBestReview());
        model.addAttribute("topCompanion", companionService.getTopUpcomingCompanion());
        model.addAttribute("topTemplate", tripPlanService.getTopSharedTemplate());
        model.addAttribute("key", key);
        return "index";
    }

    @GetMapping("community/dashboard")
    public String communityDashboard() {
        return "community/index";
    }
}
