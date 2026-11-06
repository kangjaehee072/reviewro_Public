package com.reviewro.controller;


import com.reviewro.dto.LocationDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.LocationBookMarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationBookMarkController {

    private final LocationBookMarkService locationBookMarkService;

    @GetMapping ("/api/bookmarks")
    public ResponseEntity<List<LocationDTO>> getBookmarks(@AuthenticationPrincipal MemberDetails details) {
        List<LocationDTO> bookmarks = locationBookMarkService.getBookmarks(details.getMember().getMemberId());
        return ResponseEntity.ok(bookmarks);
    }

    // 북마크 추가
    @PostMapping("/api/bookmarks/{locationId}")
    public ResponseEntity<Void> addBookmark(@PathVariable Long locationId, @AuthenticationPrincipal MemberDetails details) {
        locationBookMarkService.addBookmark(locationId, details.getMember().getMemberId());
        return ResponseEntity.ok().build();
    }

    // 북마크 삭제
    @DeleteMapping("/api/bookmarks/{locationId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long locationId, @AuthenticationPrincipal MemberDetails details) {
        locationBookMarkService.removeBookmark(locationId,details.getMember().getMemberId() );
        return ResponseEntity.ok().build();
    }


}
