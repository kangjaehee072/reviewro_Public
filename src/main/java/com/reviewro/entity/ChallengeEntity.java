package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Table ( name = "challenge")
@Entity
@NoArgsConstructor
public class ChallengeEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long challengeId;

    private String challengeName;
    private String challengeDescription;



    //@ManyToOne
    //@JoinColumn
    //private Location location;




}
