# ADR-004: Idempotência do Motor de Compra via Tabela de Execuções

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O Motor de Compra é acionado por um `@Scheduled` cron nos dias 5, 15 e 25 de cada mês. Em um ambiente produtivo, execuções podem ser disparadas múltiplas vezes em razão de:

- Reinicializações do pod/container após falha
- Múltiplas instâncias rodando ao mesmo tempo (escalabilidade horizontal)
- Reprocessamento manual por operadores
- Falhas parciais que derrubam o processo no meio da execução

Uma dupla execução numa mesma data de referência geraria compras duplicadas e débitos incorretos nas contas dos clientes — um erro financeiro grave e de difícil reversão.

## Decisão

Implementar **idempotência via tabela `execucoes_motor`** com controle de status.

```sql
CREATE TABLE execucoes_motor (
    id              BIGSERIAL PRIMARY KEY,
    data_referencia DATE NOT NULL,
    status          VARCHAR(15) NOT NULL,  -- INICIADO, CONCLUIDO, ERRO
    iniciado_em     TIMESTAMP NOT NULL,
    concluido_em    TIMESTAMP,
    total_clientes  INTEGER,
    total_acoes     BIGINT
);
```

**Fluxo de guarda:**

```
executar(dataRef):
  1. SE existsByDataReferenciaAndStatus(dataRef, CONCLUIDO) → return silencioso
  2. Criar ou recuperar ExecucaoMotor com status INICIADO
  3. Processar compras...
  4. Atualizar status → CONCLUIDO
```

A verificação na etapa 1 é a "trava" de idempotência. Uma execução com status `INICIADO` que não chegou ao `CONCLUIDO` indica falha parcial — o operador pode reprocessar manualmente ou o sistema pode tentar novamente sem risco de duplicidade, pois a lógica de custódia é acumulativa (adiciona quantidade, não substitui).

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Lock distribuído (Redis/Zookeeper) | Infraestrutura adicional; complexidade de gerenciamento de expiração de lock |
| Unique constraint no banco por data | Não permite reprocessamento em caso de falha parcial (status INICIADO) |
| Controle apenas em memória | Não persiste entre reinicializações — não resolve o problema |
| Outbox pattern completo | Overkill para este cenário; a tabela simples de execuções já resolve |

## Consequências

**Positivas:**
- Proteção contra duplicidade de compras sem infra adicional
- Rastreabilidade completa: cada execução registrada com timestamp e totais
- Suporte a múltiplas instâncias sem coordenação externa (verificação é atômica no banco)
- Reprocessamento seguro: status `INICIADO` permite nova tentativa; `CONCLUIDO` bloqueia

**Negativas:**
- Race condition teórica: duas instâncias podem passar a verificação simultaneamente antes que qualquer uma persista o `INICIADO`. Mitigação: constraint `UNIQUE(data_referencia, status='CONCLUIDO')` pode ser adicionada futuramente
- Não implementa saga/compensação para falhas parciais — exige intervenção manual

**Observação de design:**
O método `executar(LocalDate dataRef)` aceita a data como parâmetro (em vez de usar `LocalDate.now()` internamente) para permitir reprocessamento histórico e testes unitários sem mocking de clock.
