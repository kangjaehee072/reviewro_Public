package com.reviewro.controller;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.CheckField;
import com.reviewro.dto.MemberDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.security.MemberDetails;
import com.reviewro.security.SocialMemberService;
import com.reviewro.service.MemberService;
import com.reviewro.service.TermsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final TermsService termsService;
    private final SocialMemberService socialMemberService;

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        if ("social".equals(error)) {
            model.addAttribute("errorMessage", "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
        } else if ("locked".equals(error)) {
            model.addAttribute("errorMessage", "로그인 시도가 너무 많아요. 30초 뒤에 다시 시도해 주세요.");
        } else if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "member/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("memberDto", new MemberDTO());
        addTerms(model);
        return "member/signup";
    }

    // 가입 화면 아래 필수 약관 동의 칸과 '보기' 모달에 쓸 내용
    private void addTerms(Model model) {
        model.addAttribute("requiredTerms", termsService.requiredSections());
        model.addAttribute("siteInfo", termsService.siteInfo());
    }

    @GetMapping("/signup/check")
    @ResponseBody
    public boolean checkDuplicate(@RequestParam CheckField field, @RequestParam String value,
                                  HttpServletRequest request) {
        // 로그인 없이 부를 수 있어서, 대량으로 아이디·닉네임 가입 여부를 훑지 못하게 IP 당 횟수를 묶는다
        if (!allowCheck(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요.");
        }
        return memberService.isDuplicate(field, value);
    }

    // IP 당 10분에 60번까지 (입력할 때마다 0.4초 쉬고 부르므로 사람이 쓰기엔 넉넉하다)
    private static final int CHECK_LIMIT = 60;
    private static final long CHECK_WINDOW_MS = 10 * 60 * 1000L;
    private final Map<String, Deque<Long>> checkLog = new ConcurrentHashMap<>();

    private boolean allowCheck(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> hits = checkLog.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > CHECK_WINDOW_MS) {
                hits.pollFirst();
            }
            if (hits.size() >= CHECK_LIMIT) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("memberDto") MemberDTO memberDto,
                         @RequestParam(value = "agreedTerms", required = false) List<Long> agreedTerms,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            // 화면 체크박스만 믿지 않고 서버에서도 필수 약관에 다 동의했는지 본다
            if (!termsService.agreedToAllRequired(agreedTerms)) {
                throw new IllegalArgumentException("필수 약관에 모두 동의해야 가입할 수 있어요.");
            }
            memberService.signup(memberDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addTerms(model);
            return "member/signup";
        }
        redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    // 소셜로 처음 온 사람의 약관 동의 화면. 동의 전까지는 저장되지 않는다 (SocialSignupGate 가 여기로 보낸다)
    @GetMapping("/signup/social")
    public String socialSignupForm(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null || !SocialMemberService.isPending(details.getMember())) {
            return "redirect:/";
        }
        addTerms(model);
        return "member/social-signup";
    }

    @PostMapping("/signup/social")
    public String socialSignup(@AuthenticationPrincipal MemberDetails details,
                               @RequestParam(value = "agreedTerms", required = false) List<Long> agreedTerms,
                               Model model) {
        if (details == null || !SocialMemberService.isPending(details.getMember())) {
            return "redirect:/";
        }
        if (!termsService.agreedToAllRequired(agreedTerms)) {
            model.addAttribute("errorMessage", "필수 약관에 모두 동의해야 가입할 수 있어요.");
            addTerms(model);
            return "member/social-signup";
        }
        MemberEntity saved = socialMemberService.completeSignup(details.getMember());
        // 세션에 올라간 회원에 회원 번호 등을 채운다 (다른 탭에서 먼저 가입했으면 그 회원 정보로)
        if (saved != details.getMember()) {
            AutoMapper.copy(saved, details.getMember());
        }
        return "redirect:/";
    }
}
