package org.tsumiyoku.gov.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;
    private final StrictOriginFilter strictOriginFilter;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.addFilterBefore(rateLimitFilter, org.springframework.security.web.csrf.CsrfFilter.class);
        http.addFilterBefore(strictOriginFilter, org.springframework.security.web.csrf.CsrfFilter.class);

        http
                .cors(c -> {
                })
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/auth/**", "/login/**", "/oauth2/**", "/login/oauth2/**", "/authz/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                        .loginPage("/oauth2/authorization/discord")   // login unique Discord pour l’instant
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/error")
                )
                .httpBasic(Customizer.withDefaults())
                .logout(lo -> lo.logoutUrl("/logout").logoutSuccessUrl("/")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.localhost",
                "https://*.tsumiyoku.org"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Authorization"));
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
