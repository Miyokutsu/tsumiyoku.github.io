package org.tsumiyoku.gov.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class FilterConfig {
    @Bean
    public SecurityFilterChain chain(HttpSecurity http, SessionAuthFilter saf) throws Exception {
        http.addFilterBefore(saf, UsernamePasswordAuthenticationFilter.class);
        // … (le reste de ta config déjà fournie)
        return http.build();
    }
}