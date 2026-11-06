package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeDTO {

    private Long challengeId;
    private String challengeName;
    private String challengeDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
