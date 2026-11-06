package com.aluguel.controller;

import com.aluguel.dto.Dtos.AvaliacaoDTO;
import com.aluguel.dto.Dtos.PedidoDTO;
import com.aluguel.model.PedidoAluguel;
import com.aluguel.service.PedidoService;
import com.aluguel.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;
    private final UsuarioService usuarios;

    @GetMapping
    public List<PedidoAluguel> listar(Authentication a) {
        return service.listar(usuarios.atual(a.getName()));
    }

    @GetMapping("/{id}")
    public PedidoAluguel buscar(@PathVariable Long id, Authentication a) {
        return service.buscar(id, usuarios.atual(a.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoAluguel criar(
        @Valid @RequestBody PedidoDTO d,
        Authentication a
    ) {
        return service.criar(d, usuarios.atual(a.getName()));
    }

    @PutMapping("/{id}")
    public PedidoAluguel modificar(
        @PathVariable Long id,
        @Valid @RequestBody PedidoDTO d,
        Authentication a
    ) {
        return service.modificar(id, d, usuarios.atual(a.getName()));
    }

    /** Cancela o pedido (status CANCELADO). */
    @DeleteMapping("/{id}")
    public PedidoAluguel cancelar(@PathVariable Long id, Authentication a) {
        return service.cancelar(id, usuarios.atual(a.getName()));
    }

    @PostMapping("/{id}/avaliacao")
    public PedidoAluguel avaliar(
        @PathVariable Long id,
        @Valid @RequestBody AvaliacaoDTO d,
        Authentication a
    ) {
        return service.avaliar(id, d, usuarios.atual(a.getName()));
    }

    @PostMapping("/{id}/contrato")
    public PedidoAluguel contratar(@PathVariable Long id, Authentication a) {
        return service.contratar(id, usuarios.atual(a.getName()));
    }
}
