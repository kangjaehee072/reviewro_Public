package com.reviewro.controller;

import com.reviewro.entity.TypeEntity;
import com.reviewro.repository.TypeRepository;
import com.reviewro.service.TypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TypeController {

    private final TypeService typeService;

    @GetMapping ("/api/types")
    public List<TypeEntity> getTypes(){
        return typeService.getAllTypes();
    }

}
