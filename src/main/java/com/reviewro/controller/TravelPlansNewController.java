package com.reviewro.controller;

import com.reviewro.dto.TravelPlansDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.TravelPlansService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Log4j2
public class TravelPlansNewController {

    private final TravelPlansService travelPlansService;

    // 마이페이지 목록 리스트 조회
    @GetMapping("/mypage/travelplan")
    public String plans(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        Long memberUserId = details.getMember().getMemberId();

        List<TravelPlansDTO> travelplans = travelPlansService.getTravelPlanList(memberUserId);
        model.addAttribute("travelPlans", travelplans);

        return "mypage/plans/travelplan";
    }

    // 새 여행 계획 등록 폼 페이지 이동
    @GetMapping("/mypage/plans/insert")
    public String insertForm(@AuthenticationPrincipal MemberDetails details,Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        model.addAttribute("travelPlansDTO", new TravelPlansDTO());
        return "mypage/plans/planinsert";
    }

    // 새 여행 계획 등록 처리 (POST)
    @PostMapping("/mypage/plans/insert")
    public String insertPlan(@ModelAttribute TravelPlansDTO travelPlansDTO,
                             @AuthenticationPrincipal MemberDetails details,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        try {
            travelPlansDTO.setMemberId(travelPlansDTO.getMemberId());

            if (travelPlansDTO.getPlanStartDate() != null && !travelPlansDTO.getPlanStartDate().isEmpty() &&
                    travelPlansDTO.getPlanEndDate() != null && !travelPlansDTO.getPlanEndDate().isEmpty()) {

                LocalDate startDate = LocalDate.parse(travelPlansDTO.getPlanStartDate());
                LocalDate endDate = LocalDate.parse(travelPlansDTO.getPlanEndDate());

                if (startDate.isAfter(endDate)) {
                    model.addAttribute("errorMessage", "여행 시작일은 종료일보다 이전이어야 합니다.");
                    model.addAttribute("travelPlansDTO", travelPlansDTO);
                    return "mypage/plans/planinsert";
                }
            }

            travelPlansService.insertTravelPlan(travelPlansDTO, details.getMember().getMemberUserId());
            redirectAttributes.addFlashAttribute("message", "새 여행 계획이 등록되었습니다.");
            return "redirect:/mypage/travelplan";

        } catch (Exception e) {
            log.error("여행 계획 등록 중 오류 발생: ", e);
            model.addAttribute("errorMessage", "여행 계획 등록 중 오류가 발생했습니다. 다시 시도해주세요.");
            model.addAttribute("travelPlansDTO", travelPlansDTO);
            return "mypage/plans/planinsert";
        }
    }

    // 마이페이지 여행계획 상세조회
    @GetMapping("/mypage/plans/read/{id}")
    public String readPlan(@AuthenticationPrincipal MemberDetails details,@PathVariable Long id, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        Long memberUserId = details.getMember().getMemberId();
        log.info("여행계획" + id);
        TravelPlansDTO travelPlansDTO = travelPlansService.readSchedule(id, memberUserId);
        model.addAttribute("travelPlan", travelPlansDTO);

        log.info("조회된 travelPlansDTO 데이터: {}", travelPlansDTO);
        return "mypage/plans/planread";
    }

    // 여행계획 수정
    @GetMapping("/mypage/plans/update/{planId}")
    public String updateForm(@PathVariable("planId") Long planId,
                             @AuthenticationPrincipal MemberDetails details, Model model) {
        TravelPlansDTO travelPlanDTO = travelPlansService.readTravelPlan(planId, details.getMember().getMemberId());
        model.addAttribute("travelPlansDTO", travelPlanDTO);
        return "mypage/plans/planinsert";
    }

    @PostMapping("/mypage/plans/update/{planId}")
    public String updatePlan(@PathVariable("planId") Long planId,
                             @ModelAttribute TravelPlansDTO travelPlansDTO,
                             @AuthenticationPrincipal MemberDetails details,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (details == null) {
            return "redirect:/login";
        }

        try {
            // 1. 날짜 유효성 검사
            if (travelPlansDTO.getPlanStartDate() != null && !travelPlansDTO.getPlanStartDate().isEmpty() &&
                    travelPlansDTO.getPlanEndDate() != null && !travelPlansDTO.getPlanEndDate().isEmpty()) {

                LocalDate startDate = LocalDate.parse(travelPlansDTO.getPlanStartDate());
                LocalDate endDate = LocalDate.parse(travelPlansDTO.getPlanEndDate());

                if (startDate.isAfter(endDate)) {
                    model.addAttribute("errorMessage", "여행 시작일은 종료일보다 이전이어야 합니다.");
                    travelPlansDTO.setPlanId(planId); // 수정 중 id 유실 방지
                    model.addAttribute("travelPlansDTO", travelPlansDTO);
                    return "mypage/plans/planinsert"; // 에러 시 수정 폼으로 복귀
                }
            }

            travelPlansService.updateTravelPlan(planId, details.getMember().getMemberId(), travelPlansDTO);

            redirectAttributes.addFlashAttribute("message", "여행 계획이 성공적으로 수정되었습니다.");
            return "redirect:/mypage/travelplan";

        } catch (Exception e) {
            log.error("여행 계획 수정 중 오류 발생: ", e);
            model.addAttribute("errorMessage", "여행 계획 수정 중 오류가 발생했습니다. 다시 시도해주세요.");
            travelPlansDTO.setPlanId(planId);
            model.addAttribute("travelPlansDTO", travelPlansDTO);
            return "mypage/plans/planinsert";
        }
    }


    //여행계획 삭제
    @PostMapping("/mypage/plans/delete/{planId}")
    public String deletePlan(@PathVariable("planId") Long planId,
                             @AuthenticationPrincipal MemberDetails details,
                             RedirectAttributes redirectAttributes) {
        try {
            travelPlansService.deleteTravelPlan(planId, details.getMember().getMemberId());
            redirectAttributes.addFlashAttribute("message", "여행 계획이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "여행 계획 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/mypage/travelplan";
    }





}