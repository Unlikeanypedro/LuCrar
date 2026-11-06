package com.aluguel.service;

import static com.aluguel.model.StatusPedido.*;
import static org.springframework.http.HttpStatus.*;

import com.aluguel.dto.Dtos.AvaliacaoDTO;
import com.aluguel.dto.Dtos.PedidoDTO;
import com.aluguel.model.*;
import com.aluguel.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final AutomovelRepository automoveis;

    public PedidoAluguel criar(PedidoDTO d, Usuario u) {
        if (u.getPapel() != Papel.CLIENTE) throw erro(
            FORBIDDEN,
            "Apenas clientes introduzem pedidos"
        );
        var p = new PedidoAluguel();
        p.setCliente(
            clientes
                .findByUsuario(u)
                .orElseThrow(() -> erro(NOT_FOUND, "Cliente não cadastrado"))
        );
        return pedidos.save(aplicar(p, d));
    }

    public List<PedidoAluguel> listar(Usuario u) {
        if (u.getPapel().isAgente()) return pedidos.findAll();
        return clientes
            .findByUsuario(u)
            .map(pedidos::findByCliente)
            .orElse(List.of());
    }

    public PedidoAluguel buscar(Long id, Usuario u) {
        var p = pedidos
            .findById(id)
            .orElseThrow(() -> erro(NOT_FOUND, "Pedido não encontrado"));
        if (
            !u.getPapel().isAgente() &&
            !p.getCliente().getUsuario().getId().equals(u.getId())
        ) {
            throw erro(FORBIDDEN, "Acesso negado");
        }
        return p;
    }

    public PedidoAluguel modificar(Long id, PedidoDTO d, Usuario u) {
        var p = buscar(id, u);
        if (p.getStatus() == CANCELADO || p.getStatus() == CONTRATADO) {
            throw erro(CONFLICT, "Pedido não pode mais ser modificado");
        }
        if (!u.getPapel().isAgente() && p.getStatus() != PENDENTE) {
            throw erro(CONFLICT, "Cliente só modifica pedidos pendentes");
        }
        aplicar(p, d);
        if (u.getPapel().isAgente() && p.getStatus() != PENDENTE) {
            p.setStatus(PENDENTE);
            p.setCredito(null);
        }
        return pedidos.save(p);
    }

    public PedidoAluguel cancelar(Long id, Usuario u) {
        if (u.getPapel() != Papel.CLIENTE) {
            throw erro(FORBIDDEN, "Apenas clientes podem cancelar pedidos");
        }
        var p = buscar(id, u);
        if (p.getStatus() == CONTRATADO) throw erro(
            CONFLICT,
            "Contrato já executado"
        );
        if (p.getStatus() == CANCELADO) throw erro(
            CONFLICT,
            "Pedido já cancelado"
        );
        p.setStatus(CANCELADO);
        return pedidos.save(p);
    }

    /** Análise financeira feita por um agente. */
    public PedidoAluguel avaliar(Long id, AvaliacaoDTO d, Usuario u) {
        var p = agentePedido(id, u, PENDENTE);
        if (
            d.valorCredito() != null &&
            (u.getPapel() != Papel.BANCO || !Boolean.TRUE.equals(d.aprovado()))
        ) {
            throw erro(
                BAD_REQUEST,
                "Somente um banco pode conceder crédito ao aprovar um pedido"
            );
        }
        if (d.valorCredito() != null && d.valorCredito().signum() <= 0) {
            throw erro(BAD_REQUEST, "O valor do crédito deve ser positivo");
        }
        if (Boolean.TRUE.equals(d.aprovado())) {
            p.setStatus(APROVADO);
            if (u.getPapel() == Papel.BANCO && d.valorCredito() != null) {
                var cc = new ContratoCredito();
                cc.setBanco(u);
                cc.setValor(d.valorCredito());
                p.setCredito(cc);
            }
        } else {
            p.setStatus(REJEITADO);
        }
        return pedidos.save(p);
    }

    /** Execução do contrato após parecer positivo. */
    public PedidoAluguel contratar(Long id, Usuario u) {
        var p = agentePedido(id, u, APROVADO);
        p.setStatus(CONTRATADO);
        return pedidos.save(p);
    }

    private PedidoAluguel agentePedido(
        Long id,
        Usuario u,
        StatusPedido esperado
    ) {
        if (!u.getPapel().isAgente()) throw erro(FORBIDDEN, "Apenas agentes");
        var p = buscar(id, u);
        if (p.getStatus() != esperado) throw erro(
            CONFLICT,
            "Pedido deve estar " + esperado
        );
        return p;
    }

    private PedidoAluguel aplicar(PedidoAluguel p, PedidoDTO d) {
        if (!d.dataFim().isAfter(d.dataInicio())) throw erro(
            BAD_REQUEST,
            "A data final deve ser posterior à inicial"
        );
        p.setAutomovel(
            automoveis
                .findById(d.automovelId())
                .orElseThrow(() -> erro(NOT_FOUND, "Automóvel não encontrado"))
        );
        var statusesQueOcupamVeiculo = List.of(PENDENTE, APROVADO, CONTRATADO);
        long conflitos = p.getId() == null
            ? pedidos.contarConflitosDePeriodo(
                p.getAutomovel().getId(),
                statusesQueOcupamVeiculo,
                d.dataInicio(),
                d.dataFim()
            )
            : pedidos.contarConflitosDePeriodoExcetoPedido(
                p.getAutomovel().getId(),
                statusesQueOcupamVeiculo,
                d.dataInicio(),
                d.dataFim(),
                p.getId()
            );
        boolean periodoOcupado = conflitos > 0;
        if (periodoOcupado) throw erro(
            CONFLICT,
            "Este automóvel já possui um pedido para o período informado"
        );
        var valorDiaria = p.getAutomovel().getValorDiaria();
        if (valorDiaria == null || valorDiaria.signum() <= 0) throw erro(
            BAD_REQUEST,
            "Este automóvel não possui uma diária válida cadastrada"
        );
        long diarias = ChronoUnit.DAYS.between(d.dataInicio(), d.dataFim());
        p.setValorTotal(
            valorDiaria
                .multiply(BigDecimal.valueOf(diarias))
                .setScale(2, RoundingMode.HALF_UP)
        );
        p.setDataInicio(d.dataInicio());
        p.setDataFim(d.dataFim());
        return p;
    }

    private static ResponseStatusException erro(HttpStatus s, String m) {
        return new ResponseStatusException(s, m);
    }
}
