# ADR-008: Flyway para Migrações de Banco de Dados Versionadas

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O sistema possui 8 tabelas relacionadas com constraints, índices e dados de seed (conta MASTER). Sem controle de versão de schema, problemas comuns surgem em equipes:

- Divergência de schema entre ambientes (dev, staging, prod)
- Migrations aplicadas manualmente e não rastreadas
- Impossibilidade de rollback controlado
- `spring.jpa.hibernate.ddl-auto=create-drop` não é aceitável em produção

## Decisão

Usar **Flyway** para gestão de migrações, com convenção de nomenclatura `V{n}__{descricao}.sql`.

**Estrutura atual:**
```
src/main/resources/db/migration/
└── V1__schema_inicial.sql     ← cria todas as tabelas + seed da conta MASTER
```

**V1 inclui:**
- Criação das tabelas: `clientes`, `contas_graficas`, `custodias`, `cestas_recomendacao`, `itens_cesta`, `historico_operacoes`, `execucoes_motor`, `eventos_kafka`
- Índices de performance: `(conta_id, ticker)`, `(data_referencia)`, `(tipo)`
- Seed da conta MASTER: `INSERT INTO contas_graficas (numero, tipo) VALUES ('MASTER-001', 'MASTER')`
- Constraints: NOT NULL, FOREIGN KEY, UNIQUE onde necessário

**Dependências adicionadas:**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>10.10.0</version>  <!-- versão explícita: suporte ao PostgreSQL 16 -->
</dependency>
```

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| `ddl-auto=create-drop` | Destrói dados em cada restart — inaceitável em produção |
| `ddl-auto=update` | Hibernate não aplica remoções de coluna; schema pode divergir; sem auditoria |
| Liquibase | Funcionalidade equivalente para este caso; Flyway mais simples (SQL puro) |
| Migrations manuais | Sem rastreabilidade; propenso a erro humano; impossível automatizar |

## Consequências

**Positivas:**
- Schema versionado e auditável via tabela `flyway_schema_history`
- Aplicação sobe e migra automaticamente em qualquer ambiente
- SQL puro: legível por DBAs, sem abstração de DSL proprietária
- `flyway-database-postgresql` garante compatibilidade com PostgreSQL 16+

**Negativas:**
- Migrações são **irreversíveis por padrão** — correções exigem nova migration (`V2__...`)
- A versão `flyway-database-postgresql:10.10.0` deve ser atualizada manualmente quando o Spring Boot atualizar o `flyway-core`

**Convenção para evolução:**
- Nunca alterar scripts `V{n}` já aplicados em produção
- Correções retroativas: `V{n+1}__correcao_...sql`
- Dados de seed que mudam com frequência: script separado (`R__seed.sql` com prefixo repeatable, se necessário)
