package com.reviewro.entity;

import com.reviewro.ai.AiProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 슈퍼어드민이 고른 AI 설정. 한 줄만 쓴다 (id = 1)
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_setting")
public class AiSettingEntity extends BaseTimeEntity {

    public static final long ID = 1L;

    @Id
    private Long aiSettingId = ID;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AiProvider provider = AiProvider.OLLAMA;

    @Column(length = 50)
    private String claudeModel;           // CLAUDE 일 때 쓸 모델 (예: claude-opus-5-5)

    @Column(length = 50)
    private String updatedBy;             // 마지막으로 바꾼 슈퍼어드민 아이디
}
