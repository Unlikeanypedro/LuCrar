# Histórias de Usuário

| ID | Como... | Quero... | Para... | Critérios de aceite |
|---|---|---|---|---|
| HU01 | visitante | me cadastrar com usuário e senha (cliente ou agente) | usar o sistema | login único; cliente informa CPF, nome, endereço, profissão e até 3 rendimentos; RG é opcional; agente informa a chave de cadastro |
| HU02 | usuário cadastrado | fazer login | acessar o sistema | sem login, toda a API retorna 401; senha armazenada com BCrypt |
| HU03 | cliente | consultar, editar e excluir meu cadastro | manter meus dados atualizados | cliente só acessa o próprio cadastro; no máximo 3 rendimentos |
| HU04 | cliente | introduzir um pedido de aluguel | alugar um automóvel | escolho automóvel e período; pedido nasce `PENDENTE` |
| HU05 | cliente | consultar o status dos meus pedidos | acompanhar a análise | vejo apenas meus pedidos |
| HU06 | cliente | modificar um pedido pendente | corrigir período/automóvel | só enquanto `PENDENTE` |
| HU07 | cliente | cancelar um pedido | desistir do aluguel | não cancela se já `CONTRATADO` |
| HU08 | agente | avaliar financeiramente um pedido | aprovar ou rejeitar | pedido `PENDENTE` vira `APROVADO` ou `REJEITADO` |
| HU09 | agente (banco) | conceder crédito ao aprovar | financiar o aluguel | registra `ContratoCredito` com valor e banco |
| HU10 | agente | executar o contrato de um pedido aprovado | formalizar o aluguel | `APROVADO` vira `CONTRATADO` |
| HU11 | agente | cadastrar automóveis | disponibilizá-los para aluguel | matrícula, ano, marca, modelo, placa e proprietário |
| HU12 | agente | modificar qualquer pedido não finalizado | corrigir dados | não altera `CANCELADO`/`CONTRATADO` |
