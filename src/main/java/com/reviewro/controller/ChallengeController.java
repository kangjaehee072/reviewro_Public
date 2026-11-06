package com.reviewro.controller;


import com.reviewro.dto.ChallengeDTO;
import com.reviewro.repository.ChallengeRepository;
import com.reviewro.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;

    //insert
    @GetMapping("/challengeinsert")
    public String challengeinsert(Model model){
        model.addAttribute("challenge", new ChallengeDTO());
        return "challenge/challengeinsert";
    }
    @PostMapping("/challengeinsert")
    public String challengeinsert(ChallengeDTO challengeDTO){
        challengeService.challengeinsert(challengeDTO);
        return "redirect:/challengelist";
    }

    //list
    @GetMapping("/challengelist")
    public String challenge(Model model){
        List<ChallengeDTO> challenges = challengeService.challengelist();
        model.addAttribute("challenges", challenges);
        return "challenge/challengelist";
    }

    //read
    @GetMapping("/challengeread/{id}")
    public String challengeread(@PathVariable("id")Long id, Model model){
        ChallengeDTO challenge = challengeService.challengeread(id);
        model.addAttribute("challenge", challenge);
        return "challenge/challengeread";
    }

    //update
    @PostMapping("/challengeupdate")
    public String challengeupdate(@ModelAttribute ChallengeDTO challengeDTO){
        challengeService.challengeupdate(challengeDTO);
        return "redirect:/challengelist";
    }

    //delete
    @PostMapping("/challengedelete/{id}")
    public String challengedelete(@PathVariable("id")Long id){
        challengeService.challengedelete(id);
        return "redirect:/challengelist";
    }

    @GetMapping("/challenge/random")
    public String getRandomChallenge(Model model) {
        ChallengeDTO randomChallenge = challengeService.getRandomChallenge();

        // 만약 랜덤 챌린지 전용 단일 추천 페이지/모달로 보낼 경우
        model.addAttribute("challenge", randomChallenge);
        return "challenge/random-detail";
    }

    @GetMapping("/challenge/roulette")
    public String challengeRoulette(Model model) {
        // DB에 등록된 전체 챌린지 리스트 조회
        List<ChallengeDTO> challengeList = challengeService.getAllChallenges();
        model.addAttribute("challengeList", challengeList);
        return "challenge/roulette";
    }


}
