package com.reviewro.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name ="location_detail")
@NoArgsConstructor
public class LocationDetailEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itmeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="location_id")
    private LocationEntity locationEntity;

    private String itemName;
    private String itemCategory;
    private String itemCharge;
    private Boolean itemMain;
    private String itemImage;



}
