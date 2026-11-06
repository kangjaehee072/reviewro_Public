package com.reviewro.ai;

import com.reviewro.entity.AiSettingEntity;
import com.reviewro.repository.AiSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 지금 어느 모델 서버를 쓸지. 질문마다 DB 를 읽지 않게 메모리에 들고 있고, 바꾸면 다음 질문부터 바로 적용된다
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(AiProperties.class)
public class AiSettingService {

    // 관리자 화면에서 고를 수 있는 Claude 모델
    public static final List<String> CLAUDE_MODELS = List.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5");

    private final AiSettingRepository aiSettingRepository;
    private final AiProperties aiProperties;
    private final OllamaClient ollamaClient;
    private final ClaudeClient claudeClient;

    private volatile AiSettingEntity cached;

    public AiSettingEntity current() {
        AiSettingEntity setting = cached;
        if (setting == null) {
            setting = aiSettingRepository.findById(AiSettingEntity.ID).orElseGet(AiSettingEntity::new);
            if (setting.getClaudeModel() == null) {
                setting.setClaudeModel(defaultClaudeModel());
            }
            cached = setting;
        }
        return setting;
    }

    // 지금 설정대로 고른 모델 서버. Claude 로 돼 있어도 키가 없으면 Ollama 로 돌아간다
    public AiClient client() {
        AiSettingEntity setting = current();
        if (setting.getProvider() == AiProvider.CLAUDE && aiProperties.hasClaudeKey()) {
            return claudeClient.withModel(setting.getClaudeModel());
        }
        return ollamaClient;
    }

    // 지금 실제로 Claude 가 답하는지 (말투 보강을 붙일지 정할 때 쓴다)
    public boolean usingClaude() {
        return current().getProvider() == AiProvider.CLAUDE && aiProperties.hasClaudeKey();
    }

    // 관리자 화면 [연결 테스트] 용. 저장하지 않고 고른 대로 만든다
    public AiClient clientFor(AiProvider provider, String claudeModel) {
        return provider == AiProvider.CLAUDE ? claudeClient.withModel(claudeModel) : ollamaClient;
    }

    @Transactional
    public void save(AiProvider provider, String claudeModel, String updatedBy) {
        if (provider == AiProvider.CLAUDE && !aiProperties.hasClaudeKey()) {
            throw new IllegalArgumentException("Claude API 키가 서버에 없어요. .env 에 ANTHROPIC_API_KEY 를 넣고 서버를 다시 켜 주세요.");
        }
        if (!CLAUDE_MODELS.contains(claudeModel)) {
            throw new IllegalArgumentException("고를 수 없는 모델이에요.");
        }
        AiSettingEntity setting = aiSettingRepository.findById(AiSettingEntity.ID).orElseGet(AiSettingEntity::new);
        setting.setProvider(provider);
        setting.setClaudeModel(claudeModel);
        setting.setUpdatedBy(updatedBy);
        cached = aiSettingRepository.save(setting);
    }

    public boolean hasClaudeKey() {
        return aiProperties.hasClaudeKey();
    }

    public String ollamaModel() {
        return aiProperties.model();
    }

    private String defaultClaudeModel() {
        String model = aiProperties.claude() == null ? null : aiProperties.claude().model();
        return CLAUDE_MODELS.contains(model) ? model : CLAUDE_MODELS.get(0);
    }
}
