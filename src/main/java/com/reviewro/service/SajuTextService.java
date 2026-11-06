package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.SajuTextDTO;
import com.reviewro.repository.SajuTextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SajuTextService {

    private final SajuTextRepository sajuTextRepository;

    // 풀이 문구 전체 (100개 남짓이라 한 번에 내려주고 화면에서 골라 쓴다)
    public List<SajuTextDTO> findAll() {
        return sajuTextRepository.findAll().stream()
                .map(e -> AutoMapper.map(e, SajuTextDTO.class))
                .toList();
    }
}
