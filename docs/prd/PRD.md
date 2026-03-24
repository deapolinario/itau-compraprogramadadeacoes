# Product Requirements Document (PRD)

## Sistema de Compra Programada de Ações - Itau Corretora

**Data:** Março de 2026
**Versão:** 1.0
**Status:** Em Planejamento

---

## 1. Executive Summary

### Problem Statement
Clientes da Itau Corretora enfrentam dificuldade em manter um plano de investimento consistente e automatizado em ações de qualidade. Decisões manuais de compra são demoradas, propensas a erros e não garantem a diversificação adequada. Há demanda por um produto que permita investimento recorrente, automatizado e com rebalanceamento inteligente.

### Proposed Solution
Desenvolver um **Sistema de Compra Programada de Ações** que automatiza completamente o ciclo de investimento: adesão do cliente → divisão do valor mensal em 3 parcelas → compra consolidada → distribuição proporcional → rebalanceamento automático. O sistema se integra com a B3 (Bolsa de Valores), lê cotações reais (COTAHIST) e gerencia impostos (IR dedo-duro e IR sobre vendas) de forma transparente.

### Success Criteria
- ✅ **Taxa de adesão:** 500+ clientes nos primeiros 3 meses
- ✅ **Acurácia em cálculos financeiros:** 100% (zero erros em distribuição, preço médio, IR)
- ✅ **Disponibilidade do motor de compra:** 99,9% (0 falhas nos dias 5, 15, 25)
- ✅ **Satisfação do cliente (NPS):** ≥ 50
- ✅ **Tempo de resposta da API:** p95 < 200ms para consultas de carteira
- ✅ **Cobertura de testes:** ≥ 70% de testes unitários e de integração

---

## 2. User Experience & Functionality

### 2.1 User Personas

#### Persona 1: João Silva (Investidor Iniciante)
- **Idade:** 32 anos | **Profissão:** Analista de Sistemas | **Renda:** R$ 8k/mês
- **Necessidade:** Investir mensalmente sem decidir qual ação comprar cada mês
- **Dor:** Falta de tempo e conhecimento para análise de mercado
- **Motivação:** Construir patrimônio a longo prazo com automação

#### Persona 2: Maria Costa (Investidora Experiente)
- **Idade:** 45 anos | **Profissão:** Gestora de Projetos | **Renda:** R$ 15k/mês
- **Necessidade:** Acompanhar rentabilidade em tempo real e rebalancear quando necessário
- **Dor:** Processos manuais de rebalanceamento são complexos e custosos
- **Motivação:** Maximizar retorno com risco controlado

#### Persona 3: Admin Itau (Analista de Renda Variável)
- **Idade:** 38 anos | **Profissão:** Research Analyst | **Departamento:** Research
- **Necessidade:** Gerenciar a cesta Top Five e acompanhar aderências dos clientes
- **Dor:** Atualizar recomendações sem ferramenta integrada
- **Motivação:** Escalar a recomendação para todos os clientes com um clique

---

### 2.2 User Stories

#### Epic 1: Adesão e Gestão do Cliente

**US-001:** Como cliente iniciante, quero aderir ao produto fornecendo meus dados básicos e valor mensal, para que o sistema crie automaticamente minha conta e comece a investir em meu nome.

**Acceptance Criteria:**
- ✅ Validar CPF (único no sistema, formato válido)
- ✅ Validar email (formato válido)
- ✅ Validar valor mensal (mínimo R$ 100, máximo TBD)
- ✅ Criar Conta Gráfica Filhote automaticamente
- ✅ Criar Custodia Filhote vazia (saldo inicial = 0)
- ✅ Retornar confirmação com ID do cliente e número da conta
- ✅ Registrar data de adesão

**US-002:** Como cliente, quero alterar meu valor mensal de aporte a qualquer momento, para ajustar meu investimento conforme minha situação financeira.

**Acceptance Criteria:**
- ✅ Validar novo valor (mínimo R$ 100)
- ✅ Manter histórico de alterações
- ✅ Aplicar novo valor na próxima data de compra programada
- ✅ Enviar confirmação via email

**US-003:** Como cliente, quero sair do produto a qualquer momento, mantendo minha carteira, para que possa dar prosseguimento de forma independente.

