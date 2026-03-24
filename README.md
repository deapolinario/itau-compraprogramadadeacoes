# Compra Programada de Ações — Itaú Corretora

Sistema de investimento recorrente e automatizado em uma carteira recomendada de 5 ações (**Top Five**). O cliente define um valor mensal de aporte e o sistema executa as compras de forma consolidada, distribui os ativos proporcionalmente e gerencia rebalanceamentos quando a composição da carteira muda.

---

## Contexto de Negócio

O produto **Compra Programada** permite que clientes da Itaú Corretora invistam de forma disciplinada e automatizada, sem precisar acompanhar o mercado diariamente. O sistema:

- **Consolida** a demanda de todos os clientes ativos e executa compras em lote (≥ 100 ações) na B3, reduzindo custos operacionais
- **Distribui** os ativos proporcionalmente para a custódia individual de cada cliente
- **Rebalanceia** automaticamente as carteiras quando o Research altera a cesta Top Five
- **Apura** obrigações fiscais (IR dedo-duro e IR sobre vendas) e as publica em tópico Kafka

### Ciclo de Compra

As compras são executadas nos dias **5, 15 e 25** de cada mês às 09h. Se a data cair em fim de semana, executa na segunda-feira seguinte.

```
Valor mensal do cliente  →  Parcela (÷ 3)
Parcela por ativo        →  Quantidade = TRUNC(parcela × % cesta / preço)
Quantidade total         →  Lote padrão (≥ 100) + Fracionário (< 100)
Compra consolidada       →  Distribuição proporcional para cada cliente
```

### Regras Fiscais

| Evento | Alíquota | Gatilho |
|---|---|---|
| IR Dedo-Duro (IRRF) | 0,005% | Toda compra distribuída ao cliente |
| IR sobre Vendas | 20% sobre lucro líquido | Total de vendas no mês > R$ 20.000 |
| Isenção de IR | — | Total de vendas ≤ R$ 20.000 |

### Documentação de Produto e Arquitetura

| Documento | Descrição |
|---|---|
| [PRD](docs/prd/PRD.md) | Product Requirements Document completo |
| [Desafio Técnico](docs/documentacao_inicial/desafio-tecnico-compra-programada.md) | Especificação original do desafio |
| [Regras de Negócio](docs/documentacao_inicial/regras-negocio-detalhadas.md) | Detalhamento das regras de negócio |
| [Contratos de API](docs/documentacao_inicial/exemplos-contratos-api.md) | Exemplos de request/response |
| [Glossário](docs/documentacao_inicial/glossario-compra-programada.md) | Termos do domínio financeiro |
| [Layout COTAHIST](docs/documentacao_inicial/layout-cotahist-b3.md) | Especificação do arquivo B3 |
| [ADRs](docs/adr/README.md) | Decisões arquiteturais (9 ADRs) |

---

## Stack Tecnológica

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | Spring Boot | 3.2.3 |
| Persistência | Spring Data JPA + Hibernate | — |
| Banco de dados | PostgreSQL | 16 |
| Migrações | Flyway | 10.10.0 |
| Mensageria | Apache Kafka | 7.6.0 (Confluent) |
| Build | Maven | 3.x |
| Testes unitários | JUnit 5 + Mockito | — |
| Testes de integração | Testcontainers | 1.19.6 |
| Cobertura | Jacoco | 0.8.11 (mínimo 70%) |
| Documentação API | SpringDoc OpenAPI (Swagger) | 2.3.0 |
| Utilitários | Lombok, Jackson | — |

### Estrutura de Pacotes

```
src/main/java/br/com/itau/compraprogramada/
├── controller/          # REST Controllers (Clientes, Cesta, Rentabilidade)
├── service/             # Lógica de negócio
│   └── motor/           # Motor de Compra e Scheduler
├── domain/              # Entidades JPA
│   ├── cliente/
│   ├── cesta/
│   ├── custodia/
│   ├── fiscal/
│   └── motor/
├── repository/          # Spring Data JPA Repositories
├── cotahist/            # Parser do arquivo COTAHIST B3
├── dto/                 # Request/Response DTOs
├── exception/           # NegocioException + GlobalExceptionHandler
└── config/              # Kafka, Async, Swagger
```

---

## Pré-requisitos

