## ADDED Requirements

### Requirement: Rebalanceamento por mudança de cesta
O sistema SHALL, ao ativar uma nova cesta, executar rebalanceamento assíncrono para todos os clientes ativos. Para ativos que saíram da cesta, MUST vender toda a posição do cliente. Para ativos que entraram, MUST comprar proporcionalmente ao valor obtido nas vendas. Para ativos que permaneceram com percentual alterado, MUST ajustar (vender excesso ou comprar deficit).

#### Scenario: Ativo removido da cesta
- **WHEN** BBDC4 estava na cesta com 15% e não está na nova cesta
- **THEN** o sistema vende toda a posição de BBDC4 de cada cliente ativo

#### Scenario: Ativo adicionado à cesta
- **WHEN** ABEV3 não estava na cesta e entra com 20%
- **THEN** o sistema usa o valor obtido nas vendas para comprar ABEV3 para cada cliente ativo, proporcionalmente ao peso do novo ativo em relação aos demais novos ativos

#### Scenario: Ativo com percentual alterado — redução
- **WHEN** PETR4 tinha 30% na cesta antiga e passa para 25% na nova cesta
- **THEN** o sistema calcula a quantidade de PETR4 correspondente ao excesso e vende para o cliente

#### Scenario: Ativo mantido sem alteração de percentual
- **WHEN** ITUB4 mantém 20% na cesta antiga e na nova cesta
- **THEN** nenhuma operação de rebalanceamento é realizada para ITUB4

#### Scenario: Rebalanceamento registrado em histórico
- **WHEN** o rebalanceamento executa vendas e compras para um cliente
- **THEN** todas as operações MUST ser registradas em historico_operacoes com tipo VENDA ou COMPRA

---

### Requirement: Rebalanceamento por desvio de proporção
O sistema SHALL suportar rebalanceamento manual disparado quando a proporção real da carteira de um cliente desvia significativamente dos percentuais da cesta ativa. O limiar de desvio padrão MUST ser 5 pontos percentuais.

#### Scenario: Desvio acima do limiar
- **WHEN** um ativo representa 35% da carteira real e a cesta define 30% (desvio de 5pp)
- **THEN** o sistema identifica o ativo como sobre-alocado e inclui no rebalanceamento

#### Scenario: Desvio abaixo do limiar
- **WHEN** um ativo representa 28% da carteira real e a cesta define 30% (desvio de 2pp)
- **THEN** o ativo não é incluído no rebalanceamento
