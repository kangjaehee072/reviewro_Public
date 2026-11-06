package com.reviewro.service;


import com.reviewro.config.AutoMapper;
import com.reviewro.dto.ChallengeDTO;
import com.reviewro.entity.ChallengeEntity;
import com.reviewro.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;

    //insert
    public void challengeinsert(ChallengeDTO challengeDTO) {
        challengeRepository.save(AutoMapper.map(challengeDTO, ChallengeEntity.class));
    }

    //list
    public List<ChallengeDTO> challengelist() {
        List<ChallengeEntity> challenge = challengeRepository.findAll();
        List<ChallengeDTO>entity = new ArrayList<>();
        for(ChallengeEntity challengeEntity : challenge){
            entity.add(AutoMapper.map(challengeEntity, ChallengeDTO.class));
        }
        return entity;
    }

    //read

    public ChallengeDTO challengeread(Long id) {
        ChallengeEntity entity = challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("챌린지를 찾을 수 없습니다."));
        return AutoMapper.map(entity, ChallengeDTO.class);
    }


    //update
    public void challengeupdate(ChallengeDTO challengeDTO) {
        ChallengeEntity entity = challengeRepository.findById(challengeDTO.getChallengeId())
                .orElseThrow(() -> new IllegalArgumentException("챌린지를 찾을 수 없습니다."));
        AutoMapper.copyNonNull(challengeDTO, entity);
    }


    //delete
    public void challengedelete(Long id) {
        challengeRepository.deleteById(id);
    }

    // 1. 전체 챌린지 목록 조회 (룰렛 페이지용)
    public List<ChallengeDTO> getAllChallenges() {
        List<ChallengeEntity> entityList = challengeRepository.findAll();

        return entityList.stream().map(entity -> {
            ChallengeDTO dto = new ChallengeDTO();
            dto.setChallengeId(entity.getChallengeId());
            dto.setChallengeName(entity.getChallengeName());
            dto.setChallengeDescription(entity.getChallengeDescription());
            return dto;
        }).collect(Collectors.toList());
    }

    // 2. 랜덤 챌린지 1개 조회 (단일 추천/모달용)
    public ChallengeDTO getRandomChallenge() {
        ChallengeEntity entity = challengeRepository.findRandomOne().orElse(null);

        if (entity == null) {
            return null;
        }

        ChallengeDTO dto = new ChallengeDTO();
        dto.setChallengeId(entity.getChallengeId());
        dto.setChallengeName(entity.getChallengeName());
        dto.setChallengeDescription(entity.getChallengeDescription());

        return dto;
    }


}
