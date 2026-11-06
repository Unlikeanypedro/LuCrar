package com.aluguel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Crédito concedido por um banco agente para o aluguel. */
@Entity
@Getter
@Setter
public class ContratoCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal valor;

    @ManyToOne(optional = false)
    private Usuario banco;
}
