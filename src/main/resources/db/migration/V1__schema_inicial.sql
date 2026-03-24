-- =============================================
-- Schema Inicial - Compra Programada de Ações
-- =============================================

CREATE TABLE clientes (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(200)   NOT NULL,
    cpf         VARCHAR(11)    NOT NULL UNIQUE,
    email       VARCHAR(200)   NOT NULL,
    valor_mensal DECIMAL(18,2) NOT NULL,
    ativo       BOOLEAN        NOT NULL DEFAULT TRUE,
    data_adesao TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE contas_graficas (
    id           BIGSERIAL PRIMARY KEY,
    cliente_id   BIGINT        REFERENCES clientes(id),
    numero_conta VARCHAR(20)   NOT NULL UNIQUE,
    tipo         VARCHAR(10)   NOT NULL CHECK (tipo IN ('MASTER', 'FILHOTE'))
);

-- A conta master não tem cliente_id (é da corretora)
-- Uma conta FILHOTE tem cliente_id

CREATE TABLE custodias (
    id           BIGSERIAL PRIMARY KEY,
    conta_id     BIGINT          NOT NULL REFERENCES contas_graficas(id),
    ticker       VARCHAR(12)     NOT NULL,
    quantidade   BIGINT          NOT NULL DEFAULT 0,
    preco_medio  DECIMAL(18,6)   NOT NULL DEFAULT 0,
    UNIQUE (conta_id, ticker)
);

CREATE TABLE cestas_recomendacao (
    id               BIGSERIAL PRIMARY KEY,
    data_criacao     TIMESTAMP NOT NULL DEFAULT NOW(),
    data_desativacao TIMESTAMP,
    ativo            BOOLEAN   NOT NULL DEFAULT TRUE
);

CREATE TABLE itens_cesta (
    id        BIGSERIAL PRIMARY KEY,
    cesta_id  BIGINT         NOT NULL REFERENCES cestas_recomendacao(id),
    ticker    VARCHAR(12)    NOT NULL,
    percentual DECIMAL(5,2)  NOT NULL,
    UNIQUE (cesta_id, ticker)
);

CREATE TABLE historico_operacoes (
    id           BIGSERIAL PRIMARY KEY,
    cliente_id   BIGINT          REFERENCES clientes(id),
    conta_id     BIGINT          NOT NULL REFERENCES contas_graficas(id),
    ticker       VARCHAR(12)     NOT NULL,
    tipo         VARCHAR(10)     NOT NULL CHECK (tipo IN ('COMPRA', 'VENDA')),
    quantidade   BIGINT          NOT NULL,
    preco_unitario DECIMAL(18,2) NOT NULL,
    valor_total  DECIMAL(18,2)   NOT NULL,
    data_operacao TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE execucoes_motor (
    id              BIGSERIAL PRIMARY KEY,
    data_referencia DATE        NOT NULL UNIQUE,
    status          VARCHAR(15) NOT NULL CHECK (status IN ('PENDENTE','EM_EXECUCAO','CONCLUIDO','ERRO')),
    iniciado_em     TIMESTAMP,
    concluido_em    TIMESTAMP,
    mensagem_erro   TEXT
);

CREATE TABLE eventos_kafka (
    id         BIGSERIAL PRIMARY KEY,
    tipo       VARCHAR(30)  NOT NULL,
    payload    TEXT         NOT NULL,
    status     VARCHAR(10)  NOT NULL CHECK (status IN ('PENDENTE', 'ENVIADO', 'ERRO')),
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW(),
    enviado_em TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_clientes_ativo ON clientes(ativo);
CREATE INDEX idx_clientes_cpf ON clientes(cpf);
CREATE INDEX idx_custodias_conta ON custodias(conta_id);
CREATE INDEX idx_custodias_ticker ON custodias(ticker);
CREATE INDEX idx_historico_cliente ON historico_operacoes(cliente_id);
CREATE INDEX idx_historico_data ON historico_operacoes(data_operacao);
CREATE INDEX idx_cestas_ativo ON cestas_recomendacao(ativo);
CREATE INDEX idx_execucoes_data ON execucoes_motor(data_referencia);
CREATE INDEX idx_eventos_status ON eventos_kafka(status);

-- Conta master da corretora (sem cliente_id)
INSERT INTO contas_graficas (numero_conta, tipo) VALUES ('MASTER-001', 'MASTER');
