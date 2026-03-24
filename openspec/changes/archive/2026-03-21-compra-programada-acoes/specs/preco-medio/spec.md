## ADDED Requirements

### Requirement: Cálculo de preço médio em compras
O sistema SHALL calcular e manter o preço médio por ativo por cliente a cada distribuição de ações. A fórmula MUST ser: PM = (Qtd_Anterior × PM_Anterior + Qtd_Nova × Preco_Nova) / (Qtd_Anterior + Qtd_Nova). Em vendas, o preço médio NÃO DEVE ser alterado.

#### Scenario: Primeira compra do ativo
- **WHEN** cliente recebe 8 ações de PETR4 a R$ 35,00 e não tinha posição anterior
- **THEN** PM = R$ 35,00

#### Scenario: Segunda compra do mesmo ativo
- **WHEN** cliente já possui 8 ações com PM = R$ 35,00 e recebe 10 ações a R$ 37,00
- **THEN** PM = (8 × 35,00 + 10 × 37,00) / (8 + 10) = R$ 36,11

#### Scenario: Venda não altera preço médio
- **WHEN** cliente possui 18 ações com PM = R$ 36,11 e vende 5 ações
- **THEN** PM permanece R$ 36,11; apenas a quantidade diminui para 13 ações

#### Scenario: Compra após venda recalcula PM
- **WHEN** cliente possui 13 ações com PM = R$ 36,11 e recebe 7 ações a R$ 38,00
- **THEN** PM = (13 × 36,11 + 7 × 38,00) / (13 + 7) = R$ 36,77

---

### Requirement: Precisão decimal do preço médio
O sistema SHALL armazenar o preço médio com precisão de 6 casas decimais (DECIMAL 18,6) para evitar acumulação de erros de arredondamento.

#### Scenario: Armazenamento de PM com decimais
- **WHEN** o preço médio calculado resulta em valor com mais de 2 casas decimais
- **THEN** o valor é armazenado com até 6 casas decimais sem arredondamento adicional
