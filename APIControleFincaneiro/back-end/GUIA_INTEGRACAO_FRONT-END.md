# 📘 Guia de Integração — API Controle Financeiro
**Para:** Isaak (Desenvolvedor Front-End)  
**Linguagem:** JavaScript / Fetch API  
**Base URL:** `http://localhost:8080`  
**Formato:** JSON em todas as requisições e respostas (exceto upload de arquivos e imagens)

> **Dica:** A API também possui documentação interativa via **Swagger UI** em:  
> `http://localhost:8080/swagger-ui/index.html`  
> Lá você pode testar todos os endpoints direto no navegador.

---

## 📑 Índice

1. [Convenções Gerais](#1-convenções-gerais)
2. [Módulo: Usuários (`/usuarios`)](#2-módulo-usuários)
3. [Módulo: Registros (`/registros`)](#3-módulo-registros)
4. [Módulo: Configurações (`/configuracoes`)](#4-módulo-configurações)
5. [Módulo: Dashboard (`/dashboard`)](#5-módulo-dashboard)
6. [Enums de Referência](#6-enums-de-referência)
7. [Exemplos de Chamadas Completas em JS](#7-exemplos-de-chamadas-completas-em-js)

---

## 1. Convenções Gerais

### Headers obrigatórios para envio de JSON
```js
headers: {
  "Content-Type": "application/json"
}
```

### Tipos de dados importantes

| Tipo Java       | Como chega/sai no JSON       | Exemplo                                |
|-----------------|-------------------------------|----------------------------------------|
| `UUID`          | String                        | `"21eb5d2f-3fd8-439e-b647-5cc1f753ae58"` |
| `LocalDate`     | String `YYYY-MM-DD`           | `"2025-06-15"`                         |
| `LocalDateTime` | String ISO 8601               | `"2025-06-15T14:30:00"`                |
| `BigDecimal`    | Número decimal                | `1250.75`                              |
| `Double`        | Número decimal                | `99.9`                                 |
| `Enum`          | String com o nome do enum     | `"Gasto"`, `"MENSAL"`                  |

### Códigos de resposta usados

| Código | Significado                                        |
|--------|----------------------------------------------------|
| `200`  | Sucesso com dados                                  |
| `201`  | Criado com sucesso                                 |
| `204`  | Sucesso sem dados (lista vazia ou deleção)         |
| `400`  | Requisição inválida (campos errados ou arquivo inválido) |
| `404`  | Recurso não encontrado                             |
| `409`  | Conflito (ex.: usuário já tem configuração)        |

---

## 2. Módulo: Usuários

**Base:** `/usuarios`

---

### `GET /usuarios`
Lista todos os usuários cadastrados.

**Resposta 200:**
```json
[
  {
    "id": "21eb5d2f-3fd8-439e-b647-5cc1f753ae58",
    "nome": "João",
    "sobrenome": "Da Silva",
    "email": "joao@gmail.com",
    "dataNascimento": "1998-07-13",
    "sexo": "Masculino",
    "imagem": "caminho/para/imagem.jpg",
    "ativo": true
  }
]
```

---

### `GET /usuarios/{id}`
Busca um usuário pelo ID.

**Path param:** `id` (UUID)

---

### `GET /usuarios/saldo/{user_id}`
Retorna o saldo atual do usuário (soma de toda a história financeira até hoje).

**Resposta 200:** número decimal
```json
1250.75
```

---

### `GET /usuarios/calculo-xp/{user_id}`
Retorna o XP calculado do usuário (gamificação).

**Resposta 200:** número decimal
```json
320.0
```

---

### `POST /usuarios`
Cria um novo usuário. Ao criar, uma **configuração padrão é criada automaticamente**.

**Body:**
```json
{
  "nome": "João",
  "sobrenome": "Da Silva Santos",
  "dataNascimento": "1998-07-13",
  "sexo": "Masculino",
  "email": "joaosilva@gmail.com",
  "senha": "Senha123!?"
}
```

> **Valores de `sexo`:** `"Masculino"` | `"Feminino"` | `"Outro"`

**Resposta 201:** objeto do usuário criado.

---

### `POST /usuarios/login`
Autentica o usuário pelo e-mail e senha.

**Body:**
```json
{
  "email": "joaosilva@gmail.com",
  "senha": "Senha123!?"
}
```

**Resposta 200:** objeto do usuário encontrado (incluindo o `id` para usar nas demais chamadas).

---

### `PUT /usuarios/{id}`
Edita dados do usuário (nome, sobrenome, email, etc.).

**Body:** mesmo formato do `POST /usuarios` (sem senha).

---

### `PUT /usuarios/editar-senha/{user_id}`
Altera a senha do usuário.

**Body:**
```json
{
  "antigaSenha": "SenhaAntiga1!",
  "novaSenha": "NovaSenha2!"
}
```

---

### `PUT /usuarios/ativar-usuario/{user_id}`
Reativa um usuário desativado.

---

### `PUT /usuarios/{id}/upload-imagem`
Faz upload da foto de perfil do usuário.

**Content-Type:** `multipart/form-data`  
**Campo do arquivo:** `file`

**Exemplo em JS:**
```js
const formData = new FormData();
formData.append("file", arquivoImagem); // input type="file"

fetch(`http://localhost:8080/usuarios/${userId}/upload-imagem`, {
  method: "PUT",
  body: formData
  // NÃO definir Content-Type manualmente — o browser define com boundary
});
```

---

### `DELETE /usuarios/{user_id}`
Remove o usuário do sistema.

**Resposta 204:** sem corpo.

---

## 3. Módulo: Registros

**Base:** `/registros`

---

### `GET /registros/{user_id}`
Retorna todos os eventos financeiros do usuário.

**Resposta 200 — estrutura de cada item:**
```json
{
  "eventoFinanceiro": {
    "id": "uuid-do-evento",
    "tipo": "Gasto",
    "valor": 150.00,
    "descricao": "Jantar com amigos",
    "dataEvento": "2025-06-10"
  },
  "eventoInstituicao": [
    {
      "id": 1,
      "instituicao": {
        "id": 2,
        "nome": "Nubank"
      },
      "tipoMovimento": "Credito",
      "valor": 150.00,
      "parcelas": 1
    }
  ],
  "gastoDetalhe": {
    "id": 1,
    "tituloGasto": "Jantar especial",
    "categoria": [
      { "id": 3, "titulo": "Alimentação" }
    ]
  },
  "dataRegistro": "2025-06-10T21:00:00"
}
```

---

### `GET /registros/saldo-poupanca/usuarios/{user_id}`
Retorna o saldo atual acumulado na poupança do usuário.

**Resposta 200:** número decimal.

---

### `GET /registros/filtro/usuarios/{user_id}`
Busca registros aplicando filtros via query params. Todos os filtros são **opcionais**.

**Query params disponíveis:**

| Param               | Tipo                    | Exemplo                          |
|---------------------|-------------------------|----------------------------------|
| `valor`             | number                  | `150.00`                         |
| `tipoMovimento`     | enum (lista)            | `Credito` ou `Debito`            |
| `tipo`              | enum (lista)            | `Gasto`, `Recebimento`...        |
| `dataEvento`        | date `YYYY-MM-DD`       | `2025-06-10`                     |
| `descricao`         | string                  | `jantar`                         |
| `titulo`            | string                  | `Champagne`                      |

**Exemplo:**
```
GET /registros/filtro/usuarios/{user_id}?tipo=Gasto&dataEvento=2025-06-10
```

---

### `GET /registros/download/{user_id}`
Gera e baixa um arquivo com todos os registros do usuário.

**Query param:** `tipo` → `json` | `sql` | `excel` | `pdf`

**Exemplo em JS:**
```js
// Dispara o download do arquivo no navegador
window.open(`http://localhost:8080/registros/download/${userId}?tipo=excel`);
```

---

### `POST /registros`
Cria um único registro financeiro.

**Body completo:**
```json
{
  "financeiro": {
    "usuario_id": "21eb5d2f-3fd8-439e-b647-5cc1f753ae58",
    "tipo": "Gasto",
    "valor": 150.00,
    "descricao": "Jantar com amigos",
    "dataEvento": "2025-06-10"
  },
  "instituicao": [
    {
      "instituicaoUsuario_id": 2,
      "tipoMovimento": "Credito",
      "valor": 150.00,
      "parcelas": 1
    }
  ],
  "detalhe": {
    "categoriaUsuario_id": [3],
    "tituloGasto": "Jantar especial"
  }
}
```

> **Valores de `tipo`:** `"Gasto"` | `"Recebimento"` | `"Transferencia"` | `"Emprestimo"` | `"Poupanca"`  
> **Valores de `tipoMovimento`:** `"Credito"` | `"Debito"`

**Resposta 201:** objeto do registro criado.

---

### `POST /registros/recorrente`
Cria um evento **recorrente** (repete automaticamente até uma data fim).

**Body — campos adicionais no `financeiro`:**
```json
{
  "financeiro": {
    "usuario_id": "uuid...",
    "tipo": "Gasto",
    "valor": 50.00,
    "dataEvento": "2025-06-01",
    "recorrente": true,
    "periodicidade": "MENSAL",
    "intervalo": 1,
    "dataFim": "2025-12-31"
  },
  "instituicao": [...],
  "detalhe": {...}
}
```

> **Valores de `periodicidade`:** `"DIARIO"` | `"SEMANAL"` | `"MENSAL"` | `"ANUAL"`  
> Para recorrência **semanal**, adicione o campo `diasDaSemana`:
> ```json
> "diasDaSemana": ["MONDAY", "WEDNESDAY", "FRIDAY"]
> ```

**Resposta 201:** lista de todos os registros criados.

---

### `POST /registros/lote`
Cria vários registros de uma só vez (mesmo dia ou dias diferentes).

**Body:** array de objetos com o mesmo formato do `POST /registros`.

```json
[
  { "financeiro": {...}, "instituicao": [...], "detalhe": {...} },
  { "financeiro": {...}, "instituicao": [...], "detalhe": {...} }
]
```

**Resposta 201:** lista dos registros criados.

---

### `PUT /registros/{evento_id}`
Edita um registro pelo ID do evento financeiro.

**Path param:** `evento_id` (UUID do `eventoFinanceiro.id`)  
**Body:** mesmo formato do `POST /registros`.

---

### `DELETE /registros/{evento_id}`
Remove um registro pelo ID do evento financeiro.

**Resposta 204:** sem corpo.

---

## 4. Módulo: Configurações

**Base:** `/configuracoes`

---

### `GET /configuracoes/usuarios/{user_id}`
Retorna as configurações do usuário (inclui limites por instituição e por categoria).

**Resposta 200:**
```json
{
  "id": "uuid-config",
  "inicioMesFiscal": 1,
  "limiteDesejadoMensal": 1500.00,
  "limitePorInstituicao": [
    { "instituicao": "Nubank", "limite": 500.00 }
  ],
  "limitePorCategoria": [
    { "categoria": "Alimentação", "limite": 300.00 }
  ],
  "usuario": { "id": "uuid-usuario", "nome": "João" }
}
```

---

### `POST /configuracoes`
Cria configuração manualmente para um usuário.  
> ⚠️ **Isso já é feito automaticamente ao criar o usuário.** Use apenas se necessário.

**Body:**
```json
{
  "fkUsuario": "21eb5d2f-3fd8-439e-b647-5cc1f753ae58",
  "inicioMesFiscal": 1,
  "limiteDesejadoMensal": 1500.00
}
```

---

### `POST /configuracoes/{id}/instituicoes`
Adiciona limites de gastos por instituição às configurações.

**Path param:** `id` (UUID da configuração)

**Body:**
```json
[
  {
    "institucaoUsuario_id": 2,
    "limiteDesejado": 500.00
  }
]
```

---

### `POST /configuracoes/{id}/categorias`
Adiciona limites de gastos por categoria às configurações.

**Path param:** `id` (UUID da configuração)

**Body:**
```json
[
  {
    "categoriaUsuario_id": 3,
    "limiteDesejado": 300.00
  }
]
```

---

### `PUT /configuracoes/edit/{id}`
Edita as configurações existentes.

**Body:**
```json
{
  "inicioMesFiscal": 5,
  "limiteDesejadoMensal": 2000.00
}
```

---

### `POST /configuracoes/upload-arquivo/usuarios/{user_id}`
Importa registros a partir de um arquivo exportado pela aplicação (JSON, SQL, Excel ou PDF).

**Content-Type:** `multipart/form-data`  
**Campo do arquivo:** `arquivo`

**Resposta 200:**
```json
{
  "totalImportados": 12,
  "registrosImportados": [ ... ],
  "erros": []
}
```

> Se houver erros parciais (alguns registros falharem), o campo `erros` trará as mensagens, mas os registros válidos serão salvos normalmente.

**Exemplo em JS:**
```js
const formData = new FormData();
formData.append("arquivo", arquivoExportado); // input type="file"

const resp = await fetch(
  `http://localhost:8080/configuracoes/upload-arquivo/usuarios/${userId}`,
  { method: "POST", body: formData }
);
const resultado = await resp.json();
console.log(`Importados: ${resultado.totalImportados}`);
console.log("Erros:", resultado.erros);
```

---

### `DELETE /configuracoes/{id}/dados/periodo-tempo`
Remove registros do usuário dentro de um intervalo de datas.

**Path param:** `id` (UUID da configuração)  
**Body:**
```json
{
  "dataInicio": "2025-01-01",
  "dataFim": "2025-03-31"
}
```

---

### `DELETE /configuracoes/usuarios/{user_id}/dados/deletar-tudo`
Remove **todos** os registros do usuário. ⚠️ **Irreversível.**

**Resposta 204:** sem corpo.

---

## 5. Módulo: Dashboard

**Base:** `/dashboard`

Este é o módulo mais novo. Ele fornece **4 KPIs** e **5 gráficos** financeiros + uma listagem de registros do período.

---

### ⚙️ Query Params Comuns a TODOS os endpoints do Dashboard

Todos os endpoints abaixo recebem **obrigatoriamente** os seguintes query params:

| Param       | Obrigatório?                              | Valores aceitos        | Exemplo          |
|-------------|-------------------------------------------|------------------------|------------------|
| `periodo`   | ✅ Sempre                                  | `MENSAL` `TRIMESTRAL` `SEMESTRAL` `ANUAL` | `periodo=MENSAL` |
| `ano`       | ✅ Sempre                                  | inteiro                | `ano=2025`       |
| `mes`       | ✅ Se `periodo=MENSAL`                     | `1` a `12`             | `mes=6`          |
| `trimestre` | ✅ Se `periodo=TRIMESTRAL`                 | `1` a `4`              | `trimestre=2`    |
| `semestre`  | ✅ Se `periodo=SEMESTRAL`                  | `1` ou `2`             | `semestre=1`     |

**Exemplos de URL:**
```
?periodo=MENSAL&ano=2025&mes=6
?periodo=TRIMESTRAL&ano=2025&trimestre=2
?periodo=SEMESTRAL&ano=2025&semestre=1
?periodo=ANUAL&ano=2025
```

---

### KPIs

#### `GET /dashboard/kpi/saldo-total/usuarios/{user_id}`
Saldo acumulado de **toda a história financeira** do usuário até o final do período (ou até hoje se o período ainda não acabou).

**Resposta 200:**
```json
{
  "saldo": 3450.75,
  "dataReferencia": "2025-06-24",
  "labelPeriodo": "Junho / 2025"
}
```

> `saldo` positivo = no azul | negativo = no vermelho.

---

#### `GET /dashboard/kpi/gasto-total/usuarios/{user_id}`
Total de gastos (tipo `Gasto` + `Transferencia`) no período, com variação % em relação ao período anterior.

**Resposta 200:**
```json
{
  "totalGastos": 1200.00,
  "totalGastosAnterior": 950.00,
  "variacaoPercentual": 26,
  "labelPeriodo": "Junho / 2025",
  "labelPeriodoAnterior": "Maio / 2025"
}
```

> `variacaoPercentual` positivo = gastou **mais** | negativo = gastou **menos**.

---

#### `GET /dashboard/kpi/maior-gasto/usuarios/{user_id}`
O maior gasto isolado (tipo `Gasto`) dentro do período.

**Resposta 200:**
```json
{
  "titulo": "Aluguel",
  "categoria": "Moradia",
  "valor": 800.00,
  "percentualDoTotal": 67,
  "data": "2025-06-05"
}
```

---

#### `GET /dashboard/kpi/categoria-impacto/usuarios/{user_id}`
A categoria que mais consumiu dinheiro no período, com variação vs período anterior.

**Resposta 200:**
```json
{
  "categoria": "Alimentação",
  "valorAtual": 420.00,
  "valorAnterior": 390.00,
  "variacaoPercentual": 8
}
```

---

### Gráficos

#### `GET /dashboard/grafico/evolucao-gastos/usuarios/{user_id}`
Série temporal de gastos — linha do tempo. A granularidade muda automaticamente:
- **MENSAL / TRIMESTRAL** → ponto por **dia**
- **SEMESTRAL** → ponto por **semana**
- **ANUAL** → ponto por **mês**

**Resposta 200:**
```json
{
  "labelPeriodo": "Junho / 2025",
  "granularidade": "DIARIO",
  "dados": [
    { "label": "01/06", "valor": 0.00 },
    { "label": "02/06", "valor": 45.50 },
    { "label": "03/06", "valor": 120.00 }
  ]
}
```

> Pronto para usar em um **line chart** (Chart.js, Recharts, etc.).  
> `label` = eixo X | `valor` = eixo Y.

---

#### `GET /dashboard/grafico/categorias/usuarios/{user_id}`
Gastos agrupados por categoria, ordenados do maior para o menor.

**Resposta 200:**
```json
{
  "labelPeriodo": "Junho / 2025",
  "categorias": [
    {
      "nome": "Alimentação",
      "valorTotal": 420.00,
      "percentualDoTotal": 35,
      "ocorrencias": 8
    },
    {
      "nome": "Transporte",
      "valorTotal": 200.00,
      "percentualDoTotal": 17,
      "ocorrencias": 5
    }
  ]
}
```

> Pronto para **bar chart** ou **pie chart**.

---

#### `GET /dashboard/grafico/comparacao-periodo/usuarios/{user_id}`
Compara os gastos do período atual com o período anterior, **ponto a ponto**.

**Resposta 200:**
```json
{
  "labelPeriodoAtual": "Junho / 2025",
  "labelPeriodoAnterior": "Maio / 2025",
  "dados": [
    { "label": "Dia 1", "valorAtual": 0.00,   "valorAnterior": 30.00 },
    { "label": "Dia 2", "valorAtual": 45.50,  "valorAnterior": 0.00  },
    { "label": "Dia 3", "valorAtual": 120.00, "valorAnterior": 80.00 }
  ]
}
```

> Pronto para **multiple line chart** com duas séries.  
> `label` = eixo X | `valorAtual` e `valorAnterior` = séries Y.

---

#### `GET /dashboard/grafico/gastos-dia-semana/usuarios/{user_id}`
Distribuição de gastos por dia da semana — útil para **heat map** ou **bar chart horizontal**.

**Resposta 200:**
```json
{
  "labelPeriodo": "Junho / 2025",
  "dias": [
    { "dia": "segunda-feira",  "valorTotal": 310.00, "normalizado": 1.0  },
    { "dia": "terça-feira",   "valorTotal": 150.00, "normalizado": 0.48 },
    { "dia": "quarta-feira",  "valorTotal": 0.00,   "normalizado": 0.0  },
    { "dia": "quinta-feira",  "valorTotal": 75.00,  "normalizado": 0.24 },
    { "dia": "sexta-feira",   "valorTotal": 200.00, "normalizado": 0.65 },
    { "dia": "sábado",        "valorTotal": 90.00,  "normalizado": 0.29 },
    { "dia": "domingo",       "valorTotal": 40.00,  "normalizado": 0.13 }
  ]
}
```

> `normalizado` vai de `0.0` (sem gastos) até `1.0` (dia com maior gasto).  
> Use `normalizado` para colorir células de heat map.

---

#### `GET /dashboard/grafico/fluxo-financeiro/usuarios/{user_id}`
Diagrama de fluxo tipo **Sankey** — mostra de onde vem e para onde vai o dinheiro.

**Estrutura:**  
`Entrada → Instituição → Categoria (ou Transferência / Poupança)`

**Resposta 200:**
```json
{
  "labelPeriodo": "Junho / 2025",
  "nos": [
    {
      "id": "nubank",
      "label": "Nubank",
      "tipo": "INSTITUICAO",
      "totalEntrada": 2000.00,
      "totalSaida": 1200.00
    },
    {
      "id": "alimentacao",
      "label": "Alimentação",
      "tipo": "CATEGORIA",
      "totalEntrada": 420.00,
      "totalSaida": 0.00
    },
    {
      "id": "saida-transferencia",
      "label": "Transferência",
      "tipo": "SAIDA",
      "totalEntrada": 300.00,
      "totalSaida": 0.00
    }
  ],
  "links": [
    { "de": "nubank",       "para": "alimentacao",        "valor": 420.00 },
    { "de": "nubank",       "para": "saida-transferencia","valor": 300.00 }
  ]
}
```

> Use a lib **d3-sankey**, **Google Charts Sankey**, ou equivalente.  
> `nos` = nodes | `links` = edges/flows.  
> **`de` e `para` nos links correspondem ao campo `id` dos nós.**

---

### `GET /dashboard/registros/usuarios/{user_id}`
Lista todos os registros financeiros dentro do período selecionado.

**Resposta 200:** lista de registros (mesmo formato do `GET /registros/{user_id}`).  
**Resposta 204:** nenhum registro no período.

---

## 6. Enums de Referência

### `Tipo` (tipo do evento financeiro)
| Valor           | Descrição                                    |
|-----------------|----------------------------------------------|
| `Gasto`         | Saída de dinheiro (compra, despesa)          |
| `Recebimento`   | Entrada de dinheiro (salário, venda)         |
| `Transferencia` | Movimentação entre contas (saída líquida)    |
| `Emprestimo`    | Dinheiro recebido de empréstimo (entrada)    |
| `Poupanca`      | Dinheiro destinado à poupança (saída)        |

> **Regra de saldo:** `Recebimento` e `Emprestimo` = **+valor** | `Gasto`, `Transferencia` e `Poupanca` = **−valor**

### `TipoMovimento` (forma de pagamento)
| Valor     | Descrição         |
|-----------|-------------------|
| `Credito` | Cartão de crédito |
| `Debito`  | Débito / dinheiro |

### `UsuarioSexo`
| Valor        |
|--------------|
| `Masculino`  |
| `Feminino`   |
| `Outro`      |

### `TipoPeriodo` (dashboard)
| Valor         | Requer também            |
|---------------|--------------------------|
| `MENSAL`      | `mes` (1–12)             |
| `TRIMESTRAL`  | `trimestre` (1–4)        |
| `SEMESTRAL`   | `semestre` (1 ou 2)      |
| `ANUAL`       | apenas `ano`             |

---

## 7. Exemplos de Chamadas Completas em JS

### Utilitário base
```js
const BASE_URL = "http://localhost:8080";

async function api(path, options = {}) {
  const resp = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });
  if (resp.status === 204) return null;
  if (!resp.ok) {
    const err = await resp.text();
    throw new Error(`Erro ${resp.status}: ${err}`);
  }
  return resp.json();
}
```

---

### Login
```js
async function login(email, senha) {
  const usuario = await api("/usuarios/login", {
    method: "POST",
    body: JSON.stringify({ email, senha }),
  });
  // Guarde o usuario.id para usar nas chamadas seguintes
  localStorage.setItem("userId", usuario.id);
  return usuario;
}
```

---

### Criar um registro de gasto
```js
async function criarGasto(userId) {
  const registro = await api("/registros", {
    method: "POST",
    body: JSON.stringify({
      financeiro: {
        usuario_id: userId,
        tipo: "Gasto",
        valor: 89.90,
        descricao: "Almoço de negócios",
        dataEvento: "2025-06-15"
      },
      instituicao: [
        {
          instituicaoUsuario_id: 2,  // ID da instituição cadastrada para este usuário
          tipoMovimento: "Credito",
          valor: 89.90,
          parcelas: 1
        }
      ],
      detalhe: {
        categoriaUsuario_id: [3],    // ID da categoria cadastrada para este usuário
        tituloGasto: "Almoço"
      }
    }),
  });
  return registro;
}
```

---

### Buscar KPIs do Dashboard (mês de junho/2025)
```js
async function buscarKpis(userId) {
  const params = "?periodo=MENSAL&ano=2025&mes=6";

  const [saldo, gastoTotal, maiorGasto, categoriaImpacto] = await Promise.all([
    api(`/dashboard/kpi/saldo-total/usuarios/${userId}${params}`),
    api(`/dashboard/kpi/gasto-total/usuarios/${userId}${params}`),
    api(`/dashboard/kpi/maior-gasto/usuarios/${userId}${params}`),
    api(`/dashboard/kpi/categoria-impacto/usuarios/${userId}${params}`),
  ]);

  return { saldo, gastoTotal, maiorGasto, categoriaImpacto };
}
```

---

### Buscar dados do gráfico de evolução (linha do tempo)
```js
async function buscarEvolucao(userId, periodo, ano, mes) {
  const params = new URLSearchParams({ periodo, ano, mes: mes ?? "" });
  const dados = await api(`/dashboard/grafico/evolucao-gastos/usuarios/${userId}?${params}`);

  // dados.dados é um array de { label, valor } pronto para Chart.js
  const labels = dados.dados.map(p => p.label);
  const valores = dados.dados.map(p => p.valor);

  return { labels, valores, granularidade: dados.granularidade };
}
```

---

### Upload de foto de perfil
```js
async function uploadFoto(userId, inputFile) {
  const formData = new FormData();
  formData.append("file", inputFile.files[0]);

  const resp = await fetch(`${BASE_URL}/usuarios/${userId}/upload-imagem`, {
    method: "PUT",
    body: formData,
  });
  return resp.json();
}
```

---

### Importar registros via arquivo
```js
async function importarArquivo(userId, inputFile) {
  const formData = new FormData();
  formData.append("arquivo", inputFile.files[0]);

  const resp = await fetch(
    `${BASE_URL}/configuracoes/upload-arquivo/usuarios/${userId}`,
    { method: "POST", body: formData }
  );

  const resultado = await resp.json();
  // resultado = { totalImportados, registrosImportados, erros }
  return resultado;
}
```

---

### Baixar registros como Excel
```js
function baixarRegistros(userId, formato = "excel") {
  // Cria link temporário e clica
  const link = document.createElement("a");
  link.href = `${BASE_URL}/registros/download/${userId}?tipo=${formato}`;
  link.click();
}
```

---

*Documento gerado em 24/06/2026 — API Controle Financeiro v1.0*

