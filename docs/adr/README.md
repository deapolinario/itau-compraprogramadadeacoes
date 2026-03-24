# Architecture Decision Records (ADRs)

Registro das decisões arquiteturais tomadas durante o desenvolvimento do Sistema de Compra Programada de Ações.

## Índice

| ADR | Título | Status |
|---|---|---|
| [ADR-001](ADR-001-stack-tecnologica.md) | Stack Tecnológica — Spring Boot 3 + Java 21 | Aceito |
| [ADR-002](ADR-002-cotahist-como-fonte-de-cotacoes.md) | COTAHIST B3 como Fonte de Cotações de Fechamento | Aceito |
| [ADR-003](ADR-003-estrutura-master-filhote.md) | Estrutura de Custódia Master/Filhote | Aceito |
| [ADR-004](ADR-004-idempotencia-motor-de-compra.md) | Idempotência do Motor de Compra via Tabela de Execuções | Aceito |
| [ADR-005](ADR-005-bigdecimal-para-calculos-financeiros.md) | BigDecimal para Todos os Cálculos Financeiros | Aceito |
| [ADR-006](ADR-006-kafka-com-fallback-em-banco.md) | Kafka para Eventos Fiscais com Fallback Persistente | Aceito |
| [ADR-007](ADR-007-rebalanceamento-assincrono.md) | Rebalanceamento Assíncrono via Spring Events | Aceito |
| [ADR-008](ADR-008-flyway-para-migracoes.md) | Flyway para Migrações de Banco de Dados Versionadas | Aceito |
| [ADR-009](ADR-009-estrategia-de-testes.md) | Estratégia de Testes — Unitários + Integração Separados, Cobertura ≥ 70% | Aceito |

## Formato

Cada ADR segue a estrutura:
- **Contexto:** problema ou força que motivou a decisão
- **Decisão:** o que foi decidido e como foi implementado
- **Alternativas Consideradas:** o que foi avaliado e por que descartado
- **Consequências:** positivas, negativas e riscos conhecidos
