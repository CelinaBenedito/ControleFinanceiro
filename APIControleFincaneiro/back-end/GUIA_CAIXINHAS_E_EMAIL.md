# 📦 Guia de Implementação — Caixinhas de Poupança & E-mails

> **Módulos implementados:** `CaixinhaController`, `CaixinhaService`, `EmailService`, `RelatorioEmailJob`

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Caixinhas de Poupança](#2-caixinhas-de-poupança)
   - [Modelo de dados](#21-modelo-de-dados)
   - [Tipos de rendimento](#22-tipos-de-rendimento)
   - [Endpoints disponíveis](#23-endpoints-disponíveis)
   - [Payload de criação](#24-payload-de-criação-caixinhacreatedto)
   - [Payload de resposta](#25-payload-de-resposta-caixinharesponsedto)
   - [Cálculos financeiros](#26-cálculos-financeiros)
   - [Meta compartilhada](#27-meta-compartilhada)
3. [Sistema de E-mails](#3-sistema-de-e-mails)
   - [Configuração SMTP](#31-configuração-smtp)
   - [Tipos de alerta](#32-tipos-de-alerta-tipoalertaemail)
   - [Métodos do EmailService](#33-métodos-do-emailservice)
   - [Jobs agendados](#34-jobs-agendados-relatorioemailjobjava)
   - [Integração com Configurações](#35-integração-com-configurações-do-usuário)
4. [Fluxo completo — exemplo de uso](#4-fluxo-completo--exemplo-de-uso)
5. [Como ativar o envio de e-mails](#5-como-ativar-o-envio-de-e-mails)

---

## 1. Visão Geral

Esta implementação adiciona dois módulos ao sistema de controle financeiro:

| Módulo | O que faz |
|---|---|
| **Caixinhas** | Gerencia metas de poupança com rendimento (CDI, SELIC, Poupança, Prefixado, Personalizado), projeções financeiras e metas compartilhadas entre instituições. |
| **E-mails** | Envia alertas automáticos e relatórios por e-mail para o usuário, acionados por eventos (ex.: meta atingida) ou por agendamento (cron jobs). |

Os dois módulos estão integrados: quando uma caixinha atinge o percentual de meta configurado pelo usuário, o `CaixinhaService` aciona automaticamente o `EmailService`.

---

## 2. Caixinhas de Poupança

### 2.1 Modelo de dados

```
Caixinha
├── id                    UUID (PK)
├── usuario               → Usuario
├── nome                  String (máx 100, obrigatório)
├── descricao             String (máx 500, opcional)
├── valorMeta             BigDecimal (positivo, opcional)
├── dataPrazo             LocalDate (opcional)
├── tipoRendimento        TipoRendimento (enum, obrigatório)
├── percentualRendimento  Double  – % do CDI/SELIC (ex.: 100.0)
├── taxaAnualPersonalizada Double – taxa fixa a.a. (ex.: 12.5)
├── taxaReferenciaAtual   Double  – CDI/SELIC atual em % a.a.
├── isCompartilhada       Boolean
├── isAtiva               Boolean
├── dataCriacao           LocalDate
└── dataEncerramento      LocalDate (preenchido ao encerrar)

CaixinhaInstituicao  (tabela de junção)
├── caixinha          → Caixinha
└── instituicaoUsuario → InstituicaoUsuario
```

### 2.2 Tipos de rendimento

| Enum | Descrição | Campos necessários |
|---|---|---|
| `CDI` | Percentual do CDI (ex.: 100% ou 110%) | `taxaReferenciaAtual` + `percentualRendimento` |
| `SELIC` | Percentual da SELIC | `taxaReferenciaAtual` + `percentualRendimento` |
| `POUPANCA` | 70% da SELIC (regra vigente acima de 8,5% a.a.) | `taxaReferenciaAtual` |
| `PREFIXADO` | Taxa anual fixa | `taxaAnualPersonalizada` |
| `PERSONALIZADO` | Taxa definida livremente pelo usuário | `taxaAnualPersonalizada` |

### 2.3 Endpoints disponíveis

| Método | Rota | Descrição | Status |
|---|---|---|---|
| `POST` | `/caixinhas` | Criar nova caixinha | `201` |
| `GET` | `/caixinhas/usuarios/{user_id}` | Listar todas (ativas + encerradas) | `200` / `204` |
| `GET` | `/caixinhas/ativas/usuarios/{user_id}` | Listar apenas ativas | `200` / `204` |
| `GET` | `/caixinhas/{caixinha_id}` | Buscar caixinha por ID | `200` |
| `GET` | `/caixinhas/resumo/usuarios/{user_id}` | Total acumulado em poupança | `200` |
| `PUT` | `/caixinhas/{caixinha_id}` | Editar caixinha | `200` |
| `PATCH` | `/caixinhas/{caixinha_id}/encerrar` | Encerrar caixinha | `200` |
| `POST` | `/caixinhas/{caixinha_id}/instituicoes/{inst_id}` | Adicionar instituição | `200` |
| `DELETE` | `/caixinhas/{caixinha_id}` | Deletar caixinha | `204` |

> ⚠️ **Deletar** remove a caixinha e seus vínculos com instituições. Os `EventoFinanceiro` (aportes) **não** são deletados.

### 2.4 Payload de criação (`CaixinhaCreateDTO`)

```json
{
  "usuarioId": "uuid-do-usuario",
  "nome": "Viagem para o Japão",
  "descricao": "Meta de viagem para 2026",
  "valorMeta": 15000.00,
  "dataPrazo": "2026-12-31",
  "tipoRendimento": "CDI",
  "percentualRendimento": 100.0,
  "taxaReferenciaAtual": 10.40,
  "taxaAnualPersonalizada": null,
  "instituicaoUsuarioIds": [1],
  "isCompartilhada": false
}
```

**Exemplo com meta compartilhada (2 instituições):**

```json
{
  "usuarioId": "uuid-do-usuario",
  "nome": "Reserva de Emergência",
  "tipoRendimento": "SELIC",
  "percentualRendimento": 100.0,
  "taxaReferenciaAtual": 10.75,
  "valorMeta": 30000.00,
  "dataPrazo": "2027-06-01",
  "instituicaoUsuarioIds": [1, 3],
  "isCompartilhada": true
}
```

### 2.5 Payload de resposta (`CaixinhaResponseDTO`)

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | Identificador único |
| `nome` | String | Nome da caixinha |
| `descricao` | String | Descrição opcional |
| `isAtiva` | Boolean | `true` = em andamento |
| `isCompartilhada` | Boolean | Meta compartilhada entre instituições |
| `dataCriacao` | LocalDate | Data de criação |
| `dataEncerramento` | LocalDate | Preenchido ao encerrar |
| `instituicoes` | List | Lista de instituições com `valorAportado` por cada uma |
| `valorMeta` | BigDecimal | Meta total configurada |
| `dataPrazo` | LocalDate | Prazo para atingir a meta |
| `tipoRendimento` | Enum | Tipo de rendimento |
| `taxaMensalEfetiva` | Double | Taxa mensal calculada (decimal, ex.: `0.00829`) |
| `valorAtual` | BigDecimal | **Soma de todos os aportes registrados** |
| `faltaParaMeta` | BigDecimal | `max(0, meta - valorAtual)` |
| `percentualAtingido` | Double | `(valorAtual / meta) * 100` |
| `mesesRestantes` | Integer | Meses entre hoje e o prazo |
| `aporteMensalSugerido` | BigDecimal | **Quanto depositar/mês para atingir a meta** (PMT) |
| `montanteProjetadoSemAportes` | BigDecimal | Projeção só com rendimento (sem novos aportes) |
| `montanteProjetadoComAportes` | BigDecimal | Projeção com `aporteMensalSugerido` todo mês |
| `metaAlcancavel` | Boolean | `true` se a projeção com aportes >= meta |

### 2.6 Cálculos financeiros

Todos os valores calculados são gerados em tempo real a cada requisição, em `CaixinhaService.calcularEMontar()`.

#### Taxa mensal efetiva

Conversão de taxa anual para mensal via **juros compostos**:

```
taxaMensal = (1 + taxaAnual/100)^(1/12) - 1
```

Para CDI/SELIC:
```
taxaAnual = taxaReferenciaAtual × (percentualRendimento / 100)
```

Para Poupança:
```
taxaAnual = taxaReferenciaAtual × 0.70
```

#### Montante projetado sem aportes

```
VF = VP × (1 + r)^n
```

#### Aporte mensal sugerido (PMT)

```
PMT = (VF - VP × (1+r)^n) × r / ((1+r)^n - 1)
```

Se `r = 0` (sem rendimento):
```
PMT = (VF - VP) / n
```

#### Montante projetado com aportes mensais

```
VF = VP × (1+r)^n + PMT × ((1+r)^n - 1) / r
```

Onde:
- `VP` = valor atual (soma dos aportes)
- `VF` = valor da meta
- `r` = taxa mensal efetiva (decimal)
- `n` = meses restantes até o prazo

### 2.7 Meta compartilhada

Uma caixinha pode vincular **múltiplas instituições**. O saldo total é a soma de todos os aportes (`EventoFinanceiro`) registrados para a caixinha, independente de qual instituição foi usada.

A resposta inclui o campo `instituicoes`, que lista cada banco/corretora vinculado com o `valorAportado` individual.

**Adicionar uma instituição depois da criação:**
```
POST /caixinhas/{caixinha_id}/instituicoes/{inst_id}
```
Isso automaticamente define `isCompartilhada = true`.

---

## 3. Sistema de E-mails

### 3.1 Configuração SMTP

No arquivo `application.properties`, descomente e preencha as linhas abaixo:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu-email@gmail.com
spring.mail.password=sua-senha-de-app
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.remetente=seu-email@gmail.com
app.email.habilitado=true
```

> 📌 **Gmail:** acesse [myaccount.google.com](https://myaccount.google.com) → **Segurança** → **Senhas de app** para gerar a senha de app (requer verificação em 2 etapas ativa).

Enquanto `app.email.habilitado=false` (padrão), nenhum e-mail é enviado — apenas log informativo aparece no console.

### 3.2 Tipos de alerta (`TipoAlertaEmail`)

| Enum | Quando é enviado |
|---|---|
| `ANIVERSARIO` | No dia do aniversário do usuário (verificado diariamente às 08:00) |
| `ALERTA_LIMITE_MENSAL` | Quando o usuário atinge o percentual de gasto configurado sobre o limite mensal |
| `ALERTA_META_POUPANCA` | Quando uma caixinha atinge o percentual de meta configurado (padrão: 90%) |
| `RELATORIO_MENSAL` | Resumo financeiro enviado todo dia 1° do mês às 08:00 |
| `LEMBRETE_APORTE` | Lembrete semanal toda segunda-feira às 08:00 com sugestão de aportes |

O usuário controla quais alertas deseja receber através do campo `alertasEmailAtivos` em **Configurações**.

### 3.3 Métodos do `EmailService`

| Método | Disparo | Conteúdo do e-mail |
|---|---|---|
| `enviarAlertaLimiteMensal(...)` | Ao registrar gasto que ultrapassa o limiar configurado | Gasto atual, limite, restante e alerta de estouro |
| `enviarAlertaMetaPoupanca(...)` | Ao criar/atualizar caixinha que atinge o threshold de meta | Nome da caixinha, saldo, meta, percentual e quanto falta |
| `enviarFelizAniversario(...)` | Job diário às 08:00 (se for o dia de aniversário) | Mensagem personalizada de parabéns |
| `enviarRelatorioMensal(...)` | Job todo dia 1° às 08:00 | Total de gastos do mês anterior + resumo de todas as caixinhas ativas |
| `enviarLembreteAporte(...)` | Job toda segunda-feira às 08:00 | Lista de caixinhas com aporte sugerido e progresso atual |

### 3.4 Jobs agendados (`RelatorioEmailJob.java`)

```
┌──────────────────────────────────────┬──────────────────────────────────┐
│ Cron                                 │ Ação                             │
├──────────────────────────────────────┼──────────────────────────────────┤
│ 0 0 8 * * *   (todo dia, 08:00)      │ Parabéns para aniversariantes    │
│ 0 0 8 1 * *   (dia 1°, 08:00)        │ Relatório mensal                 │
│ 0 0 8 * * MON (segunda-feira, 08:00) │ Lembrete de aportes semanais     │
└──────────────────────────────────────┴──────────────────────────────────┘
```

Cada job percorre **apenas os usuários** que têm o respectivo `TipoAlertaEmail` ativo em suas configurações. Usuários sem o alerta ativado não recebem o e-mail.

### 3.5 Integração com Configurações do usuário

O `CaixinhaService` consulta as configurações do usuário antes de enviar o alerta de meta:

```
Alerta de meta enviado somente se:
  ✓ TipoAlertaEmail.ALERTA_META_POUPANCA está em alertasEmailAtivos
  ✓ percentualAtingido >= config.percentualAlertaMeta (padrão: 90%)
  ✓ Não foi enviado alerta no mês corrente (ultimoAlertaMetaEnviado)
```

Após o envio, `ultimoAlertaMetaEnviado` é atualizado para evitar spam mensal.

---

## 4. Fluxo completo — exemplo de uso

```
1. Usuário cria uma caixinha "Viagem Japão" com meta R$ 15.000 e prazo 12/2026
   → POST /caixinhas

2. A API calcula e retorna:
   → valorAtual: R$ 0,00
   → aporteMensalSugerido: R$ XXX,XX (PMT baseado no prazo e rendimento)
   → montanteProjetadoComAportes: R$ 15.000,00

3. Usuário registra aportes mensais como EventoFinanceiro (tipo = Poupança)
   → vinculados à caixinha via caixinha_id

4. A cada consulta (GET /caixinhas/{id}), a API recalcula tudo em tempo real:
   → valorAtual atualizado
   → percentualAtingido recalculado
   → aporteMensalSugerido recalculado com meses restantes mais curtos

5. Quando percentualAtingido >= 90% (ou threshold configurado):
   → CaixinhaService dispara EmailService.enviarAlertaMetaPoupanca(...)
   → Usuário recebe e-mail de paraéns

6. Todo dia 1° do mês às 08:00:
   → RelatorioEmailJob gera e envia o relatório mensal para todos
     os usuários com RELATORIO_MENSAL ativo

7. Todo dia de aniversário do usuário às 08:00:
   → RelatorioEmailJob envia mensagem de parabéns (se ANIVERSARIO ativo)
```

---

## 5. Como ativar o envio de e-mails

**Passo a passo:**

1. No Gmail, acesse: [myaccount.google.com](https://myaccount.google.com) → **Segurança** → **Como você faz login no Google** → **Verificação em duas etapas** (ative se ainda não estiver ativa)

2. Ainda em Segurança, clique em **Senhas de app**, gere uma senha para "E-mail / Outro" e copie os 16 caracteres.

3. Edite `back-end/src/main/resources/application.properties`:

```properties
# Remova o # das linhas abaixo e preencha com seus dados:
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu-email@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx   ← senha de app (sem espaços)
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.remetente=seu-email@gmail.com
app.email.habilitado=true

# Comente ou remova esta linha:
# app.email.habilitado=false
```

4. Reinicie a aplicação. Verifique os logs: ao criar uma caixinha com meta, você verá `[EmailService] E-mail enviado com sucesso para: ...`.

> 💡 **Dica:** Para testar sem conta Gmail, você pode usar [Mailtrap](https://mailtrap.io) (serviço de SMTP fake para desenvolvimento). Basta trocar `spring.mail.host` e credenciais pelas fornecidas pelo Mailtrap.

---

*Documentação gerada em 01/07/2026.*

