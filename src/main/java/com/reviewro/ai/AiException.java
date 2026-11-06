package com.reviewro.ai;

// 모델 호출 실패. 메시지는 화면에 그대로 보여 줄 문장이다 (모델 서버 원문은 로그에만 남긴다)
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
