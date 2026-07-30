package controle.api.back_end.config;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

public class DesktopApp extends Application {

    private static final String APP_URL = "http://localhost:8080/index.html";

    /** Referencia estatica ao Stage principal — usada pelo UpdateService para fechar a janela. */
    private static volatile Stage primaryStage;

    /** Fecha a janela JavaFX de forma segura a partir de qualquer thread. */
    public static void exitApplication() {
        Platform.runLater(() -> {
            if (primaryStage != null) primaryStage.close();
        });
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Injeta a DesktopBridge em cada pagina carregada
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("desktopBridge", new DesktopBridge(stage));
                System.out.println("[DesktopApp] desktopBridge injetada com sucesso.");
            }
        });

        // Exibe tela de carregamento enquanto o Spring Boot sobe
        engine.loadContent(buildLoadingPage(), "text/html");

        // Icone da janela — JavaFX suporta PNG, nao ICO
        carregarIconePNG(stage);

        Scene scene = new Scene(webView, 1200, 800);
        stage.setTitle("MyFinance");
        stage.setScene(scene);
        stage.show();

        // Aguarda o Spring Boot estar pronto e depois navega via HTTP
        // (carregamento HTTP resolve o problema de fontes/icones nao carregarem via jar://)
        iniciarPollerSpringBoot(engine);
    }

    // -------------------------------------------------------------------------
    // Carregamento da aplicacao via HTTP
    // -------------------------------------------------------------------------

    /**
     * Aguarda o Spring Boot responder na porta 8080 e entao navega para a pagina inicial.
     * Carregar via HTTP (em vez de jar://) garante que fontes e outros assets
     * sejam servidos corretamente pelo Tomcat embutido.
     */
    private void iniciarPollerSpringBoot(WebEngine engine) {
        Thread poller = new Thread(() -> {
            int tentativas = 0;
            int maxTentativas = 90; // aguarda ate 90s

            while (tentativas < maxTentativas) {
                try {
                    Thread.sleep(1000);
                    HttpURLConnection conn = (HttpURLConnection) new URL(APP_URL).openConnection();
                    conn.setConnectTimeout(1500);
                    conn.setReadTimeout(1500);
                    conn.setRequestMethod("GET");
                    int status = conn.getResponseCode();
                    conn.disconnect();

                    if (status == 200 || status == 302) {
                        System.out.println("[DesktopApp] Spring Boot pronto. Navegando para " + APP_URL);
                        Platform.runLater(() -> engine.load(APP_URL));
                        return;
                    }
                } catch (Exception ignored) {}

                tentativas++;

                // Atualiza mensagem de progresso a cada 5s
                if (tentativas % 5 == 0) {
                    final int seg = tentativas;
                    Platform.runLater(() ->
                        engine.executeScript(
                            "var el = document.getElementById('mf-status');" +
                            "if(el) el.textContent = 'Iniciando... (" + seg + "s)';"
                        )
                    );
                }
            }

            // Fallback: tenta carregar do classpath se HTTP nao responder
            System.err.println("[DesktopApp] Timeout aguardando Spring Boot. Tentando fallback do classpath.");
            Platform.runLater(() -> {
                try {
                    String fallback = getClass().getResource("/static/index.html").toExternalForm();
                    engine.load(fallback);
                } catch (Exception e) {
                    engine.loadContent("<h2 style='font-family:sans-serif;color:red'>Falha ao iniciar o servidor. Tente reiniciar o aplicativo.</h2>", "text/html");
                }
            });
        });
        poller.setDaemon(true);
        poller.setName("spring-boot-poller");
        poller.start();
    }

    // -------------------------------------------------------------------------
    // Icone da janela (Stage)
    // -------------------------------------------------------------------------

    /**
     * JavaFX suporta apenas PNG/BMP/GIF/JPEG para Stage.getIcons().
     * O arquivo .ico e usado pelo jpackage para o icone do .exe no Windows Explorer,
     * mas a janela JavaFX precisa do PNG.
     */
    private void carregarIconePNG(Stage stage) {
        // Tenta diferentes nomes/locais do icone PNG
        String[] caminhos = {
            "/static/assets/glaceonIcon .png",  // nome original com espaco
            "/static/assets/glaceonIcon.png",   // sem espaco
            "/static/assets/icon.png"
        };

        for (String caminho : caminhos) {
            try (InputStream is = getClass().getResourceAsStream(caminho)) {
                if (is != null) {
                    Image icone = new Image(is);
                    if (!icone.isError()) {
                        stage.getIcons().add(icone);
                        System.out.println("[DesktopApp] Icone carregado: " + caminho);
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("[DesktopApp] Icone nao encontrado em: " + caminho);
            }
        }

        System.out.println("[DesktopApp] Nenhum icone PNG encontrado. Usando icone padrao do sistema.");
    }

    // -------------------------------------------------------------------------
    // Tela de carregamento
    // -------------------------------------------------------------------------

    private String buildLoadingPage() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<style>" +
               "* { margin:0; padding:0; box-sizing:border-box; }" +
               "body { display:flex; align-items:center; justify-content:center;" +
               "       height:100vh; background:#f0f4f8; font-family:sans-serif; }" +
               ".card { text-align:center; background:#fff; border-radius:16px;" +
               "        padding:48px 56px; box-shadow:0 8px 32px rgba(0,0,0,.12); }" +
               "h1 { font-size:1.6rem; color:#1a3a3a; margin-bottom:8px; }" +
               "p  { font-size:0.95rem; color:#667; margin-bottom:24px; }" +
               ".spinner { width:40px; height:40px; border:4px solid #e0e7ef;" +
               "           border-top-color:#367373; border-radius:50%;" +
               "           animation:spin .8s linear infinite; margin:0 auto; }" +
               "@keyframes spin { to { transform:rotate(360deg); } }" +
               "</style></head>" +
               "<body><div class='card'>" +
               "<h1>MyFinance</h1>" +
               "<p id='mf-status'>Iniciando, aguarde...</p>" +
               "<div class='spinner'></div>" +
               "</div></body></html>";
    }

    // -------------------------------------------------------------------------
    // DesktopBridge
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        launch(args);
    }

    public static class DesktopBridge {
        private final Stage stage;

        public DesktopBridge(Stage stage) {
            this.stage = stage;
        }

        public boolean saveBase64File(String fileName, String base64Content) {
            if (base64Content == null || base64Content.isBlank()) {
                System.out.println("[DesktopApp] Exportacao abortada: conteudo vazio.");
                return false;
            }

            String safeFileName = (fileName == null || fileName.isBlank()) ? "registros.pdf" : fileName;
            String extension = getFileExtension(safeFileName);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar arquivo exportado");
            fileChooser.setInitialFileName(safeFileName);
            fileChooser.getExtensionFilters().add(createExtensionFilter(extension));

            File targetFile = fileChooser.showSaveDialog(stage);
            if (targetFile == null) {
                System.out.println("[DesktopApp] Exportacao cancelada pelo usuario.");
                return false;
            }

            try {
                byte[] bytes = Base64.getDecoder().decode(base64Content);
                Files.write(targetFile.toPath(), bytes);
                System.out.println("[DesktopApp] Arquivo salvo em: " + targetFile.getAbsolutePath());
                return true;
            } catch (IllegalArgumentException | IOException e) {
                throw new RuntimeException("Falha ao salvar arquivo exportado.", e);
            }
        }

        private String getFileExtension(String fileName) {
            int dot = fileName.lastIndexOf('.');
            if (dot < 0 || dot == fileName.length() - 1) return "pdf";
            return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        }

        private FileChooser.ExtensionFilter createExtensionFilter(String extension) {
            switch (extension) {
                case "json":  return new FileChooser.ExtensionFilter("Arquivos JSON",    "*.json");
                case "sql":   return new FileChooser.ExtensionFilter("Arquivos SQL",     "*.sql");
                case "xlsx":
                case "excel": return new FileChooser.ExtensionFilter("Planilhas Excel",  "*.xlsx");
                case "pdf":
                default:      return new FileChooser.ExtensionFilter("Arquivos PDF",     "*.pdf");
            }
        }
    }
}
