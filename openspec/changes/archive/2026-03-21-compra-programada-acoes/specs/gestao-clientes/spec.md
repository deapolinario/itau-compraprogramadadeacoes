## ADDED Requirements

### Requirement: Adesão de cliente
O sistema SHALL permitir que um novo cliente adira ao produto fornecendo Nome, CPF, Email e Valor Mensal de Aporte. Ao aderir, o sistema MUST criar automaticamente uma Conta Gráfica Filhote e uma Custódia Filhote vinculadas ao cliente, com status Ativo = true e data de adesão registrada.

#### Scenario: Adesão bem-sucedida
- **WHEN** uma requisição POST /clientes é recebida com Nome, CPF válido, Email válido e ValorMensal >= 100
- **THEN** o sistema cria o cliente com Ativo = true, cria ContaGrafica e Custodia filhotes, e retorna 201 com o ID do cliente e número da conta

#### Scenario: CPF duplicado
- **WHEN** uma requisição POST /clientes é recebida com um CPF já cadastrado no sistema
- **THEN** o sistema retorna 409 Conflict com mensagem de erro indicando CPF já existente

#### Scenario: Valor mensal abaixo do mínimo
- **WHEN** uma requisição POST /clientes é recebida com ValorMensal < 100
- **THEN** o sistema retorna 400 Bad Request com mensagem de validação

#### Scenario: CPF com formato inválido
- **WHEN** uma requisição POST /clientes é recebida com CPF fora do formato esperado (11 dígitos numéricos)
- **THEN** o sistema retorna 400 Bad Request com mensagem de validação

---

### Requirement: Saída do produto
O sistema SHALL permitir que um cliente ativo solicite saída do produto a qualquer momento. O status do cliente MUST ser alterado para Ativo = false. A posição na custódia filhote MUST ser mantida intacta. O cliente NÃO SHALL participar de compras programadas futuras.

#### Scenario: Saída bem-sucedida
- **WHEN** uma requisição DELETE /clientes/{id} é recebida para um cliente com Ativo = true
- **THEN** o sistema altera Ativo = false e retorna 200 OK

#### Scenario: Saída de cliente já inativo
- **WHEN** uma requisição DELETE /clientes/{id} é recebida para um cliente com Ativo = false
- **THEN** o sistema retorna 409 Conflict

#### Scenario: Cliente inativo não participa do motor
- **WHEN** o motor de compra é executado
- **THEN** clientes com Ativo = false são excluídos do agrupamento

---

### Requirement: Alteração de valor mensal
O sistema SHALL permitir que um cliente ativo altere seu Valor Mensal de Aporte a qualquer momento. O novo valor MUST ser aplicado na próxima data de compra programada. O valor anterior MUST ser mantido em histórico.

#### Scenario: Alteração bem-sucedida
- **WHEN** uma requisição PATCH /clientes/{id}/valor-mensal é recebida com novo valor >= 100
- **THEN** o sistema atualiza ValorMensal do cliente e retorna 200 OK

#### Scenario: Alteração entre datas de compra
- **WHEN** cliente altera valor no dia 7 (entre dia 5 e dia 15)
- **THEN** a parcela do dia 5 já executada usa o valor anterior; a parcela do dia 15 usa o novo valor

#### Scenario: Novo valor abaixo do mínimo
- **WHEN** uma requisição PATCH /clientes/{id}/valor-mensal é recebida com valor < 100
- **THEN** o sistema retorna 400 Bad Request

---

### Requirement: Consulta de cliente
O sistema SHALL permitir consultar os dados de um cliente, incluindo clientes inativos.

#### Scenario: Consulta de cliente ativo
- **WHEN** uma requisição GET /clientes/{id} é recebida para um cliente existente
- **THEN** o sistema retorna 200 com dados do cliente (Nome, CPF mascarado, Email, ValorMensal, Ativo, DataAdesao)

#### Scenario: Cliente não encontrado
- **WHEN** uma requisição GET /clientes/{id} é recebida para um ID inexistente
- **THEN** o sistema retorna 404 Not Found
