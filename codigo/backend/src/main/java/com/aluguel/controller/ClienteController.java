package com.aluguel.controller;

import com.aluguel.dto.Dtos.ClienteDTO;
import com.aluguel.model.Cliente;
import com.aluguel.service.ClienteService;
import com.aluguel.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** CRUD de cliente (o Create acontece no cadastro: POST /api/auth/register). */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService service;
    private final UsuarioService usuarios;

    @GetMapping
    public List<Cliente> listar(Authentication a) {
        return service.listar(usuarios.atual(a.getName()));
    }

    @GetMapping("/{id}")
    public Cliente buscar(@PathVariable Long id, Authentication a) {
        return service.buscar(id, usuarios.atual(a.getName()));
    }

    @PutMapping("/{id}")
    public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO d, Authentication a) {
        return service.atualizar(id, d, usuarios.atual(a.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, Authentication a) {
        service.excluir(id, usuarios.atual(a.getName()));
    }
}
