package com.aluguel.config;

import com.aluguel.model.Automovel;
import com.aluguel.repository.AutomovelRepository;
import com.aluguel.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.UUID;

/** Login obrigatório (usuário e senha via HTTP Basic, senha com BCrypt). Só o cadastro é público. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(a ->
                a
                    .requestMatchers(HttpMethod.POST, "/api/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            // 401 sem WWW-Authenticate: evita o pop-up de login do navegador
            .httpBasic(b ->
                b.authenticationEntryPoint((req, res, ex) ->
                    res.sendError(401, "Não autenticado")
                )
            )
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
        UsuarioRepository repo,
        PasswordEncoder passwordEncoder
    ) {
        // Old rows can have no password after the compatibility migration. Give
        // them a random, unshared hash so Basic auth fails cleanly instead of
        // throwing while Spring builds UserDetails with a null password.
        String unusableLegacyPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        return login ->
            repo
                .findByLogin(login)
                .map(u ->
                    User.withUsername(u.getLogin())
                        .password(u.getSenha() == null ? unusableLegacyPassword : u.getSenha())
                        .roles(u.getPapel().name())
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException(login));
    }

    /** Catálogo inicial de automóveis para o protótipo. */
    @Bean
    CommandLineRunner dadosIniciais(AutomovelRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            String[][] carros = {
                { "Fiat", "Argo", "2023", "ABC1D23" },
                { "VW", "Polo", "2024", "BRA2E19" },
                { "Chevrolet", "Onix", "2022", "QWE3F45" },
            };
            int i = 1;
            for (var c : carros) {
                var a = new Automovel();
                a.setMarca(c[0]);
                a.setModelo(c[1]);
                a.setAno(Integer.parseInt(c[2]));
                a.setPlaca(c[3]);
                a.setMatricula("MAT-" + i++);
                repo.save(a);
            }
        };
    }
}
