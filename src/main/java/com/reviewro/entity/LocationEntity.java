package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="location",
        indexes = {
        @Index( name = "idx_location_cat_address",
                columnList = "locationAddress")
        })
@NoArgsConstructor
@ToString
public class LocationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long locationId;

    private String locationName;
    private Double locationLng;
    private Double locationLat;
    private String locationDescription;
    private String locationAddress;
    private String locationAddressTw;
    private String locationNumber;
    private String locationDayofweek;
    private String locationDayoff;
    private String locationOpentime;
    private String locationClosetime;
    private String locationURL;
    private Double locationReviewAvg;
    private String locationCreatedBy;
    private String locationUpdatedBy;
    private String locationElement;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private TypeEntity type;

    @ManyToMany(fetch = FetchType.LAZY)
    private List <CategoryEntity> categories;


    @PrePersist
    @PreUpdate
    public void extractAddress(){
        if (this.locationAddress != null && !this.locationAddress.isBlank()){
            this.locationAddress = this.locationAddress.split(" ")[0];
        }
    }

    @OneToMany (mappedBy = "location" , cascade = CascadeType.ALL , orphanRemoval = true)
    private List<LocationImageEntity> locationImageEntities = new ArrayList<>();


}