**Acceptance Criteria:**
- ✅ Atualizar status do cliente para Inativo
- ✅ Não desligar automaticamente; manter posição em custodia
- ✅ Excluir das futuras compras programadas
- ✅ Permitir consulta de carteira mesmo após saída
- ✅ Enviar confirmação

---

#### Epic 2: Motor de Compra Programada

**US-004:** Como sistema, quero executar o motor de compra nos dias 5, 15 e 25 (ou próximo dia útil se feriado), para que cada cliente receba 1/3 de seu aporte mensal de forma automática.

**Acceptance Criteria:**
- ✅ Agrupar clientes ativos (Ativo = true)
- ✅ Calcular 1/3 do valor mensal de cada cliente
- ✅ Somar valores consolidados por ativo conforme % da cesta Top Five
- ✅ Obter cotação de fechamento do último pregão (arquivo COTAHIST)
- ✅ Descontar saldo residual da custodia master
- ✅ Registrar ordem de compra (separar lote padrão vs fracionário)
- ✅ Atualizar custodia master com novas compras
- ✅ Distribuir ativos para custodias filhotes proporcionalmente
- ✅ Manter residuos na custodia master para próxima compra
- ✅ Calcular IR dedo-duro (0,005%) e publicar no Kafka
- ✅ Registrar log detalhado (auditoria)

**US-005:** Como cliente, quero consultar minha carteira em tempo real, para ver quantas ações possuo, preço médio e valor atual.

**Acceptance Criteria:**
- ✅ Exibir quantidade de cada ativo
- ✅ Exibir preço médio de aquisição
- ✅ Exibir cotação atual (obtida de fonte externa ou cached)
- ✅ Exibir valor de mercado (Qtd x Cotação)
- ✅ Calcular P/L por ativo: (Cotação - PM) x Qtd
- ✅ Calcular P/L total da carteira
- ✅ Calcular rentabilidade %: ((Valor Atual - Valor Investido) / Valor Investido) x 100
- ✅ Exibir composição % real da carteira
- ✅ Tempo de resposta: < 200ms

---

#### Epic 3: Rebalanceamento

**US-006:** Como admin, quero alterar a cesta Top Five (5 ações + percentuais), para que o sistema rebalanceie automaticamente as carteiras de todos os clientes.

**Acceptance Criteria:**
- ✅ Validar exatamente 5 ações
- ✅ Validar soma de percentuais = 100%
- ✅ Desativar cesta anterior (DataDesativacao)
- ✅ Criar nova cesta ativa
- ✅ Disparar rebalanceamento assíncrono para cada cliente ativo
- ✅ Vender ativos que saíram da cesta
- ✅ Comprar ativos que entraram na cesta
- ✅ Rebalancear ativos que mudaram de percentual
- ✅ Calcular IR sobre vendas (se > R$ 20k no mês)
- ✅ Publicar evento Kafka

**US-007:** Como cliente investidor experiente, quero que o sistema rebalanceie minha carteira quando um ativo diverge significativamente da proporção alvo, para que eu mantenha o risco sob controle.

**Acceptance Criteria:**
- ✅ Monitorar desvio de proporção (limiar sugerido: 5%)
- ✅ Identificar ativos sobre-alocados e sub-alocados
- ✅ Gerar ordem de venda (ativos sobre-alocados)
- ✅ Gerar ordem de compra (ativos sub-alocados)
- ✅ Aplicar automaticamente ou solicitar confirmação (TBD)
- ✅ Registrar operação e impacto de IR

---

#### Epic 4: Acompanhamento de Rentabilidade

**US-008:** Como cliente, quero acompanhar a rentabilidade detalhada da minha carteira ao longo do tempo, para entender se estou atingindo meus objetivos de investimento.

**Acceptance Criteria:**
- ✅ Exibir saldo total (Σ Qtd x Cotação)
- ✅ Exibir valor investido acumulado
- ✅ Exibir P/L por ativo com valores e percentuais
- ✅ Exibir P/L total
- ✅ Exibir rentabilidade % acumulada
- ✅ Exibir histórico de evolução (gráfico de linha: valor carteira ao longo do tempo)
- ✅ Permitir filtro por período (mês, trimestre, ano, customizado)
- ✅ Exibir contribuição percentual de cada ativo
- ✅ Comparar com benchmark (Ibovespa ou índice de referência TBD)

