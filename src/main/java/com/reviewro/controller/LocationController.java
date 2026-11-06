package com.reviewro.controller;

import com.reviewro.dto.LocationDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RequiredArgsConstructor
@Controller
public class LocationController {
    @Value("${key.kakaoJavaScript}")
    private String kakaoJavaScriptKey;
    private final LocationService locationService;

    @GetMapping("/location")
    public String location(Model model){
        Map<String, String> key = new HashMap<>();
        key.put("kakaoJavaScript", kakaoJavaScriptKey);

        model.addAttribute("key", key);
        model.addAttribute("pageCss", "location.css");   // 이 페이지 전용 레이아웃 CSS
        return "location/location";
    }

    @GetMapping("/api/locations")
    @ResponseBody
    public List<LocationDTO> list() { return locationService.locationslist(); }

    @PostMapping("/api/locations")
    @ResponseBody
    public ResponseEntity<Long> create(@RequestBody LocationDTO dto, @AuthenticationPrincipal MemberDetails details) {

        if(details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(locationService.locationinsert(details.getMember().getMemberId(),dto));
    }
    //JSON API
    @GetMapping ("/api/locations/{id}")
    @ResponseBody
    public LocationDTO locationread(@PathVariable("id") Long id){
        return locationService.locationread(id);
    }

    @PatchMapping("/api/locations/{id}")
    @ResponseBody
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody LocationDTO dto,
                                       @AuthenticationPrincipal MemberDetails details){
        if(details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        dto.setLocationId(id);
        locationService.locationupdate(dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping ("/api/locations/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal MemberDetails details){
        if (details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        locationService.locationdelete(id);
        return ResponseEntity.ok().build();
    }

}
