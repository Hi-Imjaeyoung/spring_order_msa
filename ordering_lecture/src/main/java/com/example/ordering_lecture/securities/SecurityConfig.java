package com.example.ordering_lecture.securities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) //prePostEnabled - Spring Security의 @PreAuthorize, @PreFilter /@PostAuthorize, @PostFilter어노테이션 활성화 여부
public class SecurityConfig {
    @Autowired
    private final JwtAuthFilter jwtAuthFilter;
    @Autowired
    private final ExceptionHandlerFilter exceptionHandlerFilter;
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ExceptionHandlerFilter exceptionHandlerFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.exceptionHandlerFilter = exceptionHandlerFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // xss와 csrf의 차이 정리 필요
                .csrf().disable()
                .cors().and() // CORS 활성화
                .httpBasic().disable() //우리가 별도로 설정을 하겠다
                .authorizeRequests()                                                 // 이후 모든 경로다
                    .antMatchers("/member/create","/doLogin","/items","/item/*/image")
                    .permitAll()
                .anyRequest().authenticated()
                .and() //Session을 사용하지 않겠다
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(exceptionHandlerFilter,UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter,UsernamePasswordAuthenticationFilter.class)// 오른쪽 객체보다 먼저 실행
                .build();
    }
}
