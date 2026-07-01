# 📄 Guia de Paginação — API Controle Financeiro

**Para:** Desenvolvedor Front-End  
**Base URL:** `http://localhost:8080`  
**Atualização:** Julho/2026

> Este documento descreve **todas as implementações de paginação** adicionadas à API.  
> Para o guia geral de integração, consulte o arquivo `GUIA_INTEGRACAO_FRONT-END.md`.

---

## 📑 Índice

1. [Como funciona a paginação](#1-como-funciona-a-paginação)
2. [Módulo: Usuários — paginado](#2-módulo-usuários--paginado)
3. [Módulo: Configurações — paginado](#3-módulo-configurações--paginado)
4. [Módulo: Categorias — paginado](#4-módulo-categorias--paginado)
5. [Módulo: Instituições — paginado](#5-módulo-instituições--paginado)
6. [Módulo: Registros — navegação por calendário e paginação](#6-módulo-registros--navegação-por-calendário-e-paginação)
7. [Estrutura padrão da resposta paginada](#7-estrutura-padrão-da-resposta-paginada)
8. [Exemplos de código JavaScript](#8-exemplos-de-código-javascript)

---

## 1. Como funciona a paginação

Todos os endpoints paginados aceitam o parâmetro `pagina` na URL (query param).

| Parâmetro | Tipo    | Padrão | Descrição                                    |
|-----------|---------|--------|----------------------------------------------|
| `pagina`  | Inteiro | `0`    | Número da página desejada. **Começa em 0.**  |

**Exemplo:**
```
GET /usuarios?pagina=0   → primeira página
GET /usuarios?pagina=1   → segunda página
GET /usuarios?pagina=2   → terceira página
```

### ⚠️ Pontos importantes

- A paginação **começa em 0**, não em 1.
- Se a página solicitada estiver além do total de dados, a API retorna **`204 No Content`**.
- A resposta inclui metadados (total de páginas, total de itens etc.) — veja a [seção 7](#7-estrutura-padrão-da-resposta-paginada).

---

## 2. Módulo: Usuários — paginado

**Tamanho da página: 10 itens**

### `GET /usuarios?pagina={n}`

Retorna todos os usuários cadastrados, paginados em grupos de 10.

**Parâmetros:**

| Parâmetro | Obrigatório | Descrição         |
|-----------|-------------|-------------------|
| `pagina`  | Não         | Padrão: `0`       |

**Resposta `200`:**
```json
{
  "content": [
    {
      "id": "21eb5d2f-3fd8-439e-b647-5cc1f753ae58",
      "nome": "Celina",
      "sobrenome": "Benedito",
      "email": "celina@email.com",
      "dataNascimento": "2000-05-10"
    }
  ],
  "totalElements": 47,
  "totalPages": 5,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "empty": false
}
```

**Exemplo de chamada:**
```js
async function buscarUsuarios(pagina = 0) {
  const res = await fetch(`http://localhost:8080/usuarios?pagina=${pagina}`);
  if (res.status === 204) return { content: [], totalPages: 0 };
  return res.json();
}
```

---

## 3. Módulo: Configurações — paginado

**Tamanho da página: 10 itens**

### `GET /configuracoes?pagina={n}`

Retorna todas as configurações cadastradas no sistema, paginadas em grupos de 10.

> **Uso típico:** Endpoint administrativo. Para buscar a configuração de **um usuário específico**, use `GET /configuracoes/usuarios/{user_id}` (não paginado).

**Parâmetros:**

| Parâmetro | Obrigatório | Descrição   |
|-----------|-------------|-------------|
| `pagina`  | Não         | Padrão: `0` |

**Resposta `200`:**
```json
{
  "content": [
    {
      "id": "c1d2e3f4-...",
      "inicioMesFiscal": 1,
      "limiteDesejadoMensal": 3000.00,
      "ultimaAtualizacao": "2026-06-01"
    }
  ],
  "totalElements": 28,
  "totalPages": 3,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "empty": false
}
```

**Exemplo de chamada:**
```js
async function buscarConfiguracoes(pagina = 0) {
  const res = await fetch(`http://localhost:8080/configuracoes?pagina=${pagina}`);
  if (res.status === 204) return { content: [], totalPages: 0 };
  return res.json();
}
```

---

## 4. Módulo: Categorias — paginado

**Tamanho da página: 5 itens**

### `GET /categorias?pagina={n}`

Retorna todas as categorias do sistema (globais), paginadas em grupos de 5.

**Resposta `200`:**
```json
{
  "content": [
    { "id": 1, "titulo": "Alimentação" },
    { "id": 2, "titulo": "Transporte" },
    { "id": 3, "titulo": "Lazer" },
    { "id": 4, "titulo": "Saúde" },
    { "id": 5, "titulo": "Roupas" }
  ],
  "totalElements": 23,
  "totalPages": 5,
  "number": 0,
  "size": 5,
  "first": true,
  "last": false,
  "empty": false
}
```

---

### `GET /categorias/usuario/{user_id}?pagina={n}`

Retorna as categorias associadas a um usuário específico, paginadas em grupos de 5.

**Parâmetros:**

| Parâmetro | Local      | Obrigatório | Descrição                 |
|-----------|------------|-------------|---------------------------|
| `user_id` | Path       | Sim         | UUID do usuário           |
| `pagina`  | Query      | Não         | Padrão: `0`               |

**Resposta `200`:**
```json
{
  "content": [
    {
      "id": 12,
      "isAtivo": true,
      "ultimaAtualizacao": "2026-05-15T10:30:00",
      "usuario": { "id": "21eb5d2f-...", "nome": "Celina" },
      "categoria": { "id": 1, "titulo": "Alimentação" }
    }
  ],
  "totalElements": 8,
  "totalPages": 2,
  "number": 0,
  "size": 5,
  "first": true,
  "last": true,
  "empty": false
}
```

**Exemplo de chamada:**
```js
async function buscarCategoriasPorUsuario(userId, pagina = 0) {
  const res = await fetch(
    `http://localhost:8080/categorias/usuario/${userId}?pagina=${pagina}`
  );
  if (res.status === 204) return { content: [], totalPages: 0 };
  return res.json();
}
```

---

## 5. Módulo: Instituições — paginado

**Tamanho da página: 5 itens**

### `GET /instituicoes?pagina={n}`

Retorna todas as instituições financeiras do sistema, paginadas em grupos de 5.

**Resposta `200`:**
```json
{
  "content": [
    { "id": 1, "nome": "Nubank" },
    { "id": 2, "nome": "Inter" },
    { "id": 3, "nome": "Bradesco" },
    { "id": 4, "nome": "Itaú" },
    { "id": 5, "nome": "Caixa" }
  ],
  "totalElements": 12,
  "totalPages": 3,
  "number": 0,
  "size": 5,
  "first": true,
  "last": false,
  "empty": false
}
```

---

### `GET /instituicoes/usuarios/{user_id}?pagina={n}`

Retorna as instituições vinculadas a um usuário específico, paginadas em grupos de 5.

**Parâmetros:**

| Parâmetro | Local  | Obrigatório | Descrição       |
|-----------|--------|-------------|-----------------|
| `user_id` | Path   | Sim         | UUID do usuário |
| `pagina`  | Query  | Não         | Padrão: `0`     |

**Resposta `200`:**
```json
{
  "content": [
    {
      "id": 5,
      "isAtivo": true,
      "ultimaAtualizacao": "2026-06-10T08:00:00",
      "usuario": { "id": "21eb5d2f-...", "nome": "Celina", ... },
      "intituicao": { "id": 1, "nome": "Nubank" }
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "number": 0,
  "size": 5,
  "first": true,
  "last": true,
  "empty": false
}
```

**Exemplo de chamada:**
```js
async function buscarInstituicoesPorUsuario(userId, pagina = 0) {
  const res = await fetch(
    `http://localhost:8080/instituicoes/usuarios/${userId}?pagina=${pagina}`
  );
  if (res.status === 204) return { content: [], totalPages: 0 };
  return res.json();
}
```

---

## 6. Módulo: Registros — navegação por calendário e paginação

Este módulo foi redesenhado com uma **navegação em 3 passos** que guia o usuário do geral para o específico, evitando carregar dados desnecessários e sendo eficiente em máquinas com pouca memória.

**Tamanho da página (Passo 3): 20 itens**

---

### Fluxo visual

```
[Usuário abre a tela de registros]
         │
         ▼
 PASSO 1 — Escolher o ANO
 GET /registros/anos/usuarios/{user_id}
 → Retorna: [2026, 2025, 2024]
         │
         ▼
 PASSO 2 — Escolher o MÊS (do ano selecionado)
 GET /registros/meses/usuarios/{user_id}?ano=2026
 → Retorna: [1, 3, 6, 12]  ← apenas meses com dados
         │
         ▼
 PASSO 3 — Ver os registros do mês (paginados)
 GET /registros/mes/usuarios/{user_id}?ano=2026&mes=6&pagina=0
 → Retorna: Page<Registro> com 20 itens por página
```

---

### PASSO 1 — `GET /registros/anos/usuarios/{user_id}`

Retorna uma lista com os **anos distintos** em que o usuário possui registros, do mais recente para o mais antigo.

**Parâmetros:**

| Parâmetro | Local | Obrigatório | Descrição       |
|-----------|-------|-------------|-----------------|
| `user_id` | Path  | Sim         | UUID do usuário |

**Resposta `200`:**
```json
[2026, 2025, 2024]
```

**Resposta `204`:** O usuário não tem nenhum registro cadastrado.

**Exemplo de chamada:**
```js
async function buscarAnos(userId) {
  const res = await fetch(
    `http://localhost:8080/registros/anos/usuarios/${userId}`
  );
  if (res.status === 204) return [];
  return res.json(); // → [2026, 2025, 2024]
}
```

---

### PASSO 2 — `GET /registros/meses/usuarios/{user_id}?ano={ano}`

Retorna uma lista com os **meses distintos** (números de 1 a 12) que possuem registros no ano escolhido, em ordem crescente.

> Os meses sem registros **não aparecem** na lista, então o front-end já sabe quais meses habilitar/desabilitar no seletor.

**Parâmetros:**

| Parâmetro | Local | Obrigatório | Descrição               |
|-----------|-------|-------------|-------------------------|
| `user_id` | Path  | Sim         | UUID do usuário         |
| `ano`     | Query | Sim         | Ano escolhido no Passo 1 |

**Resposta `200`:**
```json
[1, 3, 6, 7, 12]
```
> Neste exemplo, o usuário tem registros em Janeiro, Março, Junho, Julho e Dezembro.

**Resposta `204`:** Nenhum registro para o ano informado.

**Exemplo de chamada:**
```js
async function buscarMeses(userId, ano) {
  const res = await fetch(
    `http://localhost:8080/registros/meses/usuarios/${userId}?ano=${ano}`
  );
  if (res.status === 204) return [];
  return res.json(); // → [1, 3, 6, 7, 12]
}

// Mapear número do mês para nome (opcional, feito no front):
const NOMES_MESES = [
  '', 'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];
// meses.map(m => NOMES_MESES[m]) → ["Janeiro", "Março", ...]
```

---

### PASSO 3 — `GET /registros/mes/usuarios/{user_id}?ano={ano}&mes={mes}&pagina={n}&tamanho={t}`

Retorna os registros do mês selecionado de forma **paginada** com **tamanho de página configurável**.

**Ordenação dos registros (feita automaticamente pela API):**

| Prioridade | Critério                          | Exemplo                         |
|------------|-----------------------------------|---------------------------------|
| 1º         | Dia (crescente)                   | Dia 01 antes do Dia 15          |
| 2º         | Tipo do evento                    | Recebimento → Gasto → Transferência → Poupança → Empréstimo |
| 3º         | Título (alfabético A → Z)         | "Almoço" antes de "Supermercado" |

**Exemplo visual da ordenação:**
```
Dia 01 | Recebimento  | Salário           ← Recebimento vem antes de Gasto
Dia 01 | Recebimento  | Vale-refeição     ← mesmo dia/tipo, ordem alfabética
Dia 01 | Gasto        | Almoço            ← Gasto depois de Recebimento
Dia 01 | Gasto        | Farmácia
Dia 03 | Recebimento  | Freelance         ← próximo dia
Dia 03 | Gasto        | Uber
```

**Parâmetros:**

| Parâmetro | Local | Obrigatório | Padrão | Descrição                                                  |
|-----------|-------|-------------|--------|------------------------------------------------------------|
| `user_id` | Path  | Sim         | —      | UUID do usuário                                            |
| `ano`     | Query | Sim         | —      | Ano (ex: `2026`)                                           |
| `mes`     | Query | Sim         | —      | Mês em número (ex: `6` para Junho)                         |
| `pagina`  | Query | Não         | `0`    | Número da página (começa em 0)                             |
| `tamanho` | Query | Não         | `20`   | Itens por página. **Mínimo: 5 · Máximo: 100**             |

> ⚠️ Se `tamanho` for enviado fora do intervalo permitido, a API ajusta automaticamente:  
> — Valores abaixo de `5` viram `5`  
> — Valores acima de `100` viram `100`

**Exemplos de URL:**
```
# Padrão (20 itens/pág)
GET /registros/mes/usuarios/{id}?ano=2026&mes=6&pagina=0

# 10 itens por página — ideal para telas menores
GET /registros/mes/usuarios/{id}?ano=2026&mes=6&pagina=0&tamanho=10

# 50 itens por página — ideal para tabelas densas / exportação visual
GET /registros/mes/usuarios/{id}?ano=2026&mes=6&pagina=0&tamanho=50
```

**Resposta `200`:**
```json
{
  "content": [
    {
      "gastoDetalhe": {
        "id": 42,
        "tituloGasto": "Salário",
        "categoria": [
          { "id": 5, "titulo": "Renda" }
        ]
      },
      "eventoFinanceiro": {
        "id": "a1b2c3d4-...",
        "tipo": "Recebimento",
        "valor": 5000.00,
        "descricao": "Salário mensal",
        "dataEvento": "2026-06-01"
      },
      "eventoInstituicao": [
        {
          "id": 7,
          "tipoMovimento": "Pix",
          "valor": 5000.00,
          "parcelas": 1,
          "instituicao": { "id": 1, "nome": "Nubank" }
        }
      ],
      "dataRegistro": "2026-06-01T09:00:00"
    }
  ],
  "totalElements": 34,
  "totalPages": 4,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "empty": false
}
```
> No exemplo acima foi usado `tamanho=10`, por isso `size=10` e `totalPages=4` (34 itens ÷ 10).

**Resposta `204`:** Nenhum registro para o mês/ano informado.

**Exemplo de chamada:**
```js
async function buscarRegistrosPorMes(userId, ano, mes, pagina = 0, tamanho = 20) {
  const res = await fetch(
    `http://localhost:8080/registros/mes/usuarios/${userId}` +
    `?ano=${ano}&mes=${mes}&pagina=${pagina}&tamanho=${tamanho}`
  );
  if (res.status === 204) return { content: [], totalPages: 0 };
  return res.json();
}

// Uso com tamanho padrão (20)
buscarRegistrosPorMes(userId, 2026, 6);

// Uso com 10 itens por página
buscarRegistrosPorMes(userId, 2026, 6, 0, 10);

// Segunda página com 10 itens
buscarRegistrosPorMes(userId, 2026, 6, 1, 10);
```
```

---

## 7. Estrutura padrão da resposta paginada

Todos os endpoints paginados retornam o mesmo formato:

```json
{
  "content": [ ...itens da página atual... ],

  "totalElements": 47,   // total de itens no banco para o filtro aplicado
  "totalPages": 5,       // total de páginas disponíveis
  "number": 0,           // página atual (começa em 0)
  "size": 10,            // itens por página (fixo por endpoint)

  "first": true,         // true se for a primeira página
  "last": false,         // true se for a última página
  "empty": false,        // true se content estiver vazio

  "numberOfElements": 10 // quantos itens vieram nesta página
}
```

### Como usar os metadados para criar paginação no front-end

```js
const data = await buscarUsuarios(0);

// Saber se há mais páginas
const temProxima = !data.last;
const temAnterior = !data.first;

// Total de páginas para montar botões numerados
for (let i = 0; i < data.totalPages; i++) {
  // criar botão para página i
}

// Página atual
const paginaAtual = data.number; // 0, 1, 2, ...

// Texto informativo
console.log(`Mostrando ${data.numberOfElements} de ${data.totalElements} itens`);
```

---

## 8. Exemplos de código JavaScript

### Componente de paginação genérico

```js
/**
 * Busca qualquer endpoint paginado da API.
 * @param {string} url - URL base do endpoint (sem o parâmetro pagina)
 * @param {number} pagina - Número da página (começa em 0)
 * @returns {Object} { content, totalPages, totalElements, number, first, last }
 */
async function buscarPaginado(url, pagina = 0) {
  const separador = url.includes('?') ? '&' : '?';
  const res = await fetch(`${url}${separador}pagina=${pagina}`);

  if (res.status === 204) {
    return { content: [], totalPages: 0, totalElements: 0, number: 0, first: true, last: true };
  }

  if (!res.ok) {
    throw new Error(`Erro ${res.status}: ${res.statusText}`);
  }

  return res.json();
}
```

---

### Fluxo completo dos Registros (3 passos)

```js
const BASE = 'http://localhost:8080';
const USER_ID = '21eb5d2f-3fd8-439e-b647-5cc1f753ae58';

// PASSO 1 — buscar anos disponíveis
async function iniciarNavegacao() {
  const res = await fetch(`${BASE}/registros/anos/usuarios/${USER_ID}`);
  if (res.status === 204) {
    console.log('Nenhum registro encontrado.');
    return;
  }
  const anos = await res.json();
  // anos → [2026, 2025, 2024]
  renderizarSeletorDeAno(anos);
}

// PASSO 2 — ao selecionar um ano, buscar meses disponíveis
async function aoSelecionarAno(ano) {
  const res = await fetch(`${BASE}/registros/meses/usuarios/${USER_ID}?ano=${ano}`);
  if (res.status === 204) return;

  const meses = await res.json();
  // meses → [1, 3, 6, 12]
  renderizarSeletorDeMes(meses);
}

// PASSO 3 — ao selecionar um mês, buscar registros (paginados)
async function aoSelecionarMes(ano, mes, pagina = 0, tamanho = 20) {
  const res = await fetch(
    `${BASE}/registros/mes/usuarios/${USER_ID}` +
    `?ano=${ano}&mes=${mes}&pagina=${pagina}&tamanho=${tamanho}`
  );

  if (res.status === 204) {
    renderizarListaVazia();
    return;
  }

  const dados = await res.json();
  // dados.content → array de registros
  // dados.totalPages → total de páginas
  // dados.number → página atual
  // dados.size → tamanho da página usado
  renderizarRegistros(dados.content);
  renderizarPaginacao(dados);
}

// Exemplo de uso encadeado
iniciarNavegacao();
// → usuário clica em "2026"
aoSelecionarAno(2026);
// → usuário clica em "Junho"
aoSelecionarMes(2026, 6, 0);           // padrão: 20 itens
aoSelecionarMes(2026, 6, 0, 10);       // 10 itens por página
// → usuário clica em "Próxima página"
aoSelecionarMes(2026, 6, 1, 10);       // página 2, ainda 10 itens
```

---

### Tabela de resumo — todos os endpoints paginados

| Endpoint                                            | Tam. da página        | Parâmetro obrigatório além de `pagina`                        |
|-----------------------------------------------------|:---------------------:|---------------------------------------------------------------|
| `GET /usuarios`                                     | 10 (fixo)             | —                                                             |
| `GET /configuracoes`                                | 10 (fixo)             | —                                                             |
| `GET /categorias`                                   | 5 (fixo)              | —                                                             |
| `GET /categorias/usuario/{user_id}`                 | 5 (fixo)              | `user_id` (path)                                              |
| `GET /instituicoes`                                 | 5 (fixo)              | —                                                             |
| `GET /instituicoes/usuarios/{user_id}`              | 5 (fixo)              | `user_id` (path)                                              |
| `GET /registros/anos/usuarios/{user_id}`            | — (não paginado)      | `user_id` (path)                                              |
| `GET /registros/meses/usuarios/{user_id}`           | — (não paginado)      | `user_id` (path), `ano`                                       |
| `GET /registros/mes/usuarios/{user_id}`             | **Configurável** `tamanho` (padrão `20`, mín `5`, máx `100`) | `user_id` (path), `ano`, `mes` |

---

> **Dica:** Todos os endpoints estão disponíveis e documentados de forma interativa no **Swagger UI**:  
> `http://localhost:8080/swagger-ui/index.html`




