package com.aluguel.controller;

import com.aluguel.dto.Dtos.AutomovelDTO;
import com.aluguel.dto.Dtos.AutomovelRespostaDTO;
import com.aluguel.model.Automovel;
import com.aluguel.model.Papel;
import com.aluguel.model.Usuario;
import com.aluguel.repository.AutomovelRepository;
import com.aluguel.repository.ClienteRepository;
import com.aluguel.service.UsuarioService;
import jakarta.validation.Valid;
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
    private final ClienteRepository clientes;

    @GetMapping
    public List<AutomovelRespostaDTO> listar() {
        return repo.findAll().stream().map(this::toResposta).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AutomovelRespostaDTO criar(
        @Valid @RequestBody AutomovelDTO dto,
        Authentication auth
    ) {
        Usuario solicitante = usuarios.atual(auth.getName());
        if (repo.existsByMatriculaIgnoreCase(dto.matricula())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Matrícula já cadastrada"
            );
        }
        if (repo.existsByPlacaIgnoreCase(dto.placa())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Placa já cadastrada"
            );
        }
        Usuario proprietario = solicitante;
        Automovel.Proprietario categoria = categoriaDe(solicitante.getPapel());
        Long proprietarioClienteId = null;
        Long proprietarioAgenteId = null;

        if (solicitante.getPapel() == Papel.CLIENTE) {
            if (
                dto.proprietario() != null &&
                dto.proprietario() != Automovel.Proprietario.CLIENTE
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cliente só pode cadastrar automóvel em seu próprio nome"
                );
            }
            proprietarioClienteId = clientes
                .findByUsuario(solicitante)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cadastro de cliente não encontrado"
                    )
                )
                .getId();
        } else {
            if (dto.proprietarioId() != null) {
                proprietario = usuarios
                    .findById(dto.proprietarioId())
                    .orElseThrow(() ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Proprietário não encontrado"
                        )
                    );
            }
            categoria = categoriaDe(proprietario.getPapel());
            if (
                dto.proprietario() != null &&
                dto.proprietario() != categoria
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A categoria do automóvel deve corresponder ao perfil do proprietário"
                );
            }
            if (proprietario.getPapel() == Papel.CLIENTE) {
                proprietarioClienteId = clientes
                    .findByUsuario(proprietario)
                    .orElseThrow(() ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Cadastro de cliente não encontrado"
                        )
                    )
                    .getId();
            } else {
                proprietarioAgenteId = proprietario.getId();
            }
        }

        var a = new Automovel();
        a.setMatricula(dto.matricula());
        a.setAno(dto.ano());
        a.setMarca(dto.marca());
        a.setModelo(dto.modelo());
        a.setPlaca(dto.placa());
        a.setValorDiaria(dto.valorDiaria());
        a.setProprietario(categoria);
        a.setProprietarioClienteId(proprietarioClienteId);
        a.setProprietarioAgenteId(proprietarioAgenteId);
        a.setProprietarioUsuario(proprietario);
        return toResposta(repo.save(a));
    }

    private Automovel.Proprietario categoriaDe(Papel papel) {
        return switch (papel) {
            case CLIENTE -> Automovel.Proprietario.CLIENTE;
            case EMPRESA -> Automovel.Proprietario.EMPRESA;
            case BANCO -> Automovel.Proprietario.BANCO;
        };
    }

    private Long idProprietario(Automovel a) {
        if (
            a.getProprietario() == Automovel.Proprietario.CLIENTE &&
            a.getProprietarioClienteId() != null
        ) {
            return a.getProprietarioClienteId();
        }
        if (
            (a.getProprietario() == Automovel.Proprietario.EMPRESA ||
                a.getProprietario() == Automovel.Proprietario.BANCO) &&
            a.getProprietarioAgenteId() != null
        ) {
            return a.getProprietarioAgenteId();
        }
        return a.getProprietarioUsuario() != null
            ? a.getProprietarioUsuario().getId()
            : null;
    }

    private AutomovelRespostaDTO toResposta(Automovel a) {
        return new AutomovelRespostaDTO(
            a.getId(),
            a.getMatricula(),
            a.getAno(),
            a.getMarca(),
            a.getModelo(),
            a.getPlaca(),
            a.getValorDiaria(),
            a.getProprietario(),
            idProprietario(a)
        );
    }
}
