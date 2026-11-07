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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final AutomovelRepository automoveis;
    private final AgenteRepository agentes;
    private final ContratoCreditoRepository creditos;
    private final ContratoAluguelRepository contratos;

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
        if (u.getPapel().isAgente()) {
            Long agenteId = agentes.findByUsuarioId(u.getId())
                .map(Agente::getId)
                .orElse(null);
            return agenteId == null
                ? pedidos.findByStatusOrderByIdDesc(PENDENTE)
                : pedidos.findByStatusOrAgenteIdOrderByIdDesc(PENDENTE, agenteId);
        }
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
        if (u.getPapel().isAgente()) validarAgenteResponsavel(p, u);
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
            p.setValorCreditoAprovado(null);
            p.setTaxaJurosMensalAprovada(null);
            p.setParcelasAprovadas(null);
            p.setParecerFinanceiro(null);
            p.setAgenteId(null);
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
    @Transactional
    public PedidoAluguel avaliar(Long id, AvaliacaoDTO d, Usuario u) {
        exigirAgente(u);
        var p = pedidos.findById(id).orElseThrow(() -> erro(NOT_FOUND, "Pedido não encontrado"));
        if (p.getStatus() == PENDENTE) {
            assumirSeLivre(p, u);
            p.setStatus(EM_ANALISE);
        } else if (p.getStatus() == EM_ANALISE) {
            validarAgenteResponsavel(p, u);
        } else {
            throw erro(CONFLICT, "Pedido deve estar PENDENTE ou EM_ANALISE");
        }
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
                p.setValorCreditoAprovado(d.valorCredito());
                p.setTaxaJurosMensalAprovada(d.taxaJurosMensal() != null
                    ? d.taxaJurosMensal() : new BigDecimal("1.99"));
                p.setParcelasAprovadas(d.parcelas() != null ? d.parcelas() : 6);
            } else {
                p.setValorCreditoAprovado(null);
                p.setTaxaJurosMensalAprovada(null);
                p.setParcelasAprovadas(null);
            }
        } else {
            p.setStatus(REPROVADO);
            p.setValorCreditoAprovado(null);
            p.setTaxaJurosMensalAprovada(null);
            p.setParcelasAprovadas(null);
        }
        p.setParecerFinanceiro(d.parecerFinanceiro().trim());
        return pedidos.save(p);
    }

    /** Execução do contrato após parecer positivo. */
    @Transactional
    public PedidoAluguel contratar(Long id, Usuario u) {
        var p = agentePedido(id, u, APROVADO);
        validarAgenteResponsavel(p, u);

        var contrato = new ContratoAluguel();
        contrato.setPedido(p);
        contrato.setTipoPropriedade(p.getAutomovel().getProprietario());
        contrato.setDataAssinatura(java.time.LocalDate.now());
        contrato.setValorTotal(p.getValorTotal());
        contrato.setStatus("ATIVO");
        contrato = contratos.save(contrato);

        if (p.getValorCreditoAprovado() != null) {
            var banco = agentes.findByUsuarioId(u.getId())
                .filter(agente -> agente.getTipo() == Papel.BANCO)
                .orElseThrow(() -> erro(NOT_FOUND, "Cadastro do banco não encontrado"));
            var credito = new ContratoCredito();
            credito.setContratoAluguel(contrato);
            credito.setBanco(banco);
            credito.setValorFinanciado(p.getValorCreditoAprovado());
            credito.setTaxaJurosMensal(p.getTaxaJurosMensalAprovada());
            credito.setParcelas(p.getParcelasAprovadas());
            p.setCredito(creditos.save(credito));
        }
        p.setContrato(contrato);
        p.setStatus(CONTRATADO);
        return pedidos.save(p);
    }

    /** Assume um pedido pendente para impedir duas empresas de analisarem ao mesmo tempo. */
    public PedidoAluguel assumir(Long id, Usuario u) {
        exigirAgente(u);
        var p = pedidos.findById(id).orElseThrow(() -> erro(NOT_FOUND, "Pedido não encontrado"));
        if (p.getStatus() != PENDENTE) throw erro(CONFLICT, "Somente pedidos pendentes podem ser assumidos");
        assumirSeLivre(p, u);
        p.setStatus(EM_ANALISE);
        return pedidos.save(p);
    }

    private PedidoAluguel agentePedido(
        Long id,
        Usuario u,
        StatusPedido esperado
    ) {
        exigirAgente(u);
        var p = buscar(id, u);
        if (p.getStatus() != esperado) throw erro(
            CONFLICT,
            "Pedido deve estar " + esperado
        );
        return p;
    }

    private void exigirAgente(Usuario u) {
        if (!u.getPapel().isAgente()) throw erro(FORBIDDEN, "Apenas agentes");
    }

    private Long idAgente(Usuario u) {
        return agentes.findByUsuarioId(u.getId())
            .map(Agente::getId)
            .orElseThrow(() -> erro(NOT_FOUND, "Cadastro de agente não encontrado"));
    }

    private void assumirSeLivre(PedidoAluguel p, Usuario u) {
        Long agenteId = idAgente(u);
        if (p.getAgenteId() != null && !p.getAgenteId().equals(agenteId)) {
            throw erro(FORBIDDEN, "Este pedido já está em análise por outro agente");
        }
        p.setAgenteId(agenteId);
    }

    private void validarAgenteResponsavel(PedidoAluguel p, Usuario u) {
        Long agenteId = idAgente(u);
        if (p.getAgenteId() == null || !p.getAgenteId().equals(agenteId)) {
            throw erro(FORBIDDEN, "Somente o agente responsável pode tratar este pedido");
        }
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
        var statusesQueOcupamVeiculo = List.of(PENDENTE, EM_ANALISE, APROVADO, CONTRATADO);
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
