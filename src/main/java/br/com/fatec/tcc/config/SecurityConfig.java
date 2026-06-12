package br.com.fatec.tcc.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Arrays;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.remember-me.key}")
    private String rememberMeKey;

    @Value("${cors.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    /** Converte origens HTTP/HTTPS em equivalentes WS/WSS para o connect-src do CSP. */
    private String buildWsOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .map(o -> o.replace("https://", "wss://").replace("http://", "ws://"))
                .collect(Collectors.joining(" "));
    }

    private static final String[] PUBLIC_PATHS = {
            "/",
            "/login",
            "/cadastro",
            "/cadastro/**",
            "/api/auth/register",
            "/api/auth/login",
            "/css/**",
            "/js/**",
            "/images/**",
            "/webjars/**",
            "/favicon.ico",
            "/favicon.svg",
            "/actuator/health",
            "/ws/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Spring Security 6 usa XorCsrfTokenRequestAttributeHandler por padrão,
                        // que aplica XOR no token antes de validar. O frontend lê o cookie bruto
                        // e envia como X-XSRF-TOKEN — a validação sempre falha sem este handler.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/ws/**")
                )
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .frameOptions(frame -> frame.sameOrigin())
                        // 'unsafe-inline' nos scripts é necessário enquanto os templates Thymeleaf usarem
                        // blocos <script> inline. A remoção exige migrar todos os scripts para
                        // arquivos externos (/js/) e adicionar nonces gerados por request.
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                        "img-src 'self' data: https://placehold.co " +
                                              "https://*.tile.openstreetmap.org " +
                                              "https://*.openstreetmap.org; " +
                                        "connect-src 'self' https://*.tile.openstreetmap.org " +
                                              "https://cdn.jsdelivr.net " +
                                              buildWsOrigins() + "; " +
                                        "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net;")
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/painel/**").hasAnyRole("ADMIN", "MODERADOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        // FIX 1: Redirecionar para /dashboard após login bem-sucedido
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?erro=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(3 * 24 * 60 * 60)
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?sessao-expirada=true")
                )
                // Clientes REST (Accept: application/json) recebem 401 em vez de redirect para login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            String accept = request.getHeader("Accept");
                            boolean isApiRequest = request.getRequestURI().startsWith("/api/")
                                    || (accept != null && accept.contains("application/json"));
                            if (isApiRequest) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}