package controle.api.launcher;

import org.update4j.Configuration;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Bootstrap do MyFinance.
 *
 * Fluxo:
 *  1. Abre log em %APPDATA%\MyFinance\launcher.log (visivel mesmo sem console).
 *  2. Detecta o diretorio do proprio launcher.jar para localizar back-end.jar.
 *  3. Aplica atualizacao pendente (%APPDATA%\MyFinance\pending-update.jar), se existir.
 *  4. Verifica atualizacoes via Update4j (se configurado).
 *  5. Inicia back-end.jar como processo separado usando o java.home correto.
 */
public class LauncherApp {

    private static PrintWriter logWriter;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        initLog();
        log("=== MyFinance Launcher iniciado ===");
        log("java.home  : " + System.getProperty("java.home"));
        log("user.dir   : " + System.getProperty("user.dir"));

        try {
            Properties props = loadProperties();

            // Localiza o launcher.jar no sistema de arquivos para resolver caminhos relativos
            Path launcherDir = detectLauncherDir();
            log("launcherDir: " + launcherDir);

            String appDirName = props.getProperty("app.dir", "app");
            String appJarName = props.getProperty("app.jar.name", "back-end.jar");
            Path appJarPath   = launcherDir.resolve(appDirName).resolve(appJarName).toAbsolutePath();
            log("appJarPath : " + appJarPath);

            // Garante que o diretorio de destino existe
            Files.createDirectories(appJarPath.getParent());

            // Aplica atualizacao pendente baixada pelo back-end (se existir)
            applyPendingUpdate(appJarPath);

            // Verifica e aplica atualizacoes via Update4j (se configurado)
            String configUrl = props.getProperty("update4j.config.url", "");
            tryUpdate(configUrl, appJarPath);

            // Inicia a aplicacao principal
            launchApp(appJarPath, args);

        } catch (Exception e) {
            log("ERRO FATAL: " + e.getMessage());
            e.printStackTrace(logWriter);
            logWriter.flush();
            showErrorDialog("Erro ao iniciar o MyFinance:\n" + e.getMessage()
                    + "\n\nConsulte o log em: " + logFilePath());
        } finally {
            if (logWriter != null) logWriter.close();
        }
    }

    // -------------------------------------------------------------------------
    // Atualizacao pendente (baixada pelo back-end durante execucao)
    // -------------------------------------------------------------------------

    /**
     * O back-end baixa o novo JAR em %APPDATA%\MyFinance\pending-update.jar
     * e encerra a aplicacao. Na proxima inicializacao, o launcher copia o
     * arquivo pendente sobre o back-end.jar antes de inicia-lo.
     * Isso evita o problema de lock de arquivo no Windows (o JAR nao esta
     * sendo executado quando o launcher tenta substitui-lo).
     */
    private static void applyPendingUpdate(Path appJarPath) {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        Path pendingJar = Path.of(appData, "MyFinance", "pending-update.jar");

        if (!Files.exists(pendingJar)) return;

        log("[Update] Atualizacao pendente encontrada: " + pendingJar);
        try {
            // Aguarda o processo anterior encerrar e liberar o lock do back-end.jar
            // (o back-end agenda o restart via VBScript que espera 3s antes de abrir o launcher,
            //  mas o JVM pode demorar um pouco mais para liberar todos os file handles)
            Thread.sleep(5000);
            Files.copy(pendingJar, appJarPath, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(pendingJar);
            log("[Update] Atualizacao aplicada com sucesso. Nova versao iniciando.");
        } catch (Exception e) {
            log("[Update] Falha ao aplicar atualizacao pendente: " + e.getMessage()
                    + " — iniciando versao anterior.");
            try { Files.deleteIfExists(pendingJar); } catch (Exception ignored) {}
        }
    }

    private static void tryUpdate(String configUrl, Path appJarPath) {
        if (configUrl == null || configUrl.isBlank() || configUrl.contains("SEU_USUARIO")) {
            log("[Update] URL nao configurada — pulando verificacao.");
            return;
        }
        try {
            log("[Update] Verificando: " + configUrl);
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .header("User-Agent", "MyFinance-Launcher/1.0").build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log("[Update] Config indisponivel (HTTP " + resp.statusCode() + ").");
                return;
            }

            Configuration config;
            try (Reader r = new StringReader(resp.body())) {
                config = Configuration.read(r);
            }

            if (!config.requiresUpdate()) {
                log("[Update] Ja esta na versao mais recente.");
                return;
            }

            config.update();
            log("[Update] Atualizacao concluida.");

        } catch (Exception e) {
            log("[Update] Aviso (nao critico): " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Inicializacao da aplicacao principal
    // -------------------------------------------------------------------------

    private static void launchApp(Path appJarPath, String[] args) throws Exception {
        if (!Files.exists(appJarPath)) {
            throw new FileNotFoundException(
                    "back-end.jar nao encontrado em: " + appJarPath
                    + "\nVerifique se a instalacao esta completa."
            );
        }

        String javaExe = detectJavaExecutable();
        log("[Launch] Executavel Java: " + javaExe);

        List<String> command = new ArrayList<>();
        command.add(javaExe);

        // Flags necessarias para JavaFX funcionar a partir de um fat-JAR
        command.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        command.add("--add-opens=java.base/java.util=ALL-UNNAMED");

        // Informa ao back-end onde ele proprio esta no disco (usado pelo UpdateService)
        command.add("-Dmyfinance.jar.path=" + appJarPath.toString());

        command.add("-jar");
        command.add(appJarPath.toString());

        // Repassa argumentos de aplicacao (nao flags JVM)
        for (String arg : args) {
            if (!arg.startsWith("-X") && !arg.startsWith("-D") && !arg.startsWith("--add")) {
                command.add(arg);
            }
        }

        log("[Launch] Comando: " + String.join(" ", command));
        logWriter.flush();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(appJarPath.getParent().toFile()); // CWD = pasta do JAR
        pb.redirectErrorStream(true);

        // Redireciona stdout/stderr do processo filho para o arquivo de log
        Path logFile = Path.of(logFilePath());
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        Process process = pb.start();
        log("[Launch] PID do processo filho: " + process.pid());
        logWriter.flush();

        int exitCode = process.waitFor();
        log("[Launch] Processo encerrado com codigo: " + exitCode);
    }

    // -------------------------------------------------------------------------
    // Deteccao do executavel Java correto
    // -------------------------------------------------------------------------

    /**
     * No ambiente jpackage, ProcessHandle.current().info().command() retorna o caminho
     * do .exe nativo (MyFinance.exe), nao do java. Por isso priorizamos java.home.
     */
    private static String detectJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            // Prefere javaw.exe no Windows (sem janela de console extra)
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Path javaw = Path.of(javaHome, "bin", "javaw.exe");
                if (Files.exists(javaw)) {
                    log("[Java] Usando javaw.exe do java.home.");
                    return javaw.toString();
                }
                Path java = Path.of(javaHome, "bin", "java.exe");
                if (Files.exists(java)) {
                    log("[Java] Usando java.exe do java.home.");
                    return java.toString();
                }
            } else {
                Path java = Path.of(javaHome, "bin", "java");
                if (Files.exists(java)) return java.toString();
            }
        }
        // Fallback: ProcessHandle (funciona fora do jpackage)
        String cmd = ProcessHandle.current().info().command().orElse("java");
        if (cmd.endsWith(".exe") && !cmd.endsWith("java.exe") && !cmd.endsWith("javaw.exe")) {
            log("[Java] ProcessHandle retornou launcher nativo (" + cmd + "), usando 'java' generico.");
            return "java";
        }
        return cmd;
    }

    // -------------------------------------------------------------------------
    // Deteccao do diretorio do launcher.jar
    // -------------------------------------------------------------------------

    private static Path detectLauncherDir() throws Exception {
        URI location = LauncherApp.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI();
        Path jarPath = Path.of(location).toAbsolutePath();
        log("[Path] launcher.jar localizado em: " + jarPath);
        return Files.isDirectory(jarPath) ? jarPath : jarPath.getParent();
    }

    // -------------------------------------------------------------------------
    // Logging para arquivo
    // -------------------------------------------------------------------------

    private static String logFilePath() {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        return appData + File.separator + "MyFinance" + File.separator + "launcher.log";
    }

    private static void initLog() {
        try {
            Path logPath = Path.of(logFilePath());
            Files.createDirectories(logPath.getParent());
            logWriter = new PrintWriter(new FileWriter(logPath.toFile(), true), true);
        } catch (Exception e) {
            logWriter = new PrintWriter(System.out, true);
        }
    }

    private static void log(String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + msg;
        System.out.println(line);
        if (logWriter != null) {
            logWriter.println(line);
            logWriter.flush();
        }
    }

    private static void showErrorDialog(String message) {
        try {
            Class<?> jOptionPane = Class.forName("javax.swing.JOptionPane");
            jOptionPane.getMethod("showMessageDialog",
                    Object.class, Object.class, String.class, int.class)
                    .invoke(null, null, message, "MyFinance - Erro de Inicializacao", 0);
        } catch (Exception ignored) {
            // Se Swing nao estiver disponivel, o erro ja esta no log
        }
    }

    // -------------------------------------------------------------------------
    // Utilitarios
    // -------------------------------------------------------------------------

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = LauncherApp.class.getResourceAsStream("/launcher.properties")) {
            if (is != null) props.load(is);
        }
        return props;
    }
}

