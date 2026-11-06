package com.reviewro.controller;

import com.reviewro.dto.LocationDTO;
import com.reviewro.dto.SajuTextDTO;
import com.reviewro.service.SajuPlaceService;
import com.reviewro.service.SajuTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SajuTextController {

    private final SajuTextService sajuTextService;
    private final SajuPlaceService sajuPlaceService;

    @GetMapping("/api/saju/texts")
    public List<SajuTextDTO> texts() {
        return sajuTextService.findAll();
    }

    // 부족한 오행에 어울리는 장소 (AI 없이 DB 에서 바로 고른다)
    @GetMapping("/api/saju/places")
    public List<LocationDTO> places(@RequestParam String element) {
        return sajuPlaceService.findForElement(element);
    }
}
