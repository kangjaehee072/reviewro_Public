package com.reviewro.config;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// 업로드 사진은 /data/** 로 그대로 공개되므로, .html .svg 같은 파일이 섞이면 우리 도메인에서 스크립트가 돈다.
// 확장자를 허용 목록으로 거르고, 저장 이름은 서버가 새로 짓는다
public final class ImageFiles {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private ImageFiles() {}

    // 허용된 사진이면 "uuid.확장자" 를, 아니면 예외를 던진다
    public static String newName(MultipartFile file) {
        String original = file.getOriginalFilename();
        int dot = original == null ? -1 : original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        if (!ALLOWED.contains(ext) || contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("jpg, png, gif, webp 사진만 올릴 수 있습니다.");
        }
        return UUID.randomUUID() + "." + ext;
    }
}
