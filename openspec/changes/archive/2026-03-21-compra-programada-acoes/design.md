## Context

Sistema novo sendo construído do zero como desafio técnico da Itau Corretora. Não há código existente — apenas documentação de domínio (PRD, regras de negócio, diagramas ER e de sequência). O domínio envolve operações financeiras com precisão crítica: cálculo de preço médio, distribuição proporcional de ações, apuração de IR e integração com a B3 via arquivo COTAHIST.

## Goals / Non-Goals

**Goals:**
- Implementar o sistema completo de compra programada conforme PRD e regras de negócio
- Motor de compra idempotente e confiável nos dias 5, 15 e 25
- API REST para todas as operações de cliente, cesta e consulta
- Publicação de eventos fiscais em Kafka
- Cobertura de testes ≥ 70%

**Non-Goals:**
- Integração real com bolsa (compras simuladas via registro em banco)
- Autenticação/autorização de usuários finais
- Interface gráfica (apenas API)
- Compensação de prejuízo entre meses no cálculo de IR
- Multi-tenancy ou múltiplas cestas simultâneas

## Decisions

### D1: Stack — Java 21 + Spring Boot 3

**Decisão:** Java 21 com Spring Boot 3, Spring Data JPA, Spring Scheduler para o cron.

**Alternativas consideradas:**
- Node.js/TypeScript — menos verboso, mas ecossistema financeiro mais maduro em Java
- Python — ótimo para cálculos, mas Spring oferece integração Kafka e agendamento nativos

**Rationale:** Padrão do mercado financeiro brasileiro; Spring Boot entrega cron, Kafka, JPA e REST com configuração mínima.

---

### D2: Banco de dados — PostgreSQL

**Decisão:** PostgreSQL como banco relacional principal.

**Alternativas consideradas:**
- H2 em memória — adequado para testes, mas não para produção
- MySQL — sem vantagens técnicas para esse domínio

**Rationale:** Suporte nativo a `DECIMAL` de alta precisão, transações ACID, e familiaridade ampla. Sem residuos de ponto flutuante em operações financeiras.

**Regra:** Todos os valores monetários usam `DECIMAL(18,2)`. Preço médio usa `DECIMAL(18,6)` para evitar arredondamento cumulativo.

---

### D3: Idempotência do Motor de Compra

**Decisão:** Tabela `execucoes_motor` com `(data_referencia, status)`. Antes de executar, verifica se já existe registro `CONCLUIDO` para a data. Se sim, aborta silenciosamente.

**Rationale:** O cron pode disparar mais de uma vez por falha/restart. Compras duplicadas são inaceitáveis em contexto financeiro.

**Status possíveis:** `PENDENTE → EM_EXECUCAO → CONCLUIDO | ERRO`

---

### D4: Leitura do COTAHIST

**Decisão:** Leitura de arquivo local com layout posicional fixo da B3. Parser dedicado que extrai preço de fechamento (`PREULT`) para os tickers da cesta ativa. Usa o registro mais recente por ticker (permite arquivo acumulativo anual).

**Rationale:** O COTAHIST é batch diário. Para o desafio, o arquivo é fornecido localmente. Parser simples, sem dependências externas.

---

### D5: Aritmética de distribuição — TRUNCAR, não arredondar

**Decisão:** Usar `BigDecimal.ROUND_DOWN` (truncamento) em todos os cálculos de quantidade de ações: `qtd = TRUNC(valor / cotacao)` e `qtd_cliente = TRUNC(total × proporcao)`.

**Rationale:** Definido explicitamente nas regras de negócio. Evita distribuir mais ações do que disponível. Residuos ficam na custódia master.

---

### D6: Rebalanceamento — assíncrono via Spring Event

**Decisão:** Ao alterar a cesta, publicar um `CestaAlteradaEvent` via `ApplicationEventPublisher`. Um listener `RebalanceamentoListener` processa o rebalanceamento de forma assíncrona (`@Async`).

**Alternativas consideradas:**
- Síncrono na mesma request — risco de timeout com 500+ clientes
- Fila Kafka — overhead desnecessário para evento interno

**Rationale:** `@Async` com thread pool dedicado é suficiente para o escopo do desafio, mantendo a request do admin responsiva.

---

### D7: Kafka — publicação fire-and-forget com log de auditoria

**Decisão:** `KafkaTemplate` para publicar eventos de IR. Salvar cópia em tabela `eventos_kafka` para auditoria e retry manual em caso de falha do broker.

**Rationale:** Garante rastreabilidade mesmo sem Kafka disponível no ambiente de testes.

---

### D8: Separação em camadas — Hexagonal simplificado

```
Controller (REST)
    │
Service (casos de uso, regras de negócio)
    │
Repository (Spring Data JPA)
    │
Domain (entidades, value objects)
```

Sem portas/adaptadores formais — estrutura de pacotes por domínio:
`cliente`, `cesta`, `motor`, `rebalanceamento`, `fiscal`, `cotahist`

## Risks / Trade-offs

- **Precisão decimal acumulativa** → Usar `BigDecimal` em todo o código; nunca `double` ou `float` para valores monetários
- **COTAHIST desatualizado** → Motor usa último registro disponível; se arquivo não existir, execução falha com status `ERRO` e log claro
- **Kafka indisponível** → Evento salvo em `eventos_kafka` com status `PENDENTE`; retry manual via endpoint admin
- **Rebalanceamento com muitos clientes** → Thread pool configurável; sem SLA definido no desafio
- **Cálculo de qtd_comprar negativo** (saldo master > qtd bruta) → Tratar como 0 compras; usar saldo existente para distribuição
- **Alteração de valor mensal entre parcelas** → A regra define que o novo valor é aplicado na próxima data de compra; a parcela já executada usa o valor anterior (conforme RN-012)

## Open Questions

- Qual o comportamento quando `TRUNC(valor_parcela / cotacao) = 0` para todos os clientes em um ativo? O valor monetário não utilizado naquele ciclo é descartado (sem residuo financeiro — apenas residuo em ações na master).
- Formato e localização do arquivo COTAHIST no ambiente de execução? Assumir path configurável via `application.properties`.
