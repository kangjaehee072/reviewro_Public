package com.reviewro.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiChatRequestDTO {
    private AiPersona persona;
    private List<AiMessageDTO> messages;
}