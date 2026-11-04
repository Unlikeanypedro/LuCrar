package com.aluguel.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.aluguel.dto.Dtos.RegistroDTO;
import com.aluguel.model.Papel;
import com.aluguel.model.Usuario;
import com.aluguel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final ClienteService clientes;
    private final PasswordEncoder encoder;

    @Transactional
    public Usuario registrar(RegistroDTO d) {
        if (usuarios.findByLogin(d.login()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Login já cadastrado");
        }
        if (d.papel() == Papel.CLIENTE && d.cliente() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Dados do cliente são obrigatórios");
        }
        var u = new Usuario();
        u.setLogin(d.login());
        u.setSenha(encoder.encode(d.senha()));
        u.setPapel(d.papel());
        usuarios.save(u);
        if (d.papel() == Papel.CLIENTE) {
            clientes.criar(u, d.cliente());
        }
        return u;
    }

    public Usuario atual(String login) {
        return usuarios.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Usuário inválido"));
    }
}