---

### 2.3 Non-Goals (O que NÃO está no escopo MVP)

- ❌ Simulador de cenários ("e se eu investisse X em vez de Y?")
- ❌ Integrações com plataformas de pagamento (PIX, TED) — validar transferências manualmente
- ❌ App mobile (apenas web/API REST no MVP)
- ❌ Rebalanceamento automático por desvio (apenas por mudança de cesta)
- ❌ Suporte a outras classes de ativos (fundos, ETFs, BDRs) — apenas ações vista
- ❌ Compensação de prejuízos entre meses — apenas cálculo mensal
- ❌ Análise preditiva ou IA (apenas automação determinística)

---

## 3. Technical Specifications

### 3.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENTE (Web/Mobile)                    │
│                REST API (Springdoc OpenAPI)                │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│          Java 21 + Spring Boot 3.x Backend                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │
│  │  REST Layer  │ │ Service Layer│ │ Data Layer   │         │
│  │ (@RestCtrlr) │ │ (@Service)   │ │ (Spring JPA) │         │
│  └──────────────┘ └──────────────┘ └──────────────┘         │
│                                                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │   Motor de Compra Programada (@Scheduled)       │       │
│  │   Dispara: Dias 5, 15, 25 (Spring Scheduler)    │       │
│  └──────────────────────────────────────────────────┘       │
│                                                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │   Motor de Rebalanceamento (Async @Async)       │       │
│  │   Dispara: Mudança de cesta ou desvio           │       │
│  └──────────────────────────────────────────────────┘       │
└────────────┬─────────────────┬──────────────┬────────────────┘
             │                 │              │
    ┌────────▼────────┐  ┌────▼────────┐  ┌─▼────────────┐
    │    MySQL        │  │   Kafka     │  │  Arquivo     │
    │  (Banco de      │  │ (Mensageria)│  │ COTAHIST B3  │
    │   Dados)        │  │             │  │ (Cotações)   │
    └─────────────────┘  └─────────────┘  └──────────────┘
```

### 3.2 Core Services

#### CompraService
- Agrupamento de clientes ativos
- Cálculo de valores consolidados
- Obtenção de cotações (COTAHIST parser)
- Execução de ordem de compra
- Distribuição para custodias filhotes
- Cálculo de preço médio
- Publicação de IR no Kafka

#### CustodiaService
- CRUD de custodias (master e filhote)
- Atualização de posições
- Cálculo de P/L
- Cálculo de rentabilidade

#### CotacaoService
- Parser de arquivo COTAHIST da B3
- Busca de cotação por ticker e data
- Cache de cotações (tempo real)

#### RebalanceamentoService
- Identificar ativos que entraram/saíram
- Gerar ordens de venda/compra
- Atualizar custodias
- Cálculo de IR sobre vendas

#### IRService
- Cálculo de IR dedo-duro (0,005%)
- Cálculo de IR sobre vendas (20% se > R$ 20k)
- Publicação em tópico Kafka

### 3.3 Database Model (Simplificado)

```sql
Clientes
├─ ClienteId (PK)
├─ Nome
├─ CPF (UNIQUE)
├─ Email
├─ ValorMensal
├─ Ativo
├─ DataAdesao
└─ DataSaida

ContasGraficas
├─ ContaId (PK)
├─ ClienteId (FK)
├─ Tipo (MASTER / FILHOTE)
├─ NumeroConta
└─ DataCriacao

Custodias
├─ CustodiaId (PK)
├─ ContaId (FK)
├─ Ticker
├─ Quantidade
├─ PrecoMedio
├─ DataAtualizacao

CestasRecomendadas
├─ CestaId (PK)
├─ DataCriacao
├─ DataDesativacao
├─ Ativa
└─ [Ticker1, %, Ticker2, %, ...]

OrdensCompra
├─ OrdemId (PK)
├─ DataExecucao
├─ Ticker
├─ Quantidade
├─ LotePadrao (Qtd)
├─ Fracionario (Qtd)
├─ Cotacao
└─ ValorTotal

