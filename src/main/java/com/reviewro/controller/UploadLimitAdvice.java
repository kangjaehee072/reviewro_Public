package com.reviewro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// 업로드 한도(spring.servlet.multipart.*)를 넘으면 컨트롤러에 닿기 전에 막힌다.
// 그냥 두면 브라우저에는 이유 없이 "Failed to fetch" 만 뜨니, 읽을 수 있는 문장으로 답한다
@RestControllerAdvice
public class UploadLimitAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> tooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body("사진 용량이 너무 커요. 한 장은 20MB, 한 번에 2GB까지 올릴 수 있어요.");
    }
}
