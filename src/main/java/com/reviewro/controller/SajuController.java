package com.reviewro.controller;


import com.reviewro.dto.SajuDTO;
import com.reviewro.service.SajuService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.beans.Transient;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class SajuController {

    private final SajuService sajuService;

    //insert
    @GetMapping("/sajuinsert")
    public String sajuinsert(Model model) {
        model.addAttribute("sajuDTO", new SajuDTO());
        return "saju/sajuinsert";
    }

    @PostMapping("/sajuinsert")
    public String sajuinsert(SajuDTO sajuDTO){
        sajuService.sajuinsert(sajuDTO);
        return "redirect:/sajulist";
    }


    //list
    @GetMapping("/sajulist")
    public String sajulist(Model model){
        List<SajuDTO> saju = sajuService.sajulist();
        model.addAttribute("sajuList", saju);
        return "saju/sajulist";
    }

    //read
    @GetMapping("/sajuread/{id}")
    public String sajuread(@PathVariable("id")Long id ,Model model){
        SajuDTO saju = sajuService.sajuread(id);
        model.addAttribute("saju",saju);
        return "saju/sajuread";
    }

    //update
    @GetMapping("/sajuupdate/{id}")
    public String sajuupdate(@PathVariable("id")Long id, Model model){
        SajuDTO sajuDTO = sajuService.sajuread(id);
        model.addAttribute("sajuDTO",sajuDTO);
        return "saju/sajuupdate";
    }

    @PostMapping("/sajuupdate")
    public String sajuupdate(SajuDTO sajuDTO){
        sajuService.sajuupdate(sajuDTO);
        return "saju/sajuread"+sajuDTO.getSajuId();
    }

    //delete
    @PostMapping ("/sajudelete/{id}")
    public String sajudelete(@PathVariable("id") Long id){
        sajuService.sajudelete(id);
        return "redirect:/sajulist";
    }


}
