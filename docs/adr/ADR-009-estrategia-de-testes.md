# ADR-009: Estratégia de Testes — Unitários + Integração Separados, Cobertura ≥ 70%

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O sistema possui lógica de negócio complexa (Motor de Compra, Preço Médio, Rebalanceamento, IR) que precisa ser validada automaticamente. Ao mesmo tempo, os testes de integração requerem infraestrutura (PostgreSQL, Kafka) que não está disponível em todos os ambientes de build (ex.: CI sem Docker).

Os requisitos de teste são:
1. Validar a lógica de negócio de forma rápida e independente de infraestrutura
2. Validar o comportamento end-to-end com banco real (opcionalmente)
3. Garantir cobertura mínima de 70% das linhas para manter qualidade

## Decisão

Adotar **duas camadas de teste com execução segregada**:

### Camada 1: Testes Unitários (sempre executados)

- Framework: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- Padrão: `@InjectMocks` + `@Mock` para dependências externas
- Localização: `src/test/java/**` (exceto `**/integration/**`)
- Execução: `mvn test` (Surefire exclui `**/integration/**`)

**Suites existentes:**
| Classe de Teste | Cenários | Foco |
|---|---|---|
| `CotahistParserTest` | 4 | Parser B3, formato posicional, tickers inexistentes |
| `ClienteServiceTest` | 6 | Adesão, retirada, alteração de valor mensal |
| `CestaServiceTest` | 4 | Validação de composição (5 ativos, soma 100%) |
| `FiscalServiceTest` | 9 | IR dedo-duro, IR venda, fallback Kafka |
| `PrecoMedioServiceTest` | 5 | Cálculo PM com 6 casas decimais |
| `MotorCompraServiceTest` | 4 | Idempotência, cálculo de quantidade, distribuição |
| `RebalanceamentoServiceTest` | 3 | Venda de removidos, sem alteração, sem clientes |
| `RentabilidadeServiceTest` | 4 | Lucro, prejuízo, carteira vazia, cliente inexistente |

**Total: 39 testes unitários**

### Camada 2: Testes de Integração (execução opcional)

- Framework: Testcontainers (PostgreSQL + Kafka) + `@SpringBootTest`
- Localização: `src/test/java/**/integration/**`
- Execução: `mvn verify -Dintegration.tests=true` (ou pipeline CI com Docker)
- Excluídos do Surefire por padrão:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/integration/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

### Cobertura: Jacoco ≥ 70% de linhas

```xml
<execution>
    <id>check</id>
    <goals><goal>check</goal></goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

**Resultado atual: 76.3% (555/727 linhas)**

Classes excluídas implicitamente da cobertura significativa: controllers (0% — sem `@SpringBootTest`), classes de configuração (`AsyncConfig`, `KafkaConfig`, `SwaggerConfig`), `@SpringBootApplication`.

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Apenas testes de integração | Lento; depende de Docker; dificulta feedback rápido no desenvolvimento |
| `@SpringBootTest` para tudo | Inicializa contexto Spring completo em cada suite — muito mais lento; requer banco |
| Cobertura por branch (não linha) | Branch coverage é mais difícil de atingir sem mocks extensivos; linha é padrão de mercado |
| Cobertura < 70% | Risco de regressão não detectada em lógica financeira crítica |

## Consequências

**Positivas:**
- Build rápido: `mvn test` executa 39 testes unitários em ~25 segundos sem infraestrutura
- `mvn verify` falha se cobertura < 70% — qualidade como portão de qualidade (quality gate)
- Separação clara: lógica de negócio testada isoladamente; infra testada em CI

**Negativas:**
- Controllers REST não têm cobertura unitária (requerem `MockMvc` com contexto Spring)
- Cobertura de branches (19.7%) é baixa — foco futuro para aumentar confiança
- Testcontainers nos testes de integração exige Docker no agente de CI

**Decisão de uso de `lenient()`:**
Stubs definidos no `@BeforeEach` mas não utilizados em todos os testes usam `lenient().when(...)` para evitar `UnnecessaryStubbingException` do Mockito Strict Mode — sem abrir mão do modo estrito para novos stubs dentro dos testes.
