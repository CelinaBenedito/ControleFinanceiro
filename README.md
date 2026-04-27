# Controle Financeiro - API e Aplicação

## 📖 Descrição
Aplicativo de controle financeiro desenvolvido em Java, com front-end em HTML, CSS e JS servido via JavaFX. Criado inicialmente em 2025 para substituir planilhas Excel, evoluiu até a versão atual (2.6), oferecendo dashboards, relatórios e integração com dados bancários.

---

## 🚀 Tecnologias
- Java (backend)
- JavaFX (frontend)
- HTML, CSS, JS
- Banco H2 (desenvolvimento) / Arquivo local (produção)
- Python (web scraping)
- Figma (design do front-end) [Link do Figma](https://www.figma.com/design/v8ZtkZ3IXgQetlbq0TfgfZ/ControleFInanceiro?node-id=0-1&t=HpyyFlr4rp0FdPh4-1)

---

## 🧩 Estrutura da API
### Controllers Principais
- Usuário
- Registros

---

### Controllers Auxiliares
- Instituições
- Categorias
- Configurações
- Dashboard

---

## 📊 Funcionalidades
- Dashboard com KPIs e gráficos
- Inserção de registros (individual ou múltiplos por dia)
- Visualização geral (ano/mês com filtros)
- Visualização em calendário (dia a dia com resumo mensal)
- Suporte a múltiplas contas para controle pessoal do usuário
- Exportação/importação de dados locais
  
  ---

## 🔌 Endpoints

### POST /registros
**Descrição:** Cria um novo evento financeiro adicionando-o ao banco de dados.  

**Request body (application/json):**
```json
{
  "financeiro": {
    "usuario_id": "21eb5d2f-3fd8-439e-b647-5cc1f753ae58",
    "tipo": "1",
    "valor": 100,
    "descricao": "Champagne de Ano novo",
    "dataEvento": "2026-01-01"
  },
  "instituicao": [
    {
      "instituicaoUsuario_id": 2,
      "tipoMovimento": "Credito",
      "valor": 125.5,
      "parcelas": 2
    }
  ],
  "detalhe": {
    "categoriaUsuario_id": [1, 2],
    "tituloGasto": "Champagne"
  }
}
```
**Resposta esperada:**

201 Created em caso de sucesso
400 Bad Request se o corpo da requisição estiver inválido

---

## 🔮 Roadmap
- Implementação de tokenização/autenticação
- Expansão da integração com instituições financeiras
- Melhorias na interface e usabilidade

---

## 🛠️ Instalação
###Pré-requisitos:
- Java 21 instalado
- Maven configurado
  
### Passos para rodar:
1. Clone o repositório
2. Abra o projeto em sua IDE favorita
3. Execute a aplicação com o comando:
***mvn spring-boot:run***
>ou simplesmente dar play na IDE.

### Dependências principais:
| Depêndencia                                | Função                                |
|:-------------------------------------------|:--------------------------------------|
| spring-boot-starter-data-jpa               | persistência com JPA                  |
| spring-boot-starter-web                    | criação dos endpoints REST            |
| springdoc-openapi-starter-webmvc-ui        | integração com Swagger/OpenAPI        |
| h2                                         | banco em memória para desenvolvimento |
| mysql-connector-j                          | suporte a MySQL em runtime            |
| javafx-controls e javafx-web               | front-end com JavaFX                  |

---

## 🤝 Contribuição
Este projeto é aberto para colaboração.

- Crie um fork do repositório

- Implemente sua funcionalidade

- Envie um pull request

> Todos os contribuidores terão seus créditos destacados.

---

## 📜 Licença
Este projeto é livre para uso e modificação.
Não há fins lucrativos envolvidos, sendo voltado para aprendizado e desenvolvimento pessoal.
