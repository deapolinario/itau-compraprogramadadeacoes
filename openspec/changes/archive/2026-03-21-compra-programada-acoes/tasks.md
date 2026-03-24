## 1. Setup do Projeto

- [x] 1.1 Criar projeto Spring Boot 3 com Java 21 (Spring Web, Spring Data JPA, Spring Kafka, Spring Scheduler)
- [x] 1.2 Configurar dependências no pom.xml (PostgreSQL driver, Lombok, Validation, Testcontainers)
- [x] 1.3 Configurar application.properties (datasource, Kafka, path do COTAHIST, scheduler)
- [x] 1.4 Criar script de migration SQL com todas as tabelas (Clientes, ContasGraficas, Custodias, CestaRecomendacao, ItensCesta, HistoricoOperacoes, ExecucoesMotor, EventosKafka)
- [x] 1.5 Configurar Docker Compose com PostgreSQL e Kafka para desenvolvimento local

## 2. Domínio — Entidades e Repositórios

- [x] 2.1 Criar entidade `Cliente` (id, nome, cpf, email, valorMensal, ativo, dataAdesao)
- [x] 2.2 Criar entidade `ContaGrafica` (id, clienteId, numeroConta, tipo MASTER/FILHOTE)
- [x] 2.3 Criar entidade `Custodia` (id, contaId, ticker, quantidade, precoMedio)
- [x] 2.4 Criar entidade `CestaRecomendacao` (id, dataCriacao, dataDesativacao, ativo)
- [x] 2.5 Criar entidade `ItemCesta` (id, cestaId, ticker, percentual)
- [x] 2.6 Criar entidade `HistoricoOperacao` (id, clienteId, ticker, tipo, quantidade, preco, data)
- [x] 2.7 Criar entidade `ExecucaoMotor` (id, dataReferencia, status, iniciadoEm, concluidoEm)
- [x] 2.8 Criar entidade `EventoKafka` (id, tipo, payload, status, criadoEm)
- [x] 2.9 Criar repositórios JPA para todas as entidades

## 3. Gestão de Clientes

- [x] 3.1 Implementar `ClienteService.aderir()`: validar CPF único, email, valorMensal >= 100; criar cliente, ContaGrafica e Custodia filhote
- [x] 3.2 Implementar `ClienteService.sair()`: alterar Ativo = false; validar se já inativo
- [x] 3.3 Implementar `ClienteService.alterarValorMensal()`: validar novo valor >= 100; atualizar
- [x] 3.4 Implementar `ClienteService.consultar()`: buscar por ID, retornar dados
- [x] 3.5 Criar `ClienteController` com endpoints POST /clientes, DELETE /clientes/{id}, PATCH /clientes/{id}/valor-mensal, GET /clientes/{id}
- [x] 3.6 Criar testes unitários para `ClienteService` (adesão válida, CPF duplicado, valor mínimo, saída, alteração)

## 4. Cesta de Recomendação

- [x] 4.1 Implementar `CestaService.criar()`: validar exatamente 5 ativos, soma = 100%, cada % > 0; desativar cesta anterior; criar nova cesta; disparar evento de rebalanceamento
- [x] 4.2 Implementar `CestaService.buscarAtiva()`: retornar cesta com status ativo
- [x] 4.3 Criar `CestaController` com endpoints POST /cestas e GET /cestas/ativa
- [x] 4.4 Criar testes unitários para `CestaService` (cesta válida, < 5 ativos, soma errada, % zero)

## 5. Leitor COTAHIST

- [x] 5.1 Implementar `CotahistParser` para ler arquivo de layout posicional fixo da B3
- [x] 5.2 Extrair campos: tipo registro, ticker (CODISI/CODNEG), preço de fechamento (PREULT), data do pregão
- [x] 5.3 Retornar mapa ticker → cotação mais recente (suporte a arquivo acumulativo anual)
- [x] 5.4 Lançar exceção descritiva se ativo da cesta não encontrado no arquivo
- [x] 5.5 Criar testes unitários do parser com arquivo COTAHIST de exemplo

## 6. Motor de Compra

