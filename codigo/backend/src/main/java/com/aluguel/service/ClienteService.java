package com.aluguel.service;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.aluguel.dto.Dtos.ClienteDTO;
import com.aluguel.model.Cliente;
import com.aluguel.model.Papel;
import com.aluguel.model.Usuario;
import com.aluguel.repository.AutomovelRepository;
import com.aluguel.repository.ClienteRepository;
import com.aluguel.repository.ContratoAluguelRepository;
import com.aluguel.repository.ContratoCreditoRepository;
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
    private final ContratoAluguelRepository contratos;
    private final ContratoCreditoRepository creditos;
    private final AutomovelRepository automoveis;

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

    /**
     * Exclui o cadastro do cliente (HU03) removendo antes o que referencia a
     * conta: crédito, contrato e pedidos. Sem essa ordem as chaves estrangeiras
     * de contrato_aluguel/contrato_credito quebram a exclusão.
     */
    @Transactional
    public void excluir(Long id, Usuario u) {
        var c = buscar(id, u);
        for (var pedido : pedidos.findByCliente(c)) {
            var credito = pedido.getCredito();
            if (credito != null) {
                pedido.setCredito(null);
                pedidos.save(pedido);
                creditos.delete(credito);
                creditos.flush();
            }
            contratos.findByPedidoId(pedido.getId()).ifPresent(contrato -> {
                contratos.delete(contrato);
                contratos.flush();
            });
            pedidos.delete(pedido);
            pedidos.flush();
        }
        // Veículos do cliente continuam no catálogo, mas sem vínculo com a
        // conta removida (a FK automovel.proprietario_usuario_id bloquearia).
        automoveis.findByProprietarioUsuario(c.getUsuario()).forEach(a -> {
            a.setProprietarioUsuario(null);
            automoveis.save(a);
        });
        automoveis.flush();
        clientes.delete(c);
        usuarios.delete(c.getUsuario());
    }

    private Cliente aplicar(Cliente c, ClienteDTO d) {
        c.setNome(d.nome());
        // Some deployed schemas still constrain `rg` to NOT NULL. Keep the
        // field optional at the API while storing an empty value for those DBs.
        c.setRg(d.rg() == null ? "" : d.rg());
        c.setCpf(d.cpf());
        c.setEndereco(d.endereco());
        c.setProfissao(d.profissao());
        c.setRendimentos(d.rendimentos() == null ? new ArrayList<>() : new ArrayList<>(d.rendimentos()));
        return c;
    }
}
