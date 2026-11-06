package com.aluguel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banco_id", nullable = false)
    private Agente banco;
}
