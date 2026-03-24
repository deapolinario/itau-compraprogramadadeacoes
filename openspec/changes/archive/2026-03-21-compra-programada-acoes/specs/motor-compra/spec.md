## ADDED Requirements

### Requirement: Agendamento nos dias 5, 15 e 25
O sistema SHALL executar o motor de compra nos dias 5, 15 e 25 de cada mês. Se a data cair em sábado ou domingo, o motor MUST executar no próximo dia útil (segunda-feira). Dias úteis são definidos como segunda a sexta-feira.

#### Scenario: Dia de execução em dia útil
- **WHEN** o dia 5, 15 ou 25 cai em segunda a sexta-feira
- **THEN** o motor executa no próprio dia

#### Scenario: Dia de execução em sábado
- **WHEN** o dia 5, 15 ou 25 cai em sábado
- **THEN** o motor executa na segunda-feira seguinte

#### Scenario: Dia de execução em domingo
- **WHEN** o dia 5, 15 ou 25 cai em domingo
- **THEN** o motor executa na segunda-feira seguinte

---

### Requirement: Idempotência do motor
O sistema SHALL garantir que o motor de compra execute no máximo uma vez por data de referência. Se o motor for acionado mais de uma vez para a mesma data (ex: restart do serviço), as execuções adicionais MUST ser ignoradas silenciosamente.

#### Scenario: Execução duplicada bloqueada
- **WHEN** o motor já completou com sucesso para a data referência X
- **THEN** uma segunda execução para a mesma data retorna sem realizar compras

#### Scenario: Retry após falha permitido
- **WHEN** o motor falhou (status ERRO) para a data referência X
- **THEN** uma nova execução para a mesma data DEVE prosseguir normalmente

---

### Requirement: Agrupamento e consolidação de clientes
O sistema SHALL agrupar todos os clientes com Ativo = true, calcular a parcela de cada um (ValorMensal / 3) e consolidar o total para compra única na conta master.

#### Scenario: Consolidação de múltiplos clientes
- **WHEN** o motor executa com N clientes ativos
- **THEN** o valor total consolidado é a soma de (ValorMensal / 3) de cada cliente ativo

#### Scenario: Nenhum cliente ativo
- **WHEN** o motor executa e não há clientes com Ativo = true
- **THEN** o motor registra execução como CONCLUIDO sem realizar compras

---

### Requirement: Cálculo de quantidade a comprar
O sistema SHALL calcular a quantidade de ações a comprar por ativo usando a cotação de fechamento do último pregão disponível no COTAHIST. A quantidade MUST ser calculada como TRUNCAR(valor_alvo / cotacao). O saldo existente na custódia master MUST ser descontado da quantidade a comprar. Se a quantidade a comprar resultar negativa (saldo master supera a necessidade), MUST ser tratada como zero.

#### Scenario: Cálculo básico sem saldo master
- **WHEN** valor_alvo = R$ 1.050 e cotacao = R$ 35,00 e saldo_master = 0
- **THEN** qtd_comprar = TRUNC(1050 / 35) = 30 ações

#### Scenario: Desconto do saldo master
- **WHEN** valor_alvo = R$ 1.050, cotacao = R$ 35,00 e saldo_master = 2 ações
- **THEN** qtd_bruta = 30, qtd_comprar = 30 - 2 = 28 ações

#### Scenario: Saldo master supera necessidade
- **WHEN** saldo_master >= qtd_bruta
- **THEN** qtd_comprar = 0 (sem novas compras); saldo existente é usado para distribuição

#### Scenario: COTAHIST sem cotação para ativo
- **WHEN** o arquivo COTAHIST não contém cotação para um dos ativos da cesta
- **THEN** a execução do motor MUST falhar com status ERRO e log descritivo

---

### Requirement: Separação lote padrão e fracionário
O sistema SHALL separar a compra em lote padrão (múltiplos de 100) e mercado fracionário (1 a 99 ações). No mercado fracionário, o ticker MUST receber sufixo "F".

#### Scenario: Quantidade menor que 100
- **WHEN** qtd_comprar = 28 para PETR4
- **THEN** compra 0 lotes (PETR4) e 28 fracionárias (PETR4F)

#### Scenario: Quantidade maior que 100
- **WHEN** qtd_comprar = 350 para PETR4
- **THEN** compra 3 lotes de 100 (PETR4) e 50 fracionárias (PETR4F)

#### Scenario: Múltiplo exato de 100
- **WHEN** qtd_comprar = 200 para PETR4
- **THEN** compra 2 lotes (PETR4) e 0 fracionárias (sem ordem fracionária)

---

### Requirement: Distribuição proporcional para custódias filhote
O sistema SHALL distribuir as ações compradas (mais saldo master anterior) proporcionalmente ao aporte de cada cliente. A proporção MUST ser calculada como (aporte_cliente / total_aportes). A quantidade por cliente MUST ser TRUNCAR(total_disponivel × proporcao). O residuo MUST permanecer na custódia master para o próximo ciclo.

#### Scenario: Distribuição com residuo
- **WHEN** total_disponivel = 30 ações de PETR4 e proporcoes resultam em distribuição de 29
- **THEN** 29 ações são distribuídas para os filhotes e 1 ação permanece na master

#### Scenario: Distribuição exata sem residuo
- **WHEN** total_disponivel e proporções resultam em distribuição sem sobra
- **THEN** 0 ações ficam na master para esse ativo

#### Scenario: Registro em histórico de operações
- **WHEN** ações são distribuídas para um cliente
- **THEN** o sistema MUST registrar a operação em historico_operacoes com ticker, quantidade, preço e data
