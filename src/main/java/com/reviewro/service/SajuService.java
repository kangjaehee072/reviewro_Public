package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.SajuDTO;
import com.reviewro.entity.SajuEntity;
import com.reviewro.repository.SajuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SajuService {

    private final SajuRepository sajuRepository;

    //insert
    public void sajuinsert(SajuDTO sajuDTO) {
        sajuRepository.save(AutoMapper.map(sajuDTO, SajuEntity.class));
    }

    //list
    public List<SajuDTO> sajulist() {
        List<SajuEntity> sajuEntities = sajuRepository.findAll();
        List<SajuDTO> sajuDTOs = new ArrayList<>();
        for (SajuEntity sajuEntity : sajuEntities) {
            sajuDTOs.add(AutoMapper.map(sajuEntity, SajuDTO.class));
        }
        return sajuDTOs;
    }

    //read
    public SajuDTO sajuread(Long id) {
        SajuEntity sajuEntity = sajuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사주정보를 찾을 수 없습니다."));
        return AutoMapper.map(sajuEntity, SajuDTO.class);

    }



    //update
    public void sajuupdate(SajuDTO sajuDTO) {
        SajuEntity sajuEntity = sajuRepository.findById(sajuDTO.getSajuId())
                .orElseThrow(() -> new IllegalArgumentException("사주정보를 찾을 수 없습니다."));
        AutoMapper.copyNonNull(sajuDTO, sajuEntity);
    }

    //delete
    public void sajudelete(Long id) {
        sajuRepository.deleteById(id);
    }


}
