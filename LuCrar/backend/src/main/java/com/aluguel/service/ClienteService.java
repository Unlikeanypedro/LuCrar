package com.aluguel.service;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.aluguel.dto.Dtos.ClienteDTO;
import com.aluguel.model.Cliente;
import com.aluguel.model.Papel;
import com.aluguel.model.Usuario;
import com.aluguel.repository.ClienteRepository;
import com.aluguel.repository.PedidoRepository;
import com.aluguel.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final PedidoRepository pedidos;

    public Cliente criar(Usuario usuario, ClienteDTO d) {
        var c = new Cliente();
        c.setUsuario(usuario);
        return clientes.save(aplicar(c, d));
    }

    public List<Cliente> listar(Usuario u) {
        if (u.getPapel() == Papel.CLIENTE) {
            return clientes.findByUsuario(u).map(List::of).orElse(List.of());
        }
        return clientes.findAll();
    }

    public Cliente buscar(Long id, Usuario u) {
        var c = clientes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cliente não encontrado"));
        if (u.getPapel() == Papel.CLIENTE && !c.getUsuario().getId().equals(u.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Acesso negado");
        }
        return c;
    }

    @Transactional
    public Cliente atualizar(Long id, ClienteDTO d, Usuario u) {
        return clientes.save(aplicar(buscar(id, u), d));
    }

    @Transactional
    public void excluir(Long id, Usuario u) {
        var c = buscar(id, u);
        pedidos.deleteByCliente(c);
        clientes.delete(c);
        usuarios.delete(c.getUsuario());
    }

    private Cliente aplicar(Cliente c, ClienteDTO d) {
        c.setNome(d.nome());
        c.setRg(d.rg());
        c.setCpf(d.cpf());
        c.setEndereco(d.endereco());
        c.setProfissao(d.profissao());
        c.setRendimentos(d.rendimentos() == null ? new ArrayList<>() : new ArrayList<>(d.rendimentos()));
        return c;
    }
}
