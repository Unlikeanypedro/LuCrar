package com.aluguel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/** Crédito concedido por um banco agente para o aluguel. */
@Entity
@Table(name = "contrato_credito")
@Getter
@Setter
public class ContratoCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_aluguel_id")
    private ContratoAluguel contratoAluguel;

    @Column(name = "valor_financiado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorFinanciado;

    @Column(name = "taxa_juros_mensal", precision = 5, scale = 2)
    private BigDecimal taxaJurosMensal;

    private Integer parcelas;

    /** Legacy column retained by the deployed schema; new contracts use valor_financiado. */
    @Column(precision = 12, scale = 2)
    private BigDecimal valor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banco_id", nullable = false)
    private Agente banco;
}
