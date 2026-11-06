package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.HashTagDTO;
import com.reviewro.entity.HashTagEntity;
import com.reviewro.repository.HashTagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HashTagService {
    private  final HashTagRepository hashTagRepository;

    // insert
    public void hashtaginsert(HashTagDTO hashTagDTO) {
        hashTagRepository.save(AutoMapper.map(hashTagDTO, HashTagEntity.class));
    }

    // list
    public List<HashTagDTO> hashtaglist(){
        List<HashTagEntity> hashtagEntity = hashTagRepository.findAll();
        List <HashTagDTO> hashTags = new ArrayList<>();
        for (HashTagEntity entity : hashtagEntity){
            hashTags.add(AutoMapper.map(entity, HashTagDTO.class));
        }
        return hashTags;
    }

    // read
    public HashTagDTO hashtagread(Long id){
        HashTagEntity hashTagEntity = hashTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해시태그가 존재하지 않습니다."));
        return AutoMapper.map(hashTagEntity, HashTagDTO.class);
    }

    // update
    @Transactional
    public void hashtagupdate(HashTagDTO hashTagDTO){
        HashTagEntity entity = hashTagRepository.findById(hashTagDTO.getHashTagId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 해시태그입니다."));
        AutoMapper.copyNonNull(hashTagDTO, entity);
    }


    // delete
    @Transactional
    public void hashtagdelete(Long id) {
        hashTagRepository.deleteById(id);
    }


}