- **Java 21** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- **Docker + Docker Compose** (para infraestrutura local)
- Arquivo COTAHIST da B3 no diretório `./cotacoes/` (para o Motor de Compra)

---

## Como Rodar Localmente

### 1. Subir a infraestrutura (PostgreSQL + Kafka)

```bash
docker compose up -d
```

Aguarde os health checks ficarem `healthy`:

```bash
docker compose ps
```

Os serviços sobem em:
- **PostgreSQL:** `localhost:5432` — banco `compraprogramada`, usuário `compra`, senha `compra123`
- **Kafka:** `localhost:9092`

### 2. Posicionar o arquivo de cotações

O Motor de Compra lê o preço de fechamento do arquivo COTAHIST diário da B3.
Baixe o arquivo no [site da B3](https://www.b3.com.br/pt_br/market-data-e-indices/servicos-de-dados/market-data/historico/mercado-a-vista/series-historicas/) e coloque na pasta `cotacoes/`:

```bash
mkdir -p cotacoes
# Exemplo com arquivo do dia:
cp ~/Downloads/COTAHIST_D220226.TXT cotacoes/
```

> O sistema aceita múltiplos arquivos (diários e anuais) e usa o registro mais recente por ticker.

### 3. Compilar e iniciar a aplicação

```bash
mvn spring-boot:run
```

Ou gerar o JAR e executar:

```bash
mvn package -DskipTests
java -jar target/compra-programada-1.0.0-SNAPSHOT.jar
```

A aplicação sobe em `http://localhost:8080`.

O Flyway aplica automaticamente as migrações de banco ao iniciar — não é necessário executar DDL manualmente.

### 4. Acessar a documentação interativa

```
http://localhost:8080/swagger-ui.html
```

### 5. Importar a collection do Insomnia

O projeto inclui uma collection pronta com todos os endpoints organizados em pastas e um fluxo completo de demonstração.

**Arquivo:** [`docs/insomnia/compra-programada.json`](docs/insomnia/compra-programada.json)

**Como importar:**
1. Abra o Insomnia
2. Clique em **Import** (ou `File → Import`)
3. Selecione o arquivo `docs/insomnia/compra-programada.json`
4. O ambiente **Local** já vem configurado com `base_url = http://localhost:8080`

**Conteúdo da collection:**

| Pasta | Requisições |
|---|---|
| **Clientes** | Aderir, consultar, alterar valor mensal, sair, erros (400/404) |
| **Cesta Top Five** | Criar cesta, buscar ativa, erros de validação (soma ≠ 100%, < 5 ativos) |
| **Carteira e Rentabilidade** | Consultar posição e P/L do cliente |
| **Fluxo Completo** | 8 requisições em ordem para demonstração end-to-end |

---

## API REST — Endpoints Principais

### Clientes

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/clientes` | Aderir ao produto (cadastrar cliente) |
| `DELETE` | `/clientes/{id}` | Sair do produto (desativar cliente) |
| `PATCH` | `/clientes/{id}/valor-mensal` | Alterar valor mensal de aporte |
| `GET` | `/clientes/{id}` | Consultar dados do cliente |
| `GET` | `/clientes/{id}/carteira` | Consultar carteira e rentabilidade |

### Cesta Top Five

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/cestas` | Criar nova cesta de recomendação |
| `GET` | `/cestas/ativa` | Buscar cesta ativa atual |

### Exemplos rápidos com curl

```bash
# 1. Aderir ao produto
curl -X POST http://localhost:8080/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "cpf": "12345678901",
    "email": "joao@email.com",
    "valorMensal": 3000.00
  }'

# 2. Criar cesta Top Five
curl -X POST http://localhost:8080/cestas \
  -H "Content-Type: application/json" \
  -d '{
    "itens": [
      { "ticker": "PETR4", "percentual": 30.00 },
      { "ticker": "VALE3", "percentual": 25.00 },
      { "ticker": "ITUB4", "percentual": 20.00 },
      { "ticker": "BBDC4", "percentual": 15.00 },
      { "ticker": "WEGE3", "percentual": 10.00 }
    ]
  }'

# 3. Consultar carteira e rentabilidade
curl http://localhost:8080/clientes/1/carteira
```

---

## Como Testar

### Testes Unitários (sem Docker)

Executa os 39 testes unitários e gera o relatório de cobertura Jacoco. Não requer infraestrutura.

```bash
mvn test
```

Relatório de cobertura HTML gerado em:
```
target/site/jacoco/index.html
```

**Resultado atual:** 39 testes | 0 falhas | cobertura de linhas **76,3%** (mínimo exigido: 70%)

### Verificação com Quality Gate de Cobertura

Inclui o check do Jacoco que falha o build se a cobertura de linhas cair abaixo de 70%:

```bash
mvn verify
```

### Testes de Integração (requer Docker)

Os testes de integração usam Testcontainers para subir PostgreSQL e Kafka em containers isolados.

```bash
# Executa apenas os testes de integração
mvn test -Dtest="**/integration/**" -DfailIfNoTests=false

# Executa tudo (unitários + integração)
mvn verify -P integration-tests
```

> Os testes de integração estão em `src/test/java/**/integration/` e são excluídos por padrão do `mvn test` para não exigir Docker em todo build.

### Suítes de Teste

| Classe | Testes | O que valida |
|---|---|---|
| `CotahistParserTest` | 4 | Parser B3, layout posicional de 245 chars, PREULT |
| `ClienteServiceTest` | 6 | Adesão, retirada, alteração de aporte, validações |
| `CestaServiceTest` | 4 | Composição (5 ativos, soma = 100%), publicação de evento |
| `FiscalServiceTest` | 9 | IR dedo-duro, IR vendas, fallback Kafka → banco |
| `PrecoMedioServiceTest` | 5 | Preço médio ponderado com 6 casas decimais |
| `MotorCompraServiceTest` | 4 | Idempotência, cálculo de quantidade, distribuição |
| `RebalanceamentoServiceTest` | 3 | Venda de removidos, ativo mantido, sem clientes |
| `RentabilidadeServiceTest` | 4 | P/L, rentabilidade %, carteira vazia, cliente inexistente |

---

## Variáveis de Configuração

Todas as propriedades estão em `src/main/resources/application.properties` e podem ser sobrescritas via variáveis de ambiente ou argumentos JVM:

| Propriedade | Padrão | Descrição |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/compraprogramada` | URL do banco |
| `spring.datasource.username` | `compra` | Usuário do banco |
| `spring.datasource.password` | `compra123` | Senha do banco |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Endereço do Kafka |
| `cotahist.diretorio` | `./cotacoes` | Diretório dos arquivos COTAHIST |
| `motor.compra.cron` | `0 0 9 5,15,25 * ?` | Cron do Motor de Compra |
| `kafka.topic.ir` | `ir-eventos` | Tópico Kafka para eventos de IR |

Exemplo sobrescrevendo via linha de comando:

```bash
java -jar target/compra-programada-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://prod-host:5432/compraprogramada \
  --cotahist.diretorio=/data/cotahist
```

---

## Decisões Arquiteturais

As principais decisões de design estão documentadas como ADRs em [`docs/adr/`](docs/adr/README.md):

| ADR | Decisão |
|---|---|
| [ADR-001](docs/adr/ADR-001-stack-tecnologica.md) | Spring Boot 3 + Java 21 |
| [ADR-002](docs/adr/ADR-002-cotahist-como-fonte-de-cotacoes.md) | COTAHIST B3 como fonte de cotações |
| [ADR-003](docs/adr/ADR-003-estrutura-master-filhote.md) | Estrutura de custódia Master/Filhote |
| [ADR-004](docs/adr/ADR-004-idempotencia-motor-de-compra.md) | Idempotência do Motor de Compra |
| [ADR-005](docs/adr/ADR-005-bigdecimal-para-calculos-financeiros.md) | BigDecimal para cálculos financeiros |
| [ADR-006](docs/adr/ADR-006-kafka-com-fallback-em-banco.md) | Kafka com fallback persistente |
| [ADR-007](docs/adr/ADR-007-rebalanceamento-assincrono.md) | Rebalanceamento assíncrono via Spring Events |
| [ADR-008](docs/adr/ADR-008-flyway-para-migracoes.md) | Flyway para migrações versionadas |
| [ADR-009](docs/adr/ADR-009-estrategia-de-testes.md) | Estratégia de testes e cobertura ≥ 70% |

---

## Parar a Infraestrutura

```bash
docker compose down          # para os containers
docker compose down -v       # para e remove os volumes (apaga dados)
```
