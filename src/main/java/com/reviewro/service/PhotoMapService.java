package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.config.ImageFiles;
import com.reviewro.dto.PhotoMapDTO;
import com.reviewro.dto.PhotoMapImageDTO;
import com.reviewro.dto.PhotoMapStickerDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.PhotoMapEntity;
import com.reviewro.entity.PhotoMapImageEntity;
import com.reviewro.entity.PhotoMapStickerEntity;
import com.reviewro.entity.PhotoMapStickerType;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.PhotoMapRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoMapService {

    @Value("${app.home}")
    private String appHome;

    @Value("${file.photomap.dir}")
    private Path photomapDir;

    private final PhotoMapRepository photoMapRepository;
    private final MemberRepository memberRepository;

    // 내 포토맵 목록 (최신순)
    public List<PhotoMapDTO> findMine(Long memberId) {
        return AutoMapper.mapList(photoMapRepository.findByMember_MemberIdOrderByPhotoMapIdDesc(memberId), PhotoMapDTO.class);
    }

    // 포토맵 상세
    public PhotoMapDTO findOne(Long photoMapId, Long memberId) {
        PhotoMapEntity photoMap = getOwned(photoMapId, memberId);
        Hibernate.initialize(photoMap.getPhotoMapStickers());
        return AutoMapper.map(photoMap, PhotoMapDTO.class);
    }

    // 새 포토맵 저장 (새 id 반환)
    @Transactional
    public Long create(Long memberId, PhotoMapDTO dto) {
        validateTitle(dto.getPhotoMapTitle());
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        PhotoMapEntity photoMap = new PhotoMapEntity();
        photoMap.setMember(member);
        photoMap.setPhotoMapTitle(dto.getPhotoMapTitle().trim());
        photoMap.setPhotoMapDescription(dto.getPhotoMapDescription());
        applyImages(photoMap, dto.getPhotoMapImages());
        applyStickers(photoMap, dto.getPhotoMapStickers());

        return photoMapRepository.save(photoMap).getPhotoMapId();
    }

    // 포토맵 수정 (더티 체킹으로 반영되므로 save 호출 없음)
    @Transactional
    public void update(Long photoMapId, Long memberId, PhotoMapDTO dto) {
        validateTitle(dto.getPhotoMapTitle());
        PhotoMapEntity photoMap = getOwned(photoMapId, memberId);
        photoMap.setPhotoMapTitle(dto.getPhotoMapTitle().trim());
        photoMap.setPhotoMapDescription(dto.getPhotoMapDescription());
        applyImages(photoMap, dto.getPhotoMapImages());
        applyStickers(photoMap, dto.getPhotoMapStickers());
    }

    // 포토맵 삭제 (파일을 먼저 지우고 행을 지운다)
    @Transactional
    public void delete(Long photoMapId, Long memberId) {
        PhotoMapEntity photoMap = getOwned(photoMapId, memberId);
        photoMap.getPhotoMapImages().forEach(i -> deleteFile(i.getPhotoMapImagePath()));
        photoMap.getPhotoMapStickers().forEach(s -> deleteFile(s.getPhotoMapStickerImagePath()));
        photoMapRepository.delete(photoMap);
    }

    // 내 포토맵인지 확인하면서 조회 (남의 것은 못 찾은 것으로 취급)
    private PhotoMapEntity getOwned(Long photoMapId, Long memberId) {
        return photoMapRepository.findByPhotoMapIdAndMember_MemberId(photoMapId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("포토맵을 찾을 수 없습니다."));
    }

    // 제목 검사 (공백 불가, 50자 이내)
    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }
        if (title.trim().length() > 50) {
            throw new IllegalArgumentException("제목은 50자 이내로 입력해 주세요.");
        }
    }

    // 지역 사진 재조정 (삭제 / 수정 / 추가, 지역코드로 짝을 맞춘다)
    private void applyImages(PhotoMapEntity photoMap, List<PhotoMapImageDTO> imageDtos) {
        List<PhotoMapImageDTO> submitted = imageDtos == null ? List.of() : imageDtos;
        Set<String> submittedCodes = new HashSet<>();
        for (PhotoMapImageDTO dto : submitted) {
            submittedCodes.add(dto.getPhotoMapImageRegionCode());
        }

        photoMap.getPhotoMapImages().removeIf(image -> {
            if (submittedCodes.contains(image.getPhotoMapImageRegionCode())) {
                return false;
            }
            deleteFile(image.getPhotoMapImagePath());
            return true;
        });

        for (PhotoMapImageDTO dto : submitted) {
            PhotoMapImageEntity image = null;
            for (PhotoMapImageEntity i : photoMap.getPhotoMapImages()) {
                if (i.getPhotoMapImageRegionCode().equals(dto.getPhotoMapImageRegionCode())) {
                    image = i;
                    break;
                }
            }

            MultipartFile file = dto.getPhotoMapImageFile();
            boolean hasFile = file != null && !file.isEmpty();

            if (image == null) {
                if (!hasFile) {
                    continue;
                }
                image = new PhotoMapImageEntity();
                image.setPhotoMap(photoMap);
                image.setPhotoMapImageRegionCode(dto.getPhotoMapImageRegionCode());
                image.setPhotoMapImageRegionName(dto.getPhotoMapImageRegionName());
                photoMap.getPhotoMapImages().add(image);
            }

            if (hasFile) {
                deleteFile(image.getPhotoMapImagePath());
                image.setPhotoMapImagePath(saveFile(photoMap.getMember().getMemberId(), file));
            }
            if (dto.getPhotoMapImageOffsetX() != null) image.setPhotoMapImageOffsetX(dto.getPhotoMapImageOffsetX());
            if (dto.getPhotoMapImageOffsetY() != null) image.setPhotoMapImageOffsetY(dto.getPhotoMapImageOffsetY());
            if (dto.getPhotoMapImageSize() != null) image.setPhotoMapImageSize(dto.getPhotoMapImageSize());
        }
    }

    // 스티커 재조정 (applyImages 와 같은 구조, 짝은 PK 로 맞춘다)
    private void applyStickers(PhotoMapEntity photoMap, List<PhotoMapStickerDTO> stickerDtos) {
        List<PhotoMapStickerDTO> submitted = stickerDtos == null ? List.of() : stickerDtos;
        Set<Long> submittedIds = new HashSet<>();
        for (PhotoMapStickerDTO dto : submitted) {
            if (dto.getPhotoMapStickerId() != null) {
                submittedIds.add(dto.getPhotoMapStickerId());
            }
        }

        photoMap.getPhotoMapStickers().removeIf(sticker -> {
            if (submittedIds.contains(sticker.getPhotoMapStickerId())) {
                return false;
            }
            deleteFile(sticker.getPhotoMapStickerImagePath());
            return true;
        });

        for (PhotoMapStickerDTO dto : submitted) {
            PhotoMapStickerEntity sticker = null;
            if (dto.getPhotoMapStickerId() != null) {
                for (PhotoMapStickerEntity s : photoMap.getPhotoMapStickers()) {
                    if (dto.getPhotoMapStickerId().equals(s.getPhotoMapStickerId())) {
                        sticker = s;
                        break;
                    }
                }
            }

            MultipartFile file = dto.getPhotoMapStickerImageFile();
            boolean hasFile = file != null && !file.isEmpty();
            PhotoMapStickerType type = dto.getPhotoMapStickerType() == null
                    ? PhotoMapStickerType.POLAROID : dto.getPhotoMapStickerType();

            if (sticker == null) {
                if (type == PhotoMapStickerType.POLAROID && !hasFile) {
                    continue;
                }
                sticker = new PhotoMapStickerEntity();
                sticker.setPhotoMap(photoMap);
                sticker.setPhotoMapStickerType(type);
                photoMap.getPhotoMapStickers().add(sticker);
            }

            if (hasFile) {
                deleteFile(sticker.getPhotoMapStickerImagePath());
                sticker.setPhotoMapStickerImagePath(saveFile(photoMap.getMember().getMemberId(), file));
            }
            sticker.setPhotoMapStickerText(dto.getPhotoMapStickerText());
            sticker.setPhotoMapStickerTextStyle(dto.getPhotoMapStickerTextStyle());
            sticker.setPhotoMapStickerBackground(dto.getPhotoMapStickerBackground());
            if (dto.getPhotoMapStickerX() != null) sticker.setPhotoMapStickerX(dto.getPhotoMapStickerX());
            if (dto.getPhotoMapStickerY() != null) sticker.setPhotoMapStickerY(dto.getPhotoMapStickerY());
            if (dto.getPhotoMapStickerRotation() != null) sticker.setPhotoMapStickerRotation(dto.getPhotoMapStickerRotation());
            if (dto.getPhotoMapStickerSize() != null) sticker.setPhotoMapStickerSize(dto.getPhotoMapStickerSize());
            if (dto.getPhotoMapStickerOrder() != null) sticker.setPhotoMapStickerOrder(dto.getPhotoMapStickerOrder());
        }
    }

    // 업로드 파일 저장 (이름 겹치면 _(1) 증분, DB 에 넣을 경로 반환)
    private String saveFile(Long memberId, MultipartFile file) {
        String name = ImageFiles.newName(file);
        try {
            Path dir = photomapDir.resolve(String.valueOf(memberId));
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            Files.copy(file.getInputStream(), target);
            String path = photomapDir.toString().replace("\\", "/").substring(appHome.length());
            return path + "/" + memberId + "/" + target.getFileName();
        } catch (IOException e) {
            throw new UncheckedIOException("사진 저장에 실패했습니다.", e);
        }
    }

    // 저장된 경로로 실제 파일 삭제 (app.home 기준 상대경로)
    private void deleteFile(String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(photomapDir.resolve(filePath.substring("/file/photomap/".length())));
        } catch (IOException e) {
            throw new UncheckedIOException("사진 삭제에 실패했습니다.", e);
        }
    }
}
