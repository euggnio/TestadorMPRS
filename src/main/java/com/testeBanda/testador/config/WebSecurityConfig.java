package com.testeBanda.testador.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final AutenticacaoConfig autenticacaoConfig;

    public WebSecurityConfig(AutenticacaoConfig autenticacaoConfig) {
        this.autenticacaoConfig = autenticacaoConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
               .authenticationProvider(autenticacaoConfig)
               .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(
                                "/",
                                "/grafico",
                                "/grafico/**",
                                "/error",
                                "/snmpWan",
                                "/versao",
                                "/pegarGraficoSmoke/*"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/configuracao").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .permitAll()
                        .loginPage("/login")
                        .defaultSuccessUrl("/cidadesBanda", true)
                        .failureUrl("/login?error")
                )
                .csrf(csrf -> csrf.disable())
                .logout((logout) ->
                        logout.permitAll()
                                .deleteCookies("JSESSIONID")
                                .logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return (web -> web.ignoring()
                .requestMatchers(
                        "/style.css"
                        ,"/pegarGraficoSmoke/*"
                        ,"/static/**"
                        ,"/imagens/**"
                        ,"/img.png"
                        ,"/backgroundLogin1.png"
                        ,"/backgroundLogin2.jpg"
                        ,"/backgroundLogin3.png"
                        ,"/grafico"
                        ,"/versao"
                        ,"/grafico/**"
                        ,"/snmpWan",
                        "/",
                        "/javascript.js",
                        "/graficosjs/**",
                        "/graficosCalendario.js",
                        "/graficos2.css",
                        "/favicon.ico"
                ));
    }
}