# ADR-003: Estrutura de Custódia Master/Filhote

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O modelo de compra programada opera em lote: o sistema agrega a demanda de todos os clientes, executa uma única compra de lote inteiro na B3 (mínimo 100 ações) e depois distribui frações proporcionais a cada cliente. Isso exige um mecanismo para:

1. Manter um estoque central de ações compradas em lote
2. Distribuir esse estoque proporcionalmente aos clientes
3. Comprar diretamente no mercado fracionário quando o lote não for viável
4. Rastrear a posse individual de cada cliente de forma auditável

## Decisão

Adotar uma estrutura de **contas gráficas em dois níveis: Master e Filhote**.

```
ContaGrafica (MASTER-001)
  └── Custodia[PETR4] → quantidade agregada comprada no lote
  └── Custodia[VALE3] → ...

ContaGrafica (FILHOTE-001)  ← pertence ao Cliente A
  └── Custodia[PETR4] → quantidade distribuída ao cliente A
  └── Custodia[VALE3] → ...

ContaGrafica (FILHOTE-002)  ← pertence ao Cliente B
  └── Custodia[PETR4] → quantidade distribuída ao cliente B
```

**Regras de operação:**

- A conta **MASTER** é única no sistema (seed no V1 do Flyway) e não pertence a nenhum cliente
- Cada cliente possui exatamente **uma conta FILHOTE**
- Compras de lote padrão (≥100 ações) são registradas na MASTER e depois transferidas proporcionalmente para cada FILHOTE
- Quando a quantidade calculada é < 100 ações por ticker, o sistema compra diretamente no mercado fracionário e credita na FILHOTE sem passar pela MASTER
- O saldo remanescente da MASTER (resíduo de distribuição) permanece acumulado para ciclos futuros

**Cálculo de distribuição:**

```
qtd_bruta_ticker = TRUNC(valor_parcela_cliente / preco_fechamento)
qtd_lote = TRUNC(qtd_bruta_total / 100) * 100
qtd_fracionaria = qtd_bruta_total - qtd_lote

distribuição_cliente = qtd_lote * (valor_cliente / valor_total)
```

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Conta única por cliente sem master | Impossibilita compras de lote inteiro; cada cliente compraria apenas frações, pagando spread maior |
| Fundo de investimento virtual | Complexidade regulatória; fora do escopo do desafio |
| Registro apenas contábil (sem custódia real) | Não permite rastreabilidade de PM e posição individual |

## Consequências

**Positivas:**
- Compras de lote inteiro na B3 têm melhor preço que fracionário
- Um único registro de compra na B3 reduz custos operacionais de corretagem
- A custódia individual por FILHOTE permite calcular Preço Médio e P/L por cliente
- Auditabilidade completa: cada transferência MASTER→FILHOTE gera um `HistoricoOperacao`

**Negativas:**
- Requer lógica de distribuição proporcional (complexidade adicional)
- O resíduo acumulado na MASTER exige reconciliação periódica
- A conta MASTER é um ponto único — sua consistência é crítica

**Invariantes do sistema:**
- `ContaGrafica.tipo = MASTER` → exatamente 1 registro no banco (ID seed = 1)
- `ContaGrafica.tipo = FILHOTE` → um por cliente ativo
- `Custodia.quantidade >= 0` sempre (não existem posições vendidas a descoberto)
