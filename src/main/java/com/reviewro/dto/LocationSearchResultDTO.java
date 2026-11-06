package com.reviewro.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LocationSearchResultDTO {
    private List<LocationDTO> locations;
    private boolean more;

}
