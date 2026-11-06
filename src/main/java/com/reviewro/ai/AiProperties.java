package com.reviewro.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(String endpoint, String model, int numCtx, int numPredict, Claude claude) {

    // API 키는 .env 에만 둔다 (DB·화면에 안 남긴다). 비어 있으면 Claude 로 못 바꾼다
    public record Claude(String apiKey, String model) {

        // .env 는 properties 형식이라 따옴표·끝 공백까지 값으로 들어간다. 흔한 실수라 걷어낸다
        @Override
        public String apiKey() {
            if (apiKey == null) {
                return null;
            }
            String key = apiKey.strip();
            if (key.length() >= 2 && (key.startsWith("\"") && key.endsWith("\"")
                    || key.startsWith("'") && key.endsWith("'"))) {
                key = key.substring(1, key.length() - 1).strip();
            }
            return key;
        }
    }

    public boolean hasClaudeKey() {
        return claude != null && claude.apiKey() != null && !claude.apiKey().isBlank();
    }
}
