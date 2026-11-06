package com.reviewro.ai;

// AI 답을 만들 모델 서버
public enum AiProvider {
    OLLAMA,    // 자체 서버 (무료, 느림)
    CLAUDE     // Anthropic Claude API (유료, API 키 필요)
}