- [x] 6.1 Implementar `MotorCompraService.executar(dataReferencia)`: verificar idempotência via ExecucaoMotor; retornar se já CONCLUIDO
- [x] 6.2 Implementar agrupamento: buscar clientes ativos, calcular parcela (valorMensal / 3), somar total consolidado
- [x] 6.3 Implementar cálculo de quantidade por ativo: valor_alvo = total × %, cotacao do COTAHIST, qtd_bruta = TRUNC(valor/cotacao), qtd_comprar = max(0, qtd_bruta - saldo_master)
- [x] 6.4 Implementar separação lote padrão vs fracionário (lote = qtd DIV 100 × 100; fracionario = qtd MOD 100; ticker + "F")
- [x] 6.5 Implementar registro das compras na conta master (HistoricoOperacao tipo COMPRA)
- [x] 6.6 Implementar distribuição proporcional: proporcao = parcela/total, qtd_cliente = TRUNC(total_disponivel × proporcao), residuo na master
- [x] 6.7 Implementar atualização de Custódia filhote (quantidade + preço médio) após distribuição
- [x] 6.8 Implementar scheduler Spring: `@Scheduled(cron = "0 0 9 5,15,25 * ?")` com ajuste para próximo dia útil se fim de semana
- [x] 6.9 Criar testes unitários do motor (agrupamento, cálculo de qtd, saldo negativo, distribuição, residuos)
- [x] 6.10 Criar testes de integração do fluxo completo (do cron até custódia filhote atualizada)

## 7. Preço Médio

- [x] 7.1 Implementar `PrecoMedioService.calcular()`: PM = (Qtd_Ant × PM_Ant + Qtd_Nova × Preco) / (Qtd_Ant + Qtd_Nova); usar BigDecimal com ROUND_DOWN; armazenar com 6 casas decimais
- [x] 7.2 Garantir que vendas não alteram preço médio (apenas diminuem quantidade)
- [x] 7.3 Criar testes unitários (primeira compra, compra adicional, venda não altera PM, compra após venda)

## 8. Fiscal — IR

- [x] 8.1 Implementar `FiscalService.calcularIRDedoDuro()`: valor_operacao × 0,00005; retornar DTO com campos da mensagem Kafka
- [x] 8.2 Implementar `FiscalService.calcularIRVenda()`: somar vendas do mês; se total <= 20.000 retornar isento; senão calcular 20% do lucro líquido (nunca negativo)
- [x] 8.3 Implementar `KafkaPublisher.publicar()`: enviar mensagem; em caso de falha salvar EventoKafka com status PENDENTE
- [x] 8.4 Integrar publicação de IR dedo-duro ao final da distribuição de cada cliente no motor
- [x] 8.5 Integrar publicação de IR venda ao final do rebalanceamento de cada cliente
- [x] 8.6 Criar testes unitários fiscal (IR dedo-duro, isento, tributado, prejuízo líquido)

## 9. Rebalanceamento

- [x] 9.1 Implementar `RebalanceamentoService.executar(cestaAntiga, cestaNova)`: identificar ativos que saíram, entraram e mudaram de percentual
- [x] 9.2 Implementar venda total de ativos removidos da cesta por cliente; atualizar custódia e registrar em HistoricoOperacao
- [x] 9.3 Implementar compra proporcional de novos ativos com valor obtido nas vendas
- [x] 9.4 Implementar ajuste de ativos com percentual alterado (vender excesso ou comprar deficit)
- [x] 9.5 Implementar `RebalanceamentoListener` com `@Async` para processar evento `CestaAlteradaEvent`
- [x] 9.6 Criar testes unitários do rebalanceamento (ativo removido, adicionado, percentual alterado, mantido)

## 10. Rentabilidade

- [x] 10.1 Implementar `RentabilidadeService.consultar(clienteId)`: buscar custódia filhote, obter cotações do COTAHIST, calcular valor atual por ativo
- [x] 10.2 Calcular P/L por ativo ((cotacao - PM) × qtd), P/L total, rentabilidade % e composição real
- [x] 10.3 Criar `RentabilidadeController` com endpoint GET /clientes/{id}/carteira
- [x] 10.4 Criar testes unitários de rentabilidade (carteira com lucro, com prejuízo, vazia)

## 11. Testes de Integração e Ajustes Finais

- [x] 11.1 Criar testes de integração com Testcontainers (PostgreSQL + Kafka)
- [x] 11.2 Validar cobertura de testes >= 70% (Jacoco)
- [x] 11.3 Revisar tratamento de erros: endpoints retornam códigos HTTP corretos e mensagens descritivas
- [x] 11.4 Validar precisão decimal em todos os cálculos financeiros (sem double/float)
- [x] 11.5 Documentar endpoints com Swagger/OpenAPI
