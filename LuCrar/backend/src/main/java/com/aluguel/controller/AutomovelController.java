package com.aluguel.controller;

import com.aluguel.model.Automovel;
import com.aluguel.repository.AutomovelRepository;
import com.aluguel.service.UsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/automoveis")
@RequiredArgsConstructor
public class AutomovelController {
    private final AutomovelRepository repo;
    private final UsuarioService usuarios;

    @GetMapping
    public List<Automovel> listar() {
        return repo.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Automovel criar(@RequestBody Automovel a, Authentication auth) {
        if (!usuarios.atual(auth.getName()).getPapel().isAgente()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas agentes cadastram automóveis");
        }
        a.setId(null);
        return repo.save(a);
    }
}
