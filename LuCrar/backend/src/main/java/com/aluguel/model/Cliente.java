package com.aluguel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rg;
    private String cpf;
    private String nome;
    private String endereco;
    private String profissao;

    /** Entidades empregadoras e rendimentos (máximo 3). */
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Rendimento> rendimentos = new ArrayList<>();

    @JsonIgnore
    @OneToOne(optional = false)
    private Usuario usuario;
}
