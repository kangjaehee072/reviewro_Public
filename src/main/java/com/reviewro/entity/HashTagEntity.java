package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Table (name = "hashTag")
@NoArgsConstructor
@Entity
public class HashTagEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hashTagId;

    private String hashTagName;
    private String hashTagCategory;

    @Column(nullable = true)
    private Integer hashTagUseCnt;


}