Distribuicoes
├─ DistribuicaoId (PK)
├─ OrdemId (FK)
├─ ClienteId (FK)
├─ Ticker
├─ Quantidade
├─ ValorDistribuicao
└─ DataDistribuicao

EventosIR
├─ EventoId (PK)
├─ TipoIR (DEDO_DURO / VENDA)
├─ ClienteId (FK)
├─ Valor
├─ DataCalculo
└─ PublicadoKafka
```

### 3.4 Integration Points

#### B3 COTAHIST
- **Endpoint:** Arquivo TXT baixado manualmente (ou via web scraping)
- **Localização:** Pasta `/cotacoes/` no projeto
- **Formato:** COTAHIST_D20260225.TXT
- **Parser:** Ler campos fixos conforme layout B3

#### Kafka
- **Brokers:** localhost:9092 (via Docker Compose)
- **Tópicos:**
  - `ir-dedo-duro` (IR em compras)
  - `ir-venda` (IR em rebalanceamentos)
- **Producer:** IRService
- **Consumer:** (externo, para envio à Receita Federal)

#### MySQL
- **Host:** localhost:3306
- **Database:** `compra_programada`
- **User:** root / password (ou variáveis de ambiente)
- **Pool de conexões:** Mínimo 5, máximo 20

---

### 3.5 API REST Endpoints

#### Cliente

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/clientes/adesao` | Aderir ao produto |
| POST | `/api/clientes/{id}/saida` | Sair do produto |
| PUT | `/api/clientes/{id}/valor-mensal` | Alterar aporte mensal |
| GET | `/api/clientes/{id}/carteira` | Consultar carteira atual |
| GET | `/api/clientes/{id}/rentabilidade` | Consultar rentabilidade |
| GET | `/api/clientes/{id}/historico-operacoes` | Histórico de compras/vendas |

#### Admin

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/admin/cestas` | Cadastrar nova cesta |
| GET | `/api/admin/cestas/atual` | Obter cesta ativa |
| GET | `/api/admin/cestas/historico` | Histórico de cestas |
| GET | `/api/admin/clientes` | Listar todos os clientes |
| GET | `/api/admin/compras/dia/{data}` | Ver execução de compra |

#### Motor (Interno)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/motor/comprar` | Executar motor de compra (background) |
| POST | `/api/motor/rebalancear` | Executar rebalanceamento (background) |

---

### 3.6 Security & Compliance

- **Autenticação:** JWT (access_token + refresh_token)
- **Autorização:** Role-based (Cliente, Admin)
- **Validação de entrada:** Sanitização de CPF, email, valores monetários
- **Encriptação:** Dados sensíveis (CPF) criptografados em repouso (AES-256)
- **Auditoria:** Log de todas as operações (cliente, admin, motor)
- **GDPR:** Permitir export/delete de dados do cliente
- **Conformidade Fiscal:** Registros de IR publicados no Kafka (rastreabilidade para Receita Federal)
- **Rate Limiting:** 100 req/min por cliente para APIs de leitura

### 3.7 Technology Stack

| Componente | Tecnologia | Versão |
|---|---|---|
| **Backend** | Java + Spring Boot | 21 LTS + 3.3.x |
| **Web Framework** | Spring Web (REST) | 3.3.x |
| **ORM & Data Access** | Spring Data JPA + Hibernate | 3.3.x + 6.x |
| **Banco de Dados** | MySQL | 8.0+ |
| **Mensageria** | Apache Kafka + Spring Kafka | 3.6+ + 3.3.x |
| **Scheduler** | Spring Scheduler + Quartz | 3.3.x + 2.x |
| **Async Tasks** | Spring Async (@Async) | 3.3.x |
| **API Documentation** | Springdoc OpenAPI | 2.0+ |
| **Testing** | JUnit 5 + Mockito + TestContainers | 5.9+ + 5.0+ + 1.19+ |
| **Build Tool** | Maven | 3.9+ |
| **Containerization** | Docker + Docker Compose | Latest |
| **Security** | Spring Security + JWT (jjwt) | 6.x + 0.12+ |
| **Validation** | Spring Validation (Jakarta Bean Validation) | 3.3.x + 3.0+ |
| **Logging** | SLF4J + Logback | 2.0+ + 1.4+ |
| **JSON Processing** | Jackson | 2.15+ |
| **Cache** | Spring Cache (Caffeine) | 3.3.x + 3.1+ |

