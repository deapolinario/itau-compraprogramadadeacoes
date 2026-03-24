# ADR-001: Stack Tecnológica — Spring Boot 3 + Java 21

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O sistema de Compra Programada de Ações precisa de um backend robusto capaz de:
- Processar compras recorrentes em datas fixas (agendamento)
- Integrar com Kafka para eventos fiscais
- Persistir dados financeiros com transações ACID
- Ser mantido por time de engenharia familiar ao ecossistema Java/Spring

A escolha da stack base define padrões de toda a implementação.

## Decisão

Adotar **Spring Boot 3.2.x** com **Java 21** como stack principal.

Componentes selecionados:
- `spring-boot-starter-web` — API REST
- `spring-boot-starter-data-jpa` — persistência via Hibernate
- `spring-boot-starter-validation` — validação de entrada
- `spring-kafka` — integração com Apache Kafka
- `springdoc-openapi` — documentação Swagger automática
- `lombok` — redução de boilerplate
- `flyway` — migrações de banco de dados versionadas

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Quarkus | Menor familiaridade do time; ecossistema Spring mais maduro para este domínio |
| Micronaut | Idem; compilação AOT com Lombok tem fricção |
| Java 17 LTS | Java 21 é LTS com virtual threads (Project Loom), relevante para I/O intensivo futuro |
| Jakarta EE puro | Mais verboso sem ganho funcional; ecossistema Spring oferece mais produtividade |

## Consequências

**Positivas:**
- Convenções Spring Boot reduzem decisões de baixo nível (auto-configuration)
- Java 21 LTS garante suporte de longo prazo
- `@Scheduled`, `@Async`, `@EventListener` nativos cobrem os requisitos de agendamento e eventos
- Equipe produtiva desde o primeiro dia

**Negativas:**
- Startup time maior que alternativas reativas/nativas
- Footprint de memória mais alto que Quarkus/Micronaut
- Spring Boot 3 exige Java 17+ — sem suporte a versões mais antigas

**Riscos mitigados:**
- A escolha do Spring Boot como padrão de mercado garante disponibilidade de profissionais e suporte da comunidade.
