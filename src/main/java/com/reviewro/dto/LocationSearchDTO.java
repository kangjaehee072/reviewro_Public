package com.reviewro.dto;

import com.reviewro.entity.Address;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class LocationSearchDTO {
    private Address address;
    private Double lat;
    private Double lng;
    private Integer radius;
    private String keyword;
    private String element;
    private Double minRating;
    private String sort;
    private Long typeId;
    private List<Long> categories;
    private int page;

}
