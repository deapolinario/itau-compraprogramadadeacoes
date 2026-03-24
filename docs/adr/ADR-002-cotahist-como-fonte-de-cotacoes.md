# ADR-002: COTAHIST B3 como Fonte de Cotações de Fechamento

**Status:** Aceito
**Data:** 2026-03-22
**Decisores:** Equipe de Engenharia

---

## Contexto

O Motor de Compra precisa do preço de fechamento de cada ativo para calcular quantas ações comprar com o valor mensal do cliente. Existem três abordagens possíveis:

1. API de cotações em tempo real (ex.: B3 Market Data, Bloomberg, Refinitiv)
2. Arquivo COTAHIST diário disponibilizado gratuitamente pela B3
3. Banco de dados interno de preços atualizado por outro serviço

A escolha impacta custo, complexidade de integração, latência e confiabilidade.

## Decisão

Utilizar o **arquivo COTAHIST da B3** como única fonte de cotações.

O arquivo é:
- Disponibilizado gratuitamente no site da B3 após o fechamento do pregão
- Formato posicional fixo de 245 caracteres por linha
- Contém preços de fechamento (PREULT), abertura, máximo, mínimo e médio
- Suporta arquivos diários (`COTAHIST_Dddmmaa.TXT`) e anuais (`COTAHIST_Aano.TXT`)

O componente `CotahistParser` lê o arquivo posicionalmente, extraindo apenas os campos necessários (CODBDI, CODNEG, TPMERC, PREULT), sem parsear os 245 caracteres completos desnecessariamente.

A convenção adotada: diretório configurável via `cotahist.diretorio` (default `./cotacoes`). O sistema varre todos os arquivos `COTAHIST*.TXT` e usa o registro com a maior data disponível por ticker — suportando tanto arquivos diários quanto acumulativos anuais.

## Mapeamento de Campos (0-based)

| Campo | Posição | Tamanho | Descrição |
|---|---|---|---|
| TIPREG | 0 | 2 | Tipo de registro (`01` = detalhe) |
| DATPRE | 2 | 8 | Data do pregão (AAAAMMDD) |
| CODBDI | 10 | 2 | Código BDI (`02` = lote padrão, `96` = fracionário) |
| CODNEG | 12 | 12 | Código de negociação (ticker) |
| TPMERC | 24 | 3 | Tipo de mercado (`010` = à vista, `020` = fracionário) |
| PREULT | 108 | 13 | Preço de fechamento (inteiro, 2 casas implícitas) |

## Alternativas Consideradas

| Alternativa | Motivo de descarte |
|---|---|
| API B3 Market Data | Custo elevado; requer contrato e credencial; complexidade de integração |
| Yahoo Finance / Alpha Vantage | Dependência de terceiro não regulado; instabilidade histórica; sem SLA |
| Banco interno de preços | Duplicidade de responsabilidade; requer serviço adicional de ingestion |

## Consequências

**Positivas:**
- Zero custo de licença — arquivo público da B3
- Confiabilidade: dado oficial do órgão regulador
- Retroatividade: arquivos históricos permitem reprocessamento
- Sem dependência de rede em runtime — apenas leitura de arquivo local

**Negativas:**
- Latência D+0: o preço só está disponível após o fechamento do pregão (~18h)
- O sistema operacional precisa baixar e posicionar o arquivo antes da execução do motor (responsabilidade operacional, não da aplicação)
- Parsing de formato fixo é frágil a mudanças no layout pela B3 (embora raro)

**Convenção de filtragem:**
- Apenas CODBDI `02` (lote padrão) e `96` (fracionário) são aceitos
- Apenas TPMERC `010` (mercado à vista) e `020` (fracionário) são aceitos
- Outros tipos (opções, termo, futuro) são descartados silenciosamente
