# ADR-007: Rebalanceamento Assíncrono via Spring Events

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

Quando um gestor altera a Cesta de Recomendação (composição de ativos e percentuais), o sistema precisa rebalancear automaticamente as carteiras de todos os clientes ativos:

1. Vender ativos que foram removidos da cesta
2. Comprar ativos que foram adicionados
3. Ajustar quantidades para ativos que mudaram de percentual

Esse processo envolve:
- Leitura do arquivo COTAHIST para obter preços atuais
- N operações de compra/venda (uma por cliente × ativo alterado)
- Publicação de eventos fiscais (IR sobre vendas)

O processamento pode levar vários segundos para bases de clientes grandes. Executar isso de forma síncrona no request HTTP de alteração da cesta resultaria em timeout e péssima experiência do operador.

## Decisão

Implementar o rebalanceamento de forma **assíncrona via Spring Application Events**.

**Fluxo:**

```
POST /api/cestas  (request do gestor)
    │
    ▼
CestaService.criar()
    │
    ├─ Salva nova CestaRecomendacao no banco
    │
    └─ applicationEventPublisher.publishEvent(
           new CestaAlteradaEvent(cestaAntiga, cestaNova)
       )
    │
    ▼
HTTP 201 Created  ← retorna imediatamente
    │
    ▼ (em outra thread - @Async)
RebalanceamentoListener.onCestaAlterada()
    │
    └─ RebalanceamentoService.executar(antiga, nova)
           ├─ Identifica ativos removidos → vende
           ├─ Identifica ativos adicionados → compra
           └─ Ajusta percentuais alterados
```

**Implementação:**

```java
// Publicação
@Component
public class CestaService {
    private final ApplicationEventPublisher eventPublisher;

    public CestaRecomendacao criar(...) {
        // ... salva no banco
        eventPublisher.publishEvent(new CestaAlteradaEvent(antiga, nova));
        return nova;
    }
}

// Consumo assíncrono
@Component
public class RebalanceamentoListener {

    @Async
    @EventListener
    public void onCestaAlterada(CestaAlteradaEvent event) {
        rebalanceamentoService.executar(event.getCestaAntiga(), event.getCestaNova());
    }
}
```

**Configuração do thread pool** (`AsyncConfig`):
- Core pool: 2 threads
- Max pool: 10 threads
- Queue capacity: 500

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Processamento síncrono no request | Timeout para bases grandes; bloqueia o operador; risco de HTTP 504 |
| Kafka consumer dedicado | Adiciona complexidade de infraestrutura para um caso de uso interno |
| `@Scheduled` periódico de detecção de mudanças | Latência desnecessária; requer flag de "processado" na cesta |
| CompletableFuture manual | Mais verboso sem ganho sobre `@Async`; sem pool gerenciado pelo Spring |

## Consequências

**Positivas:**
- Response time do endpoint de cesta: O(1) — não depende do número de clientes
- `@Async` com pool configurável permite controle de concorrência
- Desacoplamento limpo: `CestaService` não conhece `RebalanceamentoService`
- Testabilidade: `CestaService` pode ser testado verificando apenas a publicação do evento

**Negativas:**
- O gestor não recebe feedback imediato sobre erros de rebalanceamento
- Falhas silenciosas: se o thread lançar exceção, ela é logada mas não notifica o caller
- Em ambiente com múltiplas instâncias, o evento Spring é local (não distribuído) — cada instância procesará o evento independentemente. **Mitigação recomendada para produção:** migrar o evento para um tópico Kafka, tornando-o distribuído e idempotente.

**Observação de segurança:**
O `@Async` usa o bean `AsyncConfig` configurado explicitamente, em vez de depender do pool padrão do Spring Boot, para garantir controle sobre tamanho de fila e comportamento sob carga.
