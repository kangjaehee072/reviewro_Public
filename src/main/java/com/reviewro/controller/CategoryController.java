package com.reviewro.controller;

import com.reviewro.entity.CategoryEntity;
import com.reviewro.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/categories")
    public List<CategoryEntity> getAllCategories() {
        return categoryService.getAllCategories();
    }

}
