package com.aluguel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Automovel {
    public enum Proprietario { CLIENTE, EMPRESA, BANCO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricula;
    private Integer ano;
    private String marca;
    private String modelo;
    private String placa;

    /** Depende do tipo de contrato: cliente, empresa ou banco. */
    @Enumerated(EnumType.STRING)
    private Proprietario proprietario = Proprietario.EMPRESA;
}
