Este é o seu Guia Mestre de Implementação. Ele está organizado como uma trilha de desenvolvimento, do código à distribuição.
------------------------------
## 📋 Checklist de Preparação

1. Ambiente: Java 17+ instalado, Python 3.10+ instalado.
2. Spring Boot: Projeto criado (usando Spring Initializr) com dependências: Spring Web, Spring Data JPA, H2 Database.
3. Dependências Desktop: Adicionar módulos JavaFX no pom.xml.
4. Python Embeddable: Baixar o ZIP "Windows embeddable package" no site oficial do Python.

------------------------------
## 🛠️ 1. Java & SQL Embedded (O Coração)
Configuraremos o Spring para criar um banco de dados que vive em um arquivo na pasta do usuário, não na memória.
No application.properties:

# Cria um arquivo chamado 'dados_financeiros.mv.db' na pasta do usuário
spring.datasource.url=jdbc:h2:file:~/dados_financeiros;AUTO_SERVER=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
# Desativa o modo headless para permitir janelas
spring.main.headless=false

------------------------------
## 🐍 2. Web Scraping com Python (A Ponte)
Usaremos o Nubank como exemplo por ser o mais comum.
Instalação: pip install pynubank
Script scraper.py:

from pynubank import Nubankimport sysimport json
def buscar_dados(cpf, senha):
nu = Nubank()
# O comando abaixo pede o certificado que você deve gerar antes
nu.authenticate_with_cert(cpf, senha, "cert.p12")

    transacoes = nu.get_account_statements()
    # Retorna apenas o JSON para o Java ler
    print(json.dumps(transacoes))
if __name__ == "__main__":
buscar_dados(sys.argv[1], sys.argv[2])

------------------------------
## 🔗 3. Integrando Java + Python
No seu Service do Spring, você chama o script e captura a saída.

public String executarScraping(String cpf, String senha) throws Exception {
// Aponta para o python embutido na pasta do projeto
String pythonPath = "./python/python.exe";
ProcessBuilder pb = new ProcessBuilder(pythonPath, "scraper.py", cpf, senha);
Process p = pb.start();

    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
    return in.readLine(); // Aqui você recebe o JSON do Python
}

------------------------------
## 🖼️ 4. Interface Desktop (JavaFX WebView)
Transformando o Spring em uma janela.
Classe Principal:

@SpringBootApplicationpublic class App extends Application {
private static ConfigurableApplicationContext context;

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().load("http://localhost:8080"); // Carrega seu front
        stage.setScene(new Scene(webView, 1024, 768));
        stage.show();
    }

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        launch(args);
    }
}

------------------------------
## 📦 5. Transformando em .exe (O Final)## Passo A: Gerar o JAR
No terminal do projeto: mvn clean package. Isso gera seu arquivo na pasta target.
## Passo B: Launch4j (Criar o Executável)

1. Abra o Launch4j.
2. Output file: Escolha onde salvar o .exe.
3. Jar: Selecione o seu JAR gerado pelo Maven.
4. Tab JRE: No campo "Min JRE version", coloque 17.
5. Clique na engrenagem para gerar o .exe.

## Passo C: JPackage (Opcional - Para não exigir Java instalado)
Se quiser que o usuário não precise instalar o Java, use este comando no terminal (Java 14+):

jpackage --input target/ --name MeuAppFinanceiro --main-jar seu-projeto.jar --type exe --win-dir-chooser

------------------------------
## 🚀 Tutorial de Distribuição (O que entregar ao usuário)
Para o software funcionar na máquina de outra pessoa, você deve entregar uma pasta contendo:

1. MeuApp.exe
2. scraper.py
3. Pasta python/ (o pacote embeddable que você baixou).
4. cert.p12 (certificado do Nubank, se for o caso).

Dica Final: Como o scraping é instável, sempre adicione no seu Front-end um botão de "Importar OFX". Assim, se o banco mudar o site e o script Python quebrar, seu software continua sendo útil enquanto você atualiza o código!