### 3.8 Development Environment

```dockerfile
# Dockerfile base para Java 21
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/compra_programada
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
    depends_on:
      - mysql
      - kafka

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=compra_programada
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

volumes:
  mysql_data:
```

### 3.9 Project Structure & Dependencies

#### Maven Project Structure
```
compra-programada/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/itau/compraprogramada/
│   │   │       ├── CompraCompradaApplication.java (Main)
│   │   │       ├── config/
│   │   │       │   ├── KafkaConfig.java
│   │   │       │   ├── SchedulerConfig.java
│   │   │       │   └── SecurityConfig.java
│   │   │       ├── domain/
│   │   │       │   ├── entities/
│   │   │       │   │   ├── Cliente.java
│   │   │       │   │   ├── ContaGrafica.java
│   │   │       │   │   ├── Custodia.java
│   │   │       │   │   ├── CestaRecomendada.java
│   │   │       │   │   └── ...
│   │   │       │   └── enums/
│   │   │       │       ├── TipoContaEnum.java
│   │   │       │       └── StatusClienteEnum.java
│   │   │       ├── dto/
│   │   │       │   ├── ClienteRequestDTO.java
│   │   │       │   ├── CarteiraResponseDTO.java
│   │   │       │   └── ...
│   │   │       ├── repository/
│   │   │       │   ├── ClienteRepository.java (Spring Data JPA)
│   │   │       │   ├── CustodiaRepository.java
│   │   │       │   └── ...
│   │   │       ├── service/
│   │   │       │   ├── CompraService.java
│   │   │       │   ├── CustodiaService.java
│   │   │       │   ├── CotacaoService.java
│   │   │       │   ├── RebalanceamentoService.java
│   │   │       │   ├── IRService.java
│   │   │       │   └── ClienteService.java
│   │   │       ├── controller/
│   │   │       │   ├── ClienteController.java (@RestController)
│   │   │       │   ├── CestaController.java
│   │   │       │   └── AdminController.java
│   │   │       ├── event/
│   │   │       │   ├── IRDedoDuroEvent.java
│   │   │       │   └── IRVendaEvent.java
│   │   │       ├── listener/
│   │   │       │   └── KafkaEventListener.java (@KafkaListener)
│   │   │       ├── scheduler/
│   │   │       │   └── CompraScheduler.java (@Scheduled)
│   │   │       └── exception/
│   │   │           ├── ClienteException.java
│   │   │           └── CompraException.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── db/
│   │       │   ├── migration/ (Flyway)
│   │       │   ├── V1__init.sql
│   │       │   └── V2__add_indexes.sql
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/
│       │   └── com/itau/compraprogramada/
│       │       ├── service/
│       │       │   ├── CompraServiceTest.java (JUnit 5)
│       │       │   └── CustodiaServiceTest.java
│       │       ├── controller/
│       │       │   └── ClienteControllerTest.java (@WebMvcTest)
│       │       └── integration/
│       │           └── CompraIntegrationTest.java (TestContainers)
│       └── resources/
│           └── application-test.yml
├── docker-compose.yml
├── Dockerfile
├── README.md
└── .github/
    └── workflows/
        └── build-and-test.yml (CI/CD)
```

#### Exemplo de pom.xml (Principais Dependências)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.itau</groupId>
  <artifactId>compra-programada</artifactId>
  <version>1.0.0</version>
  <name>Sistema de Compra Programada de Ações</name>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
    <relativePath/>
  </parent>

  <properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <springdoc.version>2.0.2</springdoc.version>
  </properties>

  <dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Kafka -->
    <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- API Documentation -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>${springdoc.version}</version>
    </dependency>

    <!-- JWT -->
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.3</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.3</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.3</version>
      <scope>runtime</scope>
    </dependency>

    <!-- Quartz Scheduler -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>

    <!-- Cache: Caffeine -->
    <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Logging -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-logging</artifactId>
    </dependency>

    <!-- Utilities -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers</artifactId>
      <version>1.19.3</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>mysql</artifactId>
      <version>1.19.3</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>kafka</artifactId>
      <version>1.19.3</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.10</version>
        <executions>
          <execution>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 4. AI & Integration Requirements

