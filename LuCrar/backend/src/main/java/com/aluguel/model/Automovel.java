package com.aluguel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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

    /** Categoria do titular, mantida para exibição e validação da API. */
    @Enumerated(EnumType.STRING)
    private Proprietario proprietario = Proprietario.EMPRESA;

    /** Titular específico; nullable only for the pre-existing demonstration catalog. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario proprietarioUsuario;
}
