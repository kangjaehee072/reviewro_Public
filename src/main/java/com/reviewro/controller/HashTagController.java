package com.reviewro.controller;

import com.reviewro.dto.HashTagDTO;
import com.reviewro.repository.HashTagRepository;
import com.reviewro.service.HashTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HashTagController {

    private final HashTagService hashTagService;
    private final HashTagRepository hashTagRepository;

    // insert
    @GetMapping ("/hashtaginsert")
    public String hashtaginsert(){
        return "hashtag/hashtaginsert";
    }

    @PostMapping("/hashtaginsert")
    public String hashtaginsert(HashTagDTO hashTagDTO){
        hashTagService.hashtaginsert(hashTagDTO);
        return "redirect:/hashtaglist";
    }

    // list
    @GetMapping ("/hashtaglist")
    public String hashtag(Model model){
        List<HashTagDTO> hashtags = hashTagService.hashtaglist();
        model.addAttribute("hashtags", hashtags);
        return "hashtag/hashtaglist";
    }

    // read
    @GetMapping("/hashtagread/{id}")
    public String hashtagread(@PathVariable("id")Long id, Model model){
        HashTagDTO hashTag = hashTagService.hashtagread(id);
        model.addAttribute("hashtag", hashTag);
        return "hashtag/hashtagread";
    }

    // update
    @PostMapping("/hashtagupdate")
    public String hashtagupdate(@ModelAttribute HashTagDTO hashTagDTO){
        hashTagService.hashtagupdate(hashTagDTO);
        return "redirect:/hashtaglist";
    }


    // delete
    @DeleteMapping("/hashtagdelete/{id}")
    @ResponseBody
    public ResponseEntity<Void> hashtagdelete(@PathVariable("id") Long id){
        hashTagService.hashtagdelete(id);
        return ResponseEntity.ok().build();
    }




}
