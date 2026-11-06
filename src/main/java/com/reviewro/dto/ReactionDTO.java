package com.reviewro.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReactionDTO {
    private long likeCount;
    private long dislikeCount;
    private String myKind;     // "LIKE" / "DISLIKE" / null
}
