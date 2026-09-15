package com.testeBanda.testador.config;

import com.testeBanda.testador.api.GlpiAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AutenticacaoConfig implements AuthenticationProvider {
    @Value("${testador.localUsername}")
    private String localUsername;
    @Value("${testador.localPassword}")
    private String localPassword;

    @Autowired
    private GlpiAPI glpiAPI;

    //Atualmente temos 2 modos de entrar, userLocal definido no properties e pelo GLPI
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new BadCredentialsException("Usuário e senha obrigatórios");
        }

        if ( username.equals(localUsername) && password.equals(localPassword) ) {
            log.info("USUÁRIO LOCAL LOGADO COM SUCESSO");
            return new UsernamePasswordAuthenticationToken(username, password, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        String glpiToken = glpiAPI.logar(username,password);
        if (glpiToken.equals("200")) {
            log.info("LOGADO VIA GLPI" + username );
            return new UsernamePasswordAuthenticationToken(username, password, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }
        else{
            throw new BadCredentialsException("Usuário e senha fora da base de dados");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}
