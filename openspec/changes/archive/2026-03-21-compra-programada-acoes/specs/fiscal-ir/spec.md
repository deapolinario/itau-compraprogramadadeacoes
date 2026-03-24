## ADDED Requirements

### Requirement: IR dedo-duro em compras
O sistema SHALL calcular o IR dedo-duro de 0,005% sobre o valor de cada operação distribuída ao cliente e publicar no tópico Kafka. O cálculo MUST ser feito por ativo por cliente a cada distribuição.

#### Scenario: Cálculo do IR dedo-duro
- **WHEN** cliente recebe 8 ações de PETR4 a R$ 35,00 (valor = R$ 280,00)
- **THEN** IR dedo-duro = R$ 280,00 × 0,00005 = R$ 0,014, arredondado para R$ 0,01

#### Scenario: Publicação no Kafka
- **WHEN** IR dedo-duro é calculado para uma operação
- **THEN** o sistema publica mensagem no tópico Kafka com tipo "IR_DEDO_DURO", clienteId, CPF, ticker, tipoOperacao, quantidade, precoUnitario, valorOperacao, aliquota, valorIR e dataOperacao

#### Scenario: Kafka indisponível
- **WHEN** o broker Kafka não está acessível no momento da publicação
- **THEN** o evento MUST ser salvo em tabela de auditoria com status PENDENTE para retry manual; a distribuição ao cliente NÃO DEVE ser bloqueada

---

### Requirement: IR sobre vendas em rebalanceamento
O sistema SHALL apurar o IR sobre vendas do rebalanceamento com base no total de vendas do cliente no mês corrente. Se o total <= R$ 20.000,00, o cliente está isento. Se o total > R$ 20.000,00, MUST calcular 20% sobre o lucro líquido total. Se houver prejuízo líquido, o IR MUST ser R$ 0,00.

#### Scenario: Total de vendas abaixo de R$ 20.000 — isento
- **WHEN** total de vendas do cliente no mês = R$ 230,00
- **THEN** IR sobre venda = R$ 0,00 (isento); nenhuma mensagem de IR_VENDA é publicada

#### Scenario: Total de vendas acima de R$ 20.000 com lucro
- **WHEN** total de vendas = R$ 21.500,00 e lucro líquido = R$ 3.100,00
- **THEN** IR = R$ 3.100,00 × 20% = R$ 620,00; mensagem IR_VENDA publicada no Kafka

#### Scenario: Total de vendas acima de R$ 20.000 com prejuízo líquido
- **WHEN** total de vendas = R$ 24.400,00 e lucro líquido = -R$ 600,00
- **THEN** IR = R$ 0,00; mensagem IR_VENDA publicada com valorIR = 0

#### Scenario: Publicação IR venda no Kafka
- **WHEN** IR sobre venda é calculado (independente de ser zero ou positivo, quando total > 20k)
- **THEN** o sistema publica mensagem com tipo "IR_VENDA", clienteId, CPF, mesReferencia, totalVendasMes, lucroLiquido, aliquota, valorIR e detalhes por ticker
