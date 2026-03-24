## Why

Clientes da Itau Corretora precisam de um mecanismo de investimento recorrente e automatizado em ações, eliminando a necessidade de decisões manuais de compra e garantindo diversificação consistente. O produto precisa ser construído do zero, cobrindo todo o ciclo: adesão, compra programada, distribuição proporcional, rebalanceamento e conformidade fiscal.

## What Changes

- **Novo sistema** de compra programada de ações integrado à B3
- Motor de compra automático executando nos dias 5, 15 e 25 de cada mês
- Gestão de conta gráfica e custódia master/filhote por cliente
- Cesta de recomendação Top Five administrada pela equipe de Research
- Distribuição proporcional de ações para custódias filhote com cálculo de preço médio
- Rebalanceamento automático ao mudar composição da cesta
- Publicação de eventos de IR (dedo-duro e venda) em tópico Kafka
- API REST para adesão, gestão de clientes, cesta e consulta de rentabilidade

## Capabilities

### New Capabilities

- `gestao-clientes`: Adesão, saída e alteração de valor mensal de clientes; criação automática de conta gráfica e custódia filhote
- `cesta-recomendacao`: Cadastro e ativação da cesta Top Five com exatamente 5 ativos totalizando 100%; histórico de cestas anteriores
- `motor-compra`: Agrupamento de clientes ativos, leitura de cotações COTAHIST, cálculo de quantidades (lote padrão + fracionário), compra na conta master e distribuição proporcional para filhotes com residuos
- `preco-medio`: Cálculo e manutenção do preço médio por ativo por cliente a cada compra ou distribuição
- `rebalanceamento`: Rebalanceamento automático ao alterar a cesta (venda de ativos removidos, compra de novos, ajuste de percentuais alterados)
- `fiscal-ir`: Cálculo e publicação em Kafka do IR dedo-duro (compras) e IR sobre vendas (rebalanceamento, regra R$ 20.000/mês)
- `rentabilidade`: Consulta da carteira do cliente com saldo atual, P/L por ativo, rentabilidade percentual, composição real vs. cesta

### Modified Capabilities

## Impact

- **Banco de dados**: Novo schema com tabelas Clientes, ContasGraficas, Custodias, CestaRecomendacao, ItensCSeta, HistoricoOperacoes, ExecucoesMotor
- **API REST**: Endpoints novos — sem alteração em sistemas existentes
- **Integração B3**: Leitura do arquivo COTAHIST (formato posicional fixo)
- **Kafka**: Publicação em tópico de IR; requer broker disponível
- **Cron/Scheduler**: Job agendado para os dias 5, 15 e 25 (com ajuste para próximo dia útil)
