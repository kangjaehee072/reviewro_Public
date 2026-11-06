package com.reviewro.service;

import com.reviewro.entity.CategoryEntity;
import com.reviewro.entity.TypeEntity;
import com.reviewro.repository.CategoryRepository;
import com.reviewro.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// AI 에게 선택지로 줄 타입·카테고리 이름 목록.
// 자바 이넘으로 못 박지 않은 이유: 관리자가 수시로 추가·수정하는 값이라서다.
// 그래서 호출 시점에 DB 에서 읽어 tool 스키마의 enum 으로 넣는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiVocab {

    private final TypeRepository typeRepository;
    private final CategoryRepository categoryRepository;

    // 활성 항목만 순서대로. 비활성은 관리자가 일부러 숨긴 것이니 AI 에게도 주지 않는다
    public Vocab load() {
        List<String> types = new ArrayList<>();
        for (TypeEntity t : typeRepository.findByTypeActiveTrueOrderByTypeSortOrder()) {
            if (t.getTypeName() != null && !t.getTypeName().isBlank()) types.add(t.getTypeName());
        }

        List<String> categories = new ArrayList<>();
        for (CategoryEntity c : categoryRepository.findByCategoryActiveTrueOrderByCategorySortOrder()) {
            if (c.getCategoryName() != null && !c.getCategoryName().isBlank()) categories.add(c.getCategoryName());
        }

        return new Vocab(types, categories);
    }

    public record Vocab(List<String> types, List<String> categories) {}
}
