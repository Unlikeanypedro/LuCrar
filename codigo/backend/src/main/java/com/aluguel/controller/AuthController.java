package com.aluguel.controller;

import com.aluguel.dto.Dtos.RegistroDTO;
import com.aluguel.model.Usuario;
import com.aluguel.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioService usuarios;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario registrar(@Valid @RequestBody RegistroDTO d) {
        return usuarios.registrar(d);
    }

    /** Login: o front envia Basic Auth e usa este endpoint para validar as credenciais. */
    @GetMapping("/me")
    public Usuario me(Authentication auth) {
        return usuarios.atual(auth.getName());
    }
}
