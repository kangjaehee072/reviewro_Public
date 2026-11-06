package com.reviewro.service;


import com.reviewro.config.AutoMapper;
import com.reviewro.dto.LocationDTO;
import com.reviewro.entity.LocationBookMarkEntity;
import com.reviewro.entity.LocationEntity;
import com.reviewro.entity.MemberEntity;
import com.reviewro.repository.LocationBookMarkRepository;
import com.reviewro.repository.LocationRepository;
import com.reviewro.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationBookMarkService {

    private final LocationBookMarkRepository locationBookMarkRepository;
    private final LocationRepository locationRepository;
    private final MemberRepository memberRepository;
    private final LocationThumbnails locationThumbnails;

    //북마크 추가
    @Transactional
    public void addBookmark (Long locationId, Long memberId) {
        if (locationBookMarkRepository.existsByLocation_LocationIdAndMember_MemberId(locationId,memberId)) {
            throw new IllegalArgumentException("이미 등록된 장소입니다.");
        }

        LocationEntity location =  locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다."));
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        LocationBookMarkEntity bookMark = new LocationBookMarkEntity();
        bookMark.setLocation(location);
        bookMark.setMember(member);
        locationBookMarkRepository.save(bookMark);

    }


    // 2. 북마크 삭제
    @Transactional
    public void removeBookmark(Long locationId, Long memberId) {
        LocationBookMarkEntity bookmark = locationBookMarkRepository.findByLocation_LocationIdAndMember_MemberId(locationId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("북마크 정보를 찾을 수 없습니다."));
        locationBookMarkRepository.delete(bookmark);
    }

    // 3. 북마크 목록 조회
    public List<LocationDTO> getBookmarks(Long memberId) {
        List<LocationBookMarkEntity> bookmarks = locationBookMarkRepository.findBookmarksByMemberId(memberId);
        return locationThumbnails.fill(bookmarks.stream()
                .map(b -> AutoMapper.map(b.getLocation(), LocationDTO.class))
                .toList());
    }



}
