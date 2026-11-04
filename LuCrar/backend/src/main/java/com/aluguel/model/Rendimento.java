package com.aluguel.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Rendimento {
    private String entidadeEmpregadora;
    private BigDecimal valor;
}
