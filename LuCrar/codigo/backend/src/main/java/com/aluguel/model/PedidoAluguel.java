package com.aluguel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PedidoAluguel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    private Automovel automovel;

    private LocalDate dataInicio;
    private LocalDate dataFim;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    private StatusPedido status = StatusPedido.PENDENTE;

    /** ID do agente (tabela agente) que assumiu a análise; nulo na fila aberta. */
    @Column(name = "agente_id")
    private Long agenteId;

    /** Opcional: contrato de crédito concedido por um banco agente. */
    @OneToOne(cascade = CascadeType.ALL)
    private ContratoCredito credito;
}
