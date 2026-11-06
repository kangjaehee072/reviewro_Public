package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name="trip_place")
@NoArgsConstructor
public class TripPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripPlaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripday_id")
    private TripDayEntity tripDay;

    private Long locationId; // 장소 ID (FK)

    private String tripPlaceSequence;

    private String tripPlaceMemo;
}
