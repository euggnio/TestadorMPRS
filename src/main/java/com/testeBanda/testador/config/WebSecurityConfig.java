package com.testeBanda.testador.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(HttpMethod.POST, "/configuracao").permitAll()
                        .requestMatchers("/", "/grafico", "/error").permitAll()
                                .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .permitAll()
                        .loginPage("/login")
                        .defaultSuccessUrl("/cidadesBanda", true)
                )
                .csrf(csrf -> csrf.disable())
                .logout((logout) ->
                        logout.permitAll()
                                .deleteCookies("JSESSIONID"));
        return http.build();
    }


    //funcional apenas em ambiente de testes
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user =
                User.withDefaultPasswordEncoder() // apenas para testes
                        .username("testador")
                        .password("adm123")
                        .roles("USER")
                        .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return (web -> web.ignoring().requestMatchers("/style.css","/static/**","/img.png","/backgroundLogin1.jpg"));
    }
}
