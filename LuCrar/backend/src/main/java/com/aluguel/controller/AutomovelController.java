package com.aluguel.controller;

import com.aluguel.dto.Dtos.AutomovelDTO;
import com.aluguel.dto.Dtos.AutomovelRespostaDTO;
import com.aluguel.model.Automovel;
import com.aluguel.model.Usuario;
import com.aluguel.repository.AutomovelRepository;
import com.aluguel.repository.UsuarioRepository;
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
        Usuario agente = usuarios.atual(auth.getName());
        if (!agente.getPapel().isAgente()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Apenas agentes cadastram automóveis"
            );
        }
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
        Usuario proprietario = usuarios
            .findById(dto.proprietarioId())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Proprietário não encontrado"
                )
            );
        if (
            proprietario.getPapel() == com.aluguel.model.Papel.CLIENTE &&
            dto.proprietario() !=
                com.aluguel.model.Automovel.Proprietario.CLIENTE
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Proprietário cliente deve ter categoria CLIENTE"
            );
        }
        if (
            proprietario.getPapel() == com.aluguel.model.Papel.EMPRESA &&
            dto.proprietario() !=
                com.aluguel.model.Automovel.Proprietario.EMPRESA
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Proprietário empresa deve ter categoria EMPRESA"
            );
        }
        if (
            proprietario.getPapel() == com.aluguel.model.Papel.BANCO &&
            dto.proprietario() != com.aluguel.model.Automovel.Proprietario.BANCO
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Proprietário banco deve ter categoria BANCO"
            );
        }
        var a = new Automovel();
        a.setMatricula(dto.matricula());
        a.setAno(dto.ano());
        a.setMarca(dto.marca());
        a.setModelo(dto.modelo());
        a.setPlaca(dto.placa());
        a.setValorDiaria(dto.valorDiaria());
        a.setProprietario(dto.proprietario());
        a.setProprietarioUsuario(proprietario);
        return toResposta(repo.save(a));
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
