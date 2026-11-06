
package com.aluguel.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.aluguel.dto.Dtos.RegistroDTO;
import com.aluguel.model.Papel;
import com.aluguel.model.Perfil;
import com.aluguel.model.Agente;
import com.aluguel.model.Usuario;
import com.aluguel.repository.AgenteRepository;
import com.aluguel.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final AgenteRepository agentes;
    private final ClienteService clientes;
    private final PasswordEncoder encoder;

    @Value("${app.security.agent-registration-key:}")
    private String agentRegistrationKey;

    @Transactional
    public Usuario registrar(RegistroDTO d) {
        if (usuarios.findByLogin(d.login()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Login já cadastrado");
        }

        if (d.papel() == Papel.CLIENTE && d.cliente() == null) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Dados do cliente são obrigatórios"
            );
        }

        if (d.papel() != Papel.CLIENTE) {
            if (d.cliente() != null) {
                throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Agentes não devem enviar dados de cliente"
                );
            }

            if (
                agentRegistrationKey == null ||
                agentRegistrationKey.isBlank() ||
                d.codigoAgente() == null ||
                !MessageDigest.isEqual(
                    agentRegistrationKey.getBytes(StandardCharsets.UTF_8),
                    d.codigoAgente().getBytes(StandardCharsets.UTF_8)
                )
            ) {
                throw new ResponseStatusException(
                    FORBIDDEN,
                    "Cadastro de agente não autorizado"
                );
            }
            if (d.agente() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Dados da empresa ou banco são obrigatórios");
            }
        }

        var u = new Usuario();
        u.setLogin(d.login());
        u.setSenha(encoder.encode(d.senha()));
        u.setPapel(d.papel());
        u.setPerfil(d.papel() == Papel.CLIENTE ? Perfil.CLIENTE : Perfil.AGENTE);

        usuarios.save(u);

        if (d.papel() == Papel.CLIENTE) {
            clientes.criar(u, d.cliente());
        } else {
            var agente = new Agente();
            agente.setUsuario(u);
            agente.setTipo(d.papel());
            agente.setNome(d.agente().nome());
            agente.setCnpj(d.agente().cnpj());
            agentes.save(agente);
        }

        return u;
    }

    public Usuario atual(String login) {
        return usuarios
            .findByLogin(login)
            .orElseThrow(() ->
                new ResponseStatusException(UNAUTHORIZED, "Usuário inválido")
            );
    }

    public java.util.Optional<Usuario> findById(Long id) {
        return usuarios.findById(id);
    }
}