### 4.1 Cotações em Tempo Real

- Integração com arquivo COTAHIST da B3 (atualização diária)
- Cache em memória com TTL de 24h
- Fallback: Usar última cotação disponível se arquivo não for atualizado

### 4.2 Automação de Processos

- **Scheduler:** Spring Scheduler (@Scheduled) + Quartz para disparar motor de compra nos dias 5/15/25
- **Background Jobs:** Spring Async (@Async) para processar rebalanceamentos assincronamente
- **Event-Driven:** Spring Kafka para publicar/consumir eventos de IR
- **Message Publishing:** KafkaTemplate para envio de eventos de IR

---

## 5. Non-Functional Requirements

### Performance
- **API Latency:** p95 < 200ms (leitura), p95 < 500ms (escrita)
- **Motor de Compra:** Execução em < 5 minutos para até 10k clientes
- **Throughput:** Suportar 1.000 req/s em picos

### Availability
- **Uptime:** 99,9% (46 min/mês de downtime aceitável)
- **RTO (Recovery Time Objective):** < 15 minutos
- **RPO (Recovery Point Objective):** < 1 minuto

### Scalability
- Database: Suportar 1 milhão de registros de custodia
- API: Escalável horizontalmente (stateless)
- Kafka: Replicação com factor=3

### Reliability
- **Testes:** Cobertura mínima 70% (unitários + integração)
- **Circuit Breaker:** Para chamadas a B3 COTAHIST
- **Retry Logic:** Exponential backoff em falhas transitórias

---

## 6. Risks & Mitigation

### Risk 1: Parse Incorreto do COTAHIST
**Impacto:** Alto (cálculos de compra errados)
**Probabilidade:** Média
**Mitigação:** Testes automatizados com múltiplos formatos de arquivo; validação de campos; alertas se cotação for zero/negativa

### Risk 2: Falta de Sincronização Kafka-DB
**Impacto:** Alto (auditoria comprometida, não rastreia IR)
**Probabilidade:** Média
**Mitigação:** Transações ACID; persistir IR no DB antes de publicar Kafka; replay de eventos

### Risk 3: Desvio de Proporcionalidade em Distribuição
**Impacto:** Crítico (clientes recebem quantidade errada)
**Probabilidade:** Baixa
**Mitigação:** Validação matemática (soma distribuição = soma comprada); testes de integração com múltiplos cenários

### Risk 4: Problema com Cotação Desatualizada
**Impacto:** Médio (preço para cálculo pode estar desatualizado)
**Probabilidade:** Média
**Mitigação:** Alertas se arquivo for anterior a 2 pregões; fallback manual; validação de data

### Risk 5: Limite de Conhecimento em Lei Fiscal
**Impacto:** Crítico (IR calculado incorretamente)
**Probabilidade:** Média
**Mitigação:** Documentação fiscal validada por contador; testes com cenários conhecidos; revisão por especialista

---

## 7. Roadmap de Implementação

### **Fase 1: MVP (Semanas 1-6)**

#### Sprint 1-2: Infraestrutura & Core Models
- ✅ Setup Java 21 + Spring Boot + Maven + MySQL + Docker Compose
- ✅ Modelos de entidades JPA (Cliente, Conta, Custodia, Cesta)
- ✅ Migrations Flyway/Liquibase
- ✅ Autenticação JWT com Spring Security

#### Sprint 3-4: API Cliente & Adesão
- ✅ Endpoints: Adesão, Saída, Alterar Aporte
- ✅ Validações (CPF, Email, Valor)
- ✅ CompraService (estrutura básica)
- ✅ Parser COTAHIST

