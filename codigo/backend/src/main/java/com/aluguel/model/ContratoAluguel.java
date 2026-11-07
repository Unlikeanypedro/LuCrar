package com.aluguel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Contrato executado para um pedido aprovado. */
@Entity
@Table(name = "contrato_aluguel")
@Getter
@Setter
public class ContratoAluguel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private PedidoAluguel pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_propriedade", nullable = false)
    private Automovel.Proprietario tipoPropriedade;

    @Column(name = "data_assinatura", nullable = false)
    private LocalDate dataAssinatura;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private String status;
}
