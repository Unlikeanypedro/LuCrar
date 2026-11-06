package com.aluguel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Automovel {

    public enum Proprietario {
        CLIENTE,
        EMPRESA,
        BANCO,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricula;
    private Integer ano;
    private String marca;
    private String modelo;
    private String placa;

    @Column(name = "valor_diaria", precision = 10, scale = 2)
    private BigDecimal valorDiaria;

    /** Categoria do proprietário na tabela automovel do Supabase. */
    @Enumerated(EnumType.STRING)
    @Column(name = "proprietario_tipo")
    private Proprietario proprietario = Proprietario.EMPRESA;

    @Column(name = "proprietario_cliente_id")
    private Long proprietarioClienteId;

    @Column(name = "proprietario_agente_id")
    private Long proprietarioAgenteId;

    /** Optional user-level owner used by newly registered catalogue entries. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario proprietarioUsuario;
}
