## ADDED Requirements

### Requirement: Cadastro da cesta Top Five
O sistema SHALL manter uma cesta de recomendação com exatamente 5 ações. A soma dos percentuais MUST ser exatamente 100%. Cada percentual MUST ser maior que 0%. Apenas uma cesta MUST estar ativa por vez.

#### Scenario: Criação de cesta válida
- **WHEN** uma requisição POST /cestas é recebida com exatamente 5 ativos e soma de percentuais = 100%
- **THEN** o sistema desativa a cesta anterior (DataDesativacao preenchida), cria a nova cesta com status ativo, e retorna 201 com ID da cesta

#### Scenario: Menos de 5 ativos
- **WHEN** uma requisição POST /cestas é recebida com menos de 5 ativos
- **THEN** o sistema retorna 400 Bad Request com mensagem "A cesta deve conter exatamente 5 ações"

#### Scenario: Soma de percentuais diferente de 100%
- **WHEN** uma requisição POST /cestas é recebida com percentuais que não somam 100%
- **THEN** o sistema retorna 400 Bad Request com mensagem de validação de percentual

#### Scenario: Percentual zerado
- **WHEN** uma requisição POST /cestas é recebida com algum percentual = 0%
- **THEN** o sistema retorna 400 Bad Request

---

### Requirement: Consulta da cesta ativa
O sistema SHALL disponibilizar endpoint para consultar a cesta de recomendação atualmente ativa.

#### Scenario: Consulta da cesta ativa
- **WHEN** uma requisição GET /cestas/ativa é recebida
- **THEN** o sistema retorna 200 com a composição da cesta ativa (5 ativos com seus tickers e percentuais)

#### Scenario: Nenhuma cesta cadastrada
- **WHEN** uma requisição GET /cestas/ativa é recebida e não há cesta cadastrada
- **THEN** o sistema retorna 404 Not Found

---

### Requirement: Disparo de rebalanceamento ao alterar cesta
O sistema SHALL disparar o processo de rebalanceamento para todos os clientes ativos quando uma nova cesta for ativada.

#### Scenario: Rebalanceamento disparado após nova cesta
- **WHEN** uma nova cesta é criada com sucesso
- **THEN** o sistema publica um evento interno de rebalanceamento para processamento assíncrono de todos os clientes ativos

#### Scenario: Ativos removidos da cesta
- **WHEN** uma nova cesta é ativada e alguns ativos não fazem parte da nova composição
- **THEN** o rebalanceamento MUST vender toda a posição desses ativos para cada cliente ativo
