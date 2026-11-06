package com.reviewro.controller;

import com.reviewro.service.AdminDashboardService;
import com.reviewro.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor
@Controller
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @Value("${key.kakaoJavaScript}")
    private String kakaoJavaScriptKey;

    @GetMapping("/admin")
    public String admin(Model model){
        model.addAttribute("kpi", adminDashboardService.kpi());
        model.addAttribute("weeklySignups", adminDashboardService.weeklySignups());
        model.addAttribute("topHashTags", adminDashboardService.topHashTags());

        return "admin/adminDashboard";
    }

    @GetMapping("/admin/location")
    public String location(Model model){
        Map<String, String> key = new HashMap<>();
        key.put("kakaoJavaScript", kakaoJavaScriptKey);

        model.addAttribute("key", key);
        model.addAttribute("pageCss", "adminlocation.css");   // 이 페이지 전용 지도 레이아웃
        return "location/adminlocation";
    }


}
