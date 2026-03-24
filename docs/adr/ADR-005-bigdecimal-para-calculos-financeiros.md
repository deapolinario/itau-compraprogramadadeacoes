# ADR-005: BigDecimal para Todos os Cálculos Financeiros

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

Sistemas financeiros lidam com valores monetários que exigem precisão exata. O uso de tipos de ponto flutuante (`double`, `float`) em cálculos financeiros é uma fonte conhecida de erros de arredondamento:

```java
// Exemplo do problema:
double preco = 35.80;
double quantidade = 28;
System.out.println(preco * quantidade); // 1002.4000000000001 (errado)
```

O sistema calcula:
- Valor total de compras (preço × quantidade)
- Preço Médio de custódia (com 6 casas decimais internas)
- IR dedo-duro (0,005% do valor da operação)
- IR sobre vendas (20% do lucro líquido)
- Rentabilidade percentual da carteira

Todos esses cálculos precisam ser **deterministicamente corretos** e auditáveis.

## Decisão

Usar **`java.math.BigDecimal`** em todos os campos e cálculos financeiros, com as seguintes convenções:

| Contexto | Escala | Modo de arredondamento |
|---|---|---|
| Preços de mercado (cotações) | 2 casas | `HALF_UP` |
| Valor total de operações | 2 casas | `HALF_UP` |
| Preço Médio (PM) de custódia | 6 casas | `ROUND_DOWN` (conservador) |
| IR calculado | 2 casas | `HALF_UP` |
| Rentabilidade percentual | 4 casas | `HALF_UP` |

**Colunas JPA:** `precision = 18, scale = 2` para valores de mercado; `precision = 18, scale = 6` para preço médio.

**Conversão do COTAHIST:** O campo PREULT é um inteiro com 2 casas implícitas. A conversão usa `BigDecimal.valueOf(longValue, 2)` — que é exata, sem representação intermediária de ponto flutuante:

```java
long valor = Long.parseLong(valorBruto.trim()); // ex.: 3580
return BigDecimal.valueOf(valor, 2);             // = 35.80 (exato)
```

**Quantidade de ações:** `Long` (inteiro) — ações não têm fração no lote padrão. O cálculo usa `TRUNC` explícito:

```java
long quantidade = valorParcela.divide(preco, 0, RoundingMode.DOWN).longValue();
```

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| `double` / `float` | Erros de ponto flutuante inaceitáveis em domínio financeiro |
| `long` com centavos (valor × 100) | Perda de legibilidade; conversões manuais propensas a bug; dificulta integrações |
| Biblioteca `Money` (JSR 354) | Dependência adicional sem ganho claro para o escopo atual |

## Consequências

**Positivas:**
- Resultados determinísticos e reproduzíveis
- Conformidade com práticas contábeis e regulatórias
- `BigDecimal.compareTo` (em vez de `equals`) usado nos testes para ignorar zeros à direita (`35.80` == `35.8`)

**Negativas:**
- Verbosidade maior no código (`.multiply()`, `.setScale()`, etc.)
- Performance levemente inferior ao `double` — irrelevante para o volume esperado
- `BigDecimal` não é serializável por padrão em alguns contextos JSON: requer configuração do `ObjectMapper` (suportado nativamente pelo Jackson)

**Regra de ouro aplicada:**
> Nunca usar `new BigDecimal(double)`. Sempre `BigDecimal.valueOf(double)` ou `new BigDecimal("string literal")`.