#### Sprint 5-6: Motor de Compra & Distribuição
- ✅ Scheduler (Spring Scheduler + Quartz) para disparar nos dias 5/15/25
- ✅ Lógica de cálculo de compra consolidada
- ✅ Distribuição para custodias filhotes
- ✅ Cálculo de preço médio
- ✅ Publicação IR no Kafka via KafkaTemplate
- ✅ Testes unitários e de integração com Testcontainers (cobertura ≥ 70%)

### **Fase 2: Consultas & Rentabilidade (Semanas 7-9)**

- ✅ API: Consultar Carteira (GET /carteira)
- ✅ API: Rentabilidade Detalhada (GET /rentabilidade)
- ✅ Cálculo de P/L por ativo e total
- ✅ Rentabilidade percentual
- ✅ Gráficos de evolução (backend json, frontend TBD)

### **Fase 3: Rebalanceamento (Semanas 10-12)**

- ✅ Admin API: Cadastrar/Alterar Cesta Top Five
- ✅ Validações de cesta (5 ações, 100%)
- ✅ Disparo automático de rebalanceamento
- ✅ Lógica de venda/compra de rebalanceamento
- ✅ IR sobre vendas (regra dos R$ 20k)
- ✅ Testes de cenários complexos

### **Fase 4: Qualidade & Deploy (Semanas 13-14)**

- ✅ Testes de integração (banco + Kafka)
- ✅ Testes de carga (1k clientes)
- ✅ Springdoc OpenAPI documentação (acessível em /swagger-ui.html)
- ✅ README com instruções de setup (incluindo docker-compose up)
- ✅ CI/CD (GitHub Actions ou similar - build, test, deploy)
- ✅ Deploy em staging

### **Fase 5: Diferenciais (Após MVP)**

- 🔜 Rebalanceamento por desvio de proporção (automático)
- 🔜 Frontend web (React/Vue para consulta de carteira)
- 🔜 Compensação de prejuízos entre meses
- 🔜 Observabilidade (Prometheus, Grafana)
- 🔜 Alertas de anomalias (compra não executada, etc)

---

## 8. Success Metrics & KPIs

### Business Metrics
| Métrica | Target | Frequência |
|---------|--------|------------|
| Clientes ativos | 500 em 3 meses | Mensal |
| AUM (Assets Under Management) | R$ 5M em 6 meses | Mensal |
| Taxa de abandono (churn) | < 5% ao mês | Mensal |
| NPS (Net Promoter Score) | ≥ 50 | Trimestral |
| Ticket médio | R$ 3k/mês | Mensal |

### Technical Metrics
| Métrica | Target | Frequência |
|---------|--------|------------|
| Uptime | 99,9% | Contínuo |
| Latência API (p95) | < 200ms | Contínuo |
| Erro em cálculos | 0% | Por execução |
| Cobertura de testes | ≥ 70% | Por release |
| Tempo execução motor | < 5 min (10k clientes) | Por execução |

---

## 9. Definition of Done

Um requisito é considerado "pronto" quando:

✅ Código implementado e revisado (peer review)
✅ Testes unitários/integração com Testcontainers, cobertura ≥ 70%
✅ Documentação técnica (código comments e Javadoc onde necessário)
✅ API documentada no Springdoc OpenAPI (endpoints acessíveis em /swagger-ui.html)
✅ Validado em staging (docker-compose + banco de dados de teste)
✅ Nenhum bug crítico ou alta prioridade aberto

---

## 10. Assumptions & Constraints

### Assumptions
- Clientes têm CPF único e válido
- Cotação COTAHIST atualizada diariamente (dias de pregão)
- Kafka disponível e confiável
- MySQL com backup diário
- Equipe com experiência em Java 21, Spring Boot e desenvolvimento de sistemas distribuídos

### Constraints
- Budget: Infraestrutura mínima (1 servidor app, 1 DB, Kafka)
- Timeline: MVP em 14 semanas
- Regulatória: Conformidade com regras fiscais da Receita Federal
- Técnica: Deve suportar até 10k clientes no MVP

---

## 11. Approved By

| Papel | Nome | Data | Assinatura |
|-------|------|------|-----------|
| Product Owner | [TBD] | | |
| Tech Lead | [TBD] | | |
| Sponsor | [TBD] | | |

---

**Documento compilado em:** Março de 2026
**Próxima revisão:** [Data TBD]
