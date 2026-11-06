package com.reviewro.service;

import com.reviewro.entity.TypeEntity;
import com.reviewro.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeService {

    private final TypeRepository typeRepository;

    public List<TypeEntity> getAllTypes(){
        return typeRepository.findAll();
    }
}
