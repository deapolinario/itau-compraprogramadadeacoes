# ADR-006: Kafka para Eventos Fiscais com Fallback Persistente

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O sistema precisa notificar a Receita Federal (simulado via tópico Kafka) sobre dois tipos de eventos fiscais:

1. **IR Dedo-Duro (IRRF):** 0,005% retido na fonte sobre cada compra de ações distribuída ao cliente
2. **IR sobre Vendas:** 20% sobre lucro líquido quando total de vendas no mês > R$ 20.000

Esses eventos têm natureza regulatória obrigatória — a falha no envio não pode resultar em perda silenciosa do dado.

## Decisão

Publicar eventos fiscais via **Apache Kafka** com **fallback de persistência em banco** quando o broker está indisponível.

**Arquitetura:**

```
FiscalService.publicar(mensagem)
    │
    ├─► kafkaTemplate.send(topicIr, payload)
    │       │
    │       ├─ Sucesso → salvarEventoKafka(tipo, payload, ENVIADO)
    │       │
    │       └─ Exceção → salvarEventoKafka(tipo, payload, PENDENTE)
    │                     (log.warn para alerta operacional)
    │
    └─► tabela eventos_kafka (outbox simplificado)
```

**Tabela `eventos_kafka`:**
```sql
id          BIGSERIAL PRIMARY KEY
tipo        VARCHAR(30)   -- IR_DEDO_DURO | IR_VENDA
payload     TEXT          -- JSON completo do evento
status      VARCHAR(10)   -- PENDENTE | ENVIADO | ERRO
criado_em   TIMESTAMP
enviado_em  TIMESTAMP
```

O status `PENDENTE` serve como fila de reprocessamento para um job futuro (não implementado nesta versão) que pode tentar reenviar eventos falhos.

**Conteúdo do evento IR_DEDO_DURO:**
```json
{
  "tipo": "IR_DEDO_DURO",
  "clienteId": 1,
  "cpf": "12345678901",
  "ticker": "PETR4",
  "tipoOperacao": "COMPRA",
  "quantidade": 28,
  "precoUnitario": 35.80,
  "valorOperacao": 1002.40,
  "aliquota": 0.00005,
  "valorIR": 0.05,
  "dataOperacao": "2026-02-05T18:30:00"
}
```

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Kafka puro sem fallback | Perda silenciosa de eventos fiscais em caso de indisponibilidade — inaceitável |
| Apenas banco de dados (sem Kafka) | Sem integração com sistemas downstream da Receita; não escalável |
| Transactional Outbox completo | Mais robusto, mas requer CDC (Debezium) ou polling — overkill para o escopo |
| REST para serviço de IR | Acoplamento síncrono; risco de timeout em horário de pico pós-pregão |

## Consequências

**Positivas:**
- Eventos nunca perdidos: ou vão para o Kafka ou ficam no banco com status `PENDENTE`
- Desacoplamento: o consumidor do tópico processa no seu próprio ritmo
- Rastreabilidade completa de todos os eventos fiscais gerados

**Negativas:**
- Possibilidade de duplicidade: se o Kafka aceitar a mensagem mas a confirmação for perdida, o evento é salvo como `ENVIADO` mas pode ser reenviado por um job de retry — require idempotência no consumidor
- O padrão implementado é um **outbox simplificado** (sem garantia de exactly-once): o reprocessamento dos eventos `PENDENTE` não foi implementado nesta versão

**Configuração:**
- Tópico configurável via `kafka.topic.ir` (default: `ir-eventos`)
- Serialização JSON via Jackson `ObjectMapper` injetado — permite customizações globais (ex.: formato de datas)
