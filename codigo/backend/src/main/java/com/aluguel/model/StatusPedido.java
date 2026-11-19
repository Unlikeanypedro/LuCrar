package com.aluguel.model;

/** Ciclo de vida descrito em docs/modelagem.md e docs/historias-usuario.md. */
public enum StatusPedido {
    PENDENTE,
    /** Pedido assumido por um agente para análise financeira. */
    EM_ANALISE,
    APROVADO,
    /** Valor legado de bases antigas; novos pareceres negativos usam REJEITADO. */
    REPROVADO,
    REJEITADO,
    CONTRATADO,
    CANCELADO
}
