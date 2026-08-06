package controle.api.back_end.config;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

public class DesktopApp extends Application {

    private static final String APP_URL = "http://localhost:8080/index.html";

    /** Referencia estatica ao Stage principal — usada pelo UpdateService para fechar a janela. */
    private static volatile Stage primaryStage;

    /** System Tray icon */
    private TrayIcon trayIcon;
    private boolean exitingApp = false;
    private volatile boolean trayInitialized = false;

    /** Fecha a janela JavaFX de forma segura a partir de qualquer thread. */
    public static void exitApplication() {
        Platform.runLater(() -> {
            if (primaryStage != null) primaryStage.close();
        });
    }

    @Override
    public void start(Stage stage) {
        // Garantir que headless está desabilitado (CRITICAL para System Tray)
        String headlessValue = System.getProperty("java.awt.headless");
        if ("true".equals(headlessValue)) {
            System.err.println("[DesktopApp] ✗ ATENÇÃO: Modo headless detectado como TRUE!");
            System.err.println("[DesktopApp] Forçando java.awt.headless=false...");
            System.setProperty("java.awt.headless", "false");
        }

        primaryStage = stage;

        // Verificação inicial do System Tray
        System.out.println("[DesktopApp] ========================================");
        System.out.println("[DesktopApp] Iniciando aplicação MyFinance");
        System.out.println("[DesktopApp] java.awt.headless = " + System.getProperty("java.awt.headless"));
        System.out.println("[DesktopApp] System Tray suportado: " + SystemTray.isSupported());
        System.out.println("[DesktopApp] Java Version: " + System.getProperty("java.version"));
        System.out.println("[DesktopApp] OS: " + System.getProperty("os.name"));
        System.out.println("[DesktopApp] ========================================");

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Injeta a DesktopBridge em cada pagina carregada
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                DesktopBridge bridge = new DesktopBridge(stage);
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("desktopBridge", bridge);
                System.out.println("[DesktopApp] desktopBridge injetada com sucesso.");

                // Se estiver na tela de seleção de perfil, injeta os perfis diretamente
                // via Java (evita dependência de localStorage em contexto async do WebView)
                String loc = engine.getLocation();
                if (loc != null && (loc.contains("/index.html") || loc.matches(".*localhost:8080/?$"))) {
                    try {
                        String perfisJson = bridge.loadPerfis();
                        window.setMember("_perfisJsonFromBridge", perfisJson);
                        engine.executeScript(
                            "if (typeof window._carregarPerfisViaBridge === 'function') " +
                            "  window._carregarPerfisViaBridge(window._perfisJsonFromBridge);"
                        );
                        System.out.println("[DesktopApp] Perfis injetados na index.html via bridge.");
                    } catch (Exception ex) {
                        System.out.println("[DesktopApp] Aviso ao injetar perfis: " + ex.getMessage());
                    }
                }
            }
        });

        // Exibe tela de carregamento enquanto o Spring Boot sobe
        engine.loadContent(buildLoadingPage(), "text/html");

        // Icone da janela — JavaFX suporta PNG, nao ICO
        carregarIconePNG(stage);

        Scene scene = new Scene(webView, 1200, 800);
        stage.setTitle("MyFinance");
        stage.setScene(scene);

        // Configura o comportamento ao fechar a janela
        stage.setOnCloseRequest(event -> {
            if (!exitingApp) {
                event.consume();
                showCloseDialog(stage);
            } else {
                // Encerra a aplicação completamente
                cleanup();
            }
        });

        // Configura o system tray antes de mostrar a janela
        Platform.setImplicitExit(false); // Não encerra o JavaFX quando todas as janelas são fechadas
        setupSystemTray(stage);

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

                    // Tenta primeiro apenas conectar TCP (rapido) antes de fazer HTTP completo
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress("127.0.0.1", 8080), 500);
                    }

                    // Porta aberta — faz requisicao HTTP completa
                    HttpURLConnection conn = (HttpURLConnection) new URL(APP_URL).openConnection();
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(3000);
                    conn.setRequestMethod("GET");
                    conn.setInstanceFollowRedirects(false);
                    int status = conn.getResponseCode();
                    conn.disconnect();

                    if (status >= 200 && status < 500) {
                        System.out.println("[DesktopApp] Spring Boot pronto (HTTP " + status + "). Navegando para " + APP_URL);
                        Platform.runLater(() -> engine.load(APP_URL));
                        return;
                    }
                } catch (Exception ignored) {}

                tentativas++;

                // Atualiza mensagem de progresso a cada 5s
                if (tentativas % 5 == 0) {
                    final int seg = tentativas;
                    // Detecta se o Spring Boot falhou (porta 8080 nunca abriu mas processo JVM ainda existe)
                    boolean portaDB = isPortResponding8080();
                    String msg = portaDB
                        ? "Aguardando servidor... (" + seg + "s)"
                        : "Inicializando banco de dados... (" + seg + "s)";
                    Platform.runLater(() ->
                        engine.executeScript(
                            "var el = document.getElementById('mf-status');" +
                            "if(el) el.textContent = '" + msg + "';"
                        )
                    );
                }
            }

            // Timeout — mostra mensagem de erro clara ao usuario
            System.err.println("[DesktopApp] Timeout aguardando Spring Boot apos " + maxTentativas + "s.");
            Platform.runLater(() ->
                engine.loadContent(buildErrorPage(
                    "Falha ao iniciar o servidor",
                    "O servidor não respondeu em " + maxTentativas + " segundos.<br>" +
                    "Possíveis causas:<br>" +
                    "• Antivírus bloqueando o processo mysqld<br>" +
                    "• Outra instância do banco de dados ainda em execução<br>" +
                    "• Memória ou recursos insuficientes<br><br>" +
                    "Tente fechar e abrir o aplicativo novamente."
                ), "text/html")
            );
        });
        poller.setDaemon(true);
        poller.setName("spring-boot-poller");
        poller.start();
    }

    private static boolean isPortResponding8080() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", 8080), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
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
    // System Tray
    // -------------------------------------------------------------------------

    /**
     * Configura o ícone na bandeja do sistema com menu de contexto.
     */
    private void setupSystemTray(Stage stage) {
        System.out.println("[DesktopApp] Configurando System Tray...");

        if (!SystemTray.isSupported()) {
            System.err.println("[DesktopApp] ✗ System Tray não é suportado!");
            System.err.println("[DesktopApp]   java.awt.headless = " + System.getProperty("java.awt.headless"));
            System.err.println("[DesktopApp]   OS: " + System.getProperty("os.name"));
            return;
        }

        System.out.println("[DesktopApp] ✓ System Tray é suportado!");

        try {
            java.awt.Image trayImage = loadTrayIcon();

            if (trayImage == null) {
                System.err.println("[DesktopApp] ✗ Falha ao carregar imagem do tray icon!");
                return;
            }

            SystemTray tray = SystemTray.getSystemTray();

            // Menu popup do tray
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Mostrar MyFinance");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                stage.show();
                stage.toFront();
                stage.requestFocus();
            }));

            MenuItem exitItem = new MenuItem("Sair");
            exitItem.addActionListener(e -> {
                exitingApp = true;
                Platform.runLater(() -> {
                    cleanup();
                    Platform.exit();
                });
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(trayImage, "MyFinance", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("MyFinance - Clique para abrir");

            // Duplo clique no ícone mostra a janela
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                stage.show();
                stage.toFront();
                stage.requestFocus();
            }));

            tray.add(trayIcon);

            // Aguarda um pouco para garantir que foi adicionado
            Thread.sleep(100);

            trayInitialized = true;
            System.out.println("[DesktopApp] ✓ Ícone adicionado à bandeja do sistema com sucesso!");

        } catch (AWTException e) {
            System.err.println("[DesktopApp] ✗ Falha AWTException ao adicionar ícone: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[DesktopApp] ✗ Erro ao configurar system tray: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carrega o ícone para o system tray (AWT Image).
     */
    private java.awt.Image loadTrayIcon() {
        String[] caminhos = {
            "/static/assets/glaceonIcon .png",
            "/static/assets/glaceonIcon.png",
            "/static/assets/icon.png"
        };

        for (String caminho : caminhos) {
            try (InputStream is = getClass().getResourceAsStream(caminho)) {
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) {
                        System.out.println("[DesktopApp] ✓ Ícone do tray carregado: " + caminho);
                        return img;
                    }
                }
            } catch (Exception e) {
                // Continua tentando outros caminhos
            }
        }

        // Fallback: cria um ícone simples se não encontrar nenhum
        System.out.println("[DesktopApp] ⚠ Usando ícone padrão (fallback)");
        try {
            BufferedImage fallback = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = fallback.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Fundo colorido
            g2d.setColor(new Color(54, 115, 115));
            g2d.fillOval(2, 2, 28, 28);
            
            // Borda
            g2d.setColor(new Color(40, 90, 90));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(2, 2, 28, 28);
            
            // Letra M
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("M", 9, 23);
            
            g2d.dispose();
            return fallback;
        } catch (Exception e) {
            System.err.println("[DesktopApp] ✗ ERRO ao criar ícone fallback: " + e.getMessage());
            return null;
        }
    }

    /**
     * Mostra uma notificação na bandeja do sistema.
     */
    private void showTrayNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    /**
     * Mostra um diálogo perguntando ao usuário o que fazer ao fechar a janela.
     */
    private void showCloseDialog(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Fechar MyFinance");
        alert.setHeaderText("O que você deseja fazer?");
        alert.setContentText("Escolha uma opção:");

        ButtonType minimizarButton = new ButtonType("Minimizar para Bandeja");
        ButtonType fecharButton = new ButtonType("Fechar Aplicação");
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(minimizarButton, fecharButton, cancelarButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == minimizarButton) {
                System.out.println("[DesktopApp] Minimizando para a bandeja...");
                
                // Verifica se o tray icon foi inicializado
                if (!trayInitialized || trayIcon == null) {
                    System.err.println("[DesktopApp] ✗ System Tray não inicializado!");
                    System.err.println("[DesktopApp] Tentando reconfigurar...");
                    
                    // Tenta configurar novamente
                    setupSystemTray(stage);
                    
                    // Verifica se funcionou
                    if (!trayInitialized || trayIcon == null) {
                        System.err.println("[DesktopApp] ✗ Falha ao reconfigurar system tray.");
                        
                        // Mostra alerta ao usuário
                        Alert errorAlert = new Alert(Alert.AlertType.WARNING);
                        errorAlert.setTitle("System Tray não disponível");
                        errorAlert.setHeaderText("Não foi possível minimizar para a bandeja");
                        errorAlert.setContentText("O ícone da bandeja do sistema não está disponível.\n\n" +
                            "Possíveis causas:\n" +
                            "• Sistema operacional não suporta bandeja do sistema\n" +
                            "• Serviço de bandeja desabilitado no sistema\n" +
                            "• Java em modo headless\n\n" +
                            "A janela permanecerá visível.");
                        errorAlert.showAndWait();
                        return;
                    }
                    System.out.println("[DesktopApp] ✓ Reconfiguração bem-sucedida!");
                }
                
                stage.hide();
                showTrayNotification("MyFinance", "Aplicação minimizada para a bandeja do sistema");
                System.out.println("[DesktopApp] ✓ Janela minimizada com sucesso!");
                
            } else if (response == fecharButton) {
                System.out.println("[DesktopApp] Fechando aplicação...");
                exitingApp = true;
                cleanup();
                Platform.exit();
            }
        });
    }

    /**
     * Limpeza ao encerrar a aplicação: remove o ícone do tray e encerra o Spring Boot.
     */
    private void cleanup() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }

        // Encerra o Spring Boot
        System.out.println("[DesktopApp] Encerrando aplicação...");
        System.exit(0);
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
               "           border-top-color:#367373; border-radius:50%; margin:0 auto;" +
               "           -webkit-animation:spin .8s linear infinite;" +
               "           animation:spin .8s linear infinite; }" +
               "@-webkit-keyframes spin {" +
               "  0%   { -webkit-transform:rotate(0deg);   transform:rotate(0deg); }" +
               "  100% { -webkit-transform:rotate(360deg); transform:rotate(360deg); } }" +
               "@keyframes spin {" +
               "  0%   { transform:rotate(0deg); }" +
               "  100% { transform:rotate(360deg); } }" +
               "</style></head>" +
               "<body><div class='card'>" +
               "<h1>MyFinance</h1>" +
               "<p id='mf-status'>Inicializando banco de dados...</p>" +
               "<div class='spinner'></div>" +
               "</div></body></html>";
    }

    private String buildErrorPage(String titulo, String mensagem) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<style>" +
               "* { margin:0; padding:0; box-sizing:border-box; }" +
               "body { display:flex; align-items:center; justify-content:center;" +
               "       height:100vh; background:#f0f4f8; font-family:sans-serif; }" +
               ".card { text-align:center; background:#fff; border-radius:16px;" +
               "        padding:48px 56px; box-shadow:0 8px 32px rgba(0,0,0,.12); max-width:480px; }" +
               "h1 { font-size:1.4rem; color:#c0392b; margin-bottom:16px; }" +
               "p  { font-size:0.9rem; color:#555; line-height:1.6; text-align:left; }" +
               "button { margin-top:24px; padding:10px 24px; background:#367373; color:#fff;" +
               "         border:none; border-radius:8px; font-size:1rem; cursor:pointer; }" +
               "button:hover { background:#2a5858; }" +
               "</style></head>" +
               "<body><div class='card'>" +
               "<h1>⚠ " + titulo + "</h1>" +
               "<p>" + mensagem + "</p>" +
               "<button onclick='window.location.reload()'>Tentar novamente</button>" +
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
        private static final java.nio.file.Path PERFIS_FILE = java.nio.file.Path.of(
                System.getProperty("user.home"), ".myfinance", "perfis.json");

        public DesktopBridge(Stage stage) {
            this.stage = stage;
        }

        /** Carrega a lista de perfis salvos em disco. Retorna "[]" se não existir. */
        public String loadPerfis() {
            try {
                if (!Files.exists(PERFIS_FILE)) return "[]";
                return Files.readString(PERFIS_FILE);
            } catch (Exception e) {
                System.out.println("[DesktopBridge] Erro ao carregar perfis: " + e.getMessage());
                return "[]";
            }
        }

        /** Salva a lista de perfis em disco (JSON). */
        public void savePerfis(String json) {
            try {
                Files.createDirectories(PERFIS_FILE.getParent());
                Files.writeString(PERFIS_FILE, json == null ? "[]" : json);
            } catch (Exception e) {
                System.out.println("[DesktopBridge] Erro ao salvar perfis: " + e.getMessage());
            }
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
