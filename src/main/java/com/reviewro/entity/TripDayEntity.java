package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@Setter
@Table(name="trip_day")
@NoArgsConstructor
public class TripDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripDayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripplan_id")
    private TripPlanEntity tripPlan;

    private String tripDayNumber;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripPlaceEntity> tripPlaces = new ArrayList<>();
}
