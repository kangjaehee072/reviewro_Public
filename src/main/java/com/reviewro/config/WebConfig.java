package com.reviewro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).normalize().toUri().toString();
        registry.addResourceHandler("/data/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 소셜 가입 약관 동의 전에 열어 둘 곳: 동의 화면, 약관, 오류 화면, 정적 파일
        registry.addInterceptor(new SocialSignupGate())
                .excludePathPatterns(SocialSignupGate.CONSENT_PATH, "/terms", "/terms/**", "/error",
                        "/css/**", "/js/**", "/img/**", "/frontend/**", "/data/**", "/favicon.ico");
    }


    // 내부 로컬 테스트용

}
