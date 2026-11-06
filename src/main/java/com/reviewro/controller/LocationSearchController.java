package com.reviewro.controller;

import com.reviewro.dto.LocationSearchDTO;
import com.reviewro.dto.LocationSearchResultDTO;
import com.reviewro.service.LocationSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationSearchController {
    private final LocationSearchService locationSearchService;

    @GetMapping("/api/locations/search")
    public LocationSearchResultDTO search(@ModelAttribute LocationSearchDTO locationSearchDTO) {
        return locationSearchService.search(locationSearchDTO);
    }
}
