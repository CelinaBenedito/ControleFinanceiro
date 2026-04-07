## 📁 Roadmap: Minha API Financeira (Java + Python + Scraping)
Este documento resume a estratégia para criar um agregador financeiro pessoal, do backend ao executável final.
## 1. Definição do Modelo de Dados

* Abordagem Selecionada: Web Scraping (Foco em projetos pessoais/estudo).
* Por que não Open Finance oficial? Custo elevado e exigências regulatórias (BACEN) para produção.
* Fonte de Dados Recomendada:
* Nubank: Via biblioteca pynubank (Python).
    * Banco Inter: Via API oficial gratuita (SDK Python).
    * Universal: Importação de arquivos .OFX (Padrão bancário).

## 2. Arquitetura do Sistema (Híbrida)
Para não reescrever o código Spring (Java), utilizaremos uma arquitetura de microserviços locais:

* Backend Principal (Java + Spring Boot): Gerencia o banco de dados (H2 ou PostgreSQL), regras de negócio e segurança.
* Módulo de Extração (Python + FastAPI): Um "serviço auxiliar" que roda os scripts de scraping e devolve JSON para o Spring.
* Comunicação: O Spring consome o Python via WebClient ou RestTemplate em localhost.

## 3. O "Pulo do Gato": O Executável Único (.exe)
Para esconder a complexidade do usuário e entregar um software profissional:
## Estratégia de Empacotamento:

1. Python Embarcado: Baixar o Windows embeddable package do Python e incluí-lo na pasta do projeto.
2. Scripts Python: Usar o PyInstaller para transformar os scripts em .exe independentes, facilitando a chamada pelo Java.
3. Java Maestro: O Spring Boot inicia os processos de background e abre o navegador automaticamente no ApplicationReadyEvent.
4. Launch4j: Ferramenta para transformar o seu .jar em um .exe com ícone personalizado.
5. JPackage: Para criar um instalador oficial que já inclui o Java (JRE), dispensando a instalação prévia pelo usuário.

## 4. Próximos Passos Técnicos

1. Fase 1: Criar um script Python simples que faz login no Nubank e dá print no extrato.
2. Fase 2: Criar um Controller no Spring que executa esse script via ProcessBuilder.
3. Fase 3: Integrar o Front-end (React/Thymeleaf) para exibir esses dados.
4. Fase 4: Gerar o .exe usando Launch4j para testes de distribuição.

------------------------------
## 💡 Dicas de Ouro:

* Segurança: Use arquivos .env para credenciais; nunca deixe senhas no código.
* Estabilidade: Scraping quebra. Sempre implemente um "Plano B" de importação manual de arquivo OFX no seu sistema Java.
* Simulação: Use o Sandbox de empresas como Pluggy ou Belvo para testar fluxos de Open Finance real sem pagar nada durante o desenvolvimento.

------------------------------
Deseja que eu detalhe algum desses tópicos (como o código do ProcessBuilder ou a configuração do Launch4j) para você salvar também?

