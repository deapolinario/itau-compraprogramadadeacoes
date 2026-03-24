## ADDED Requirements

### Requirement: Consulta de rentabilidade da carteira
O sistema SHALL disponibilizar endpoint para consulta da carteira do cliente com dados de rentabilidade. A resposta MUST incluir: saldo total atual, valor investido total, P/L total, rentabilidade percentual, e por ativo: ticker, quantidade, preço médio, cotação atual, valor atual, P/L e composição percentual real.

#### Scenario: Consulta de carteira com posições
- **WHEN** uma requisição GET /clientes/{id}/carteira é recebida para cliente com custódia não vazia
- **THEN** o sistema retorna 200 com saldo total (Qtd × Cotacao_Atual por ativo), P/L por ativo ((Cotacao_Atual - PM) × Qtd), P/L total, rentabilidade percentual ((Valor_Atual - Valor_Investido) / Valor_Investido × 100) e composição real (% de cada ativo no total)

#### Scenario: Consulta de carteira vazia
- **WHEN** uma requisição GET /clientes/{id}/carteira é recebida para cliente sem posições
- **THEN** o sistema retorna 200 com saldo total = 0 e lista de ativos vazia

#### Scenario: Cotação usada na rentabilidade
- **WHEN** a consulta de carteira é feita
- **THEN** o sistema MUST usar a cotação de fechamento mais recente disponível no COTAHIST para calcular o valor atual de cada ativo

#### Scenario: Cliente não encontrado
- **WHEN** uma requisição GET /clientes/{id}/carteira é recebida para ID inexistente
- **THEN** o sistema retorna 404 Not Found

---

### Requirement: Cálculo de valor investido
O sistema SHALL calcular o valor investido total como a soma de (Quantidade × Preço_Médio) para todos os ativos da custódia filhote do cliente.

#### Scenario: Valor investido com múltiplos ativos
- **WHEN** cliente possui PETR4 (24 ações, PM R$ 35,50) e VALE3 (12 ações, PM R$ 60,00)
- **THEN** valor investido = (24 × 35,50) + (12 × 60,00) = R$ 852,00 + R$ 720,00 = R$ 1.572,00
