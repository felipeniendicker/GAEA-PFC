package Gaea.Api.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtFilter jwtFilter,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
            List<String> allowedOrigins) {

        this.jwtFilter = jwtFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                allowedOrigins
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(401, "Autenticação necessária"))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(403, "Acesso negado"))
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ativacoes/aluno").permitAll()
                        .requestMatchers("/api/instituicoes/alunos/**").hasRole("INSTITUICAO")
                        .requestMatchers(HttpMethod.POST, "/api/processos").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/processos/meus").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.GET, "/api/processos").hasAnyRole("INSTITUICAO", "EMPRESA")
                        .requestMatchers(HttpMethod.GET, "/api/processos/*/historico")
                        .hasAnyRole("ALUNO", "INSTITUICAO", "EMPRESA")
                        .requestMatchers("/api/dashboard/**").hasRole("INSTITUICAO")
                        .requestMatchers("/api/processos/**").hasRole("INSTITUICAO")
                        .requestMatchers("/api/auditoria/**").hasRole("INSTITUICAO")
                        .requestMatchers("/api/cep/**").hasRole("INSTITUICAO")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
