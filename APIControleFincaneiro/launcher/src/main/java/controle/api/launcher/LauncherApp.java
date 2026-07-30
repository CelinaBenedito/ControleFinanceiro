package controle.api.launcher;

import org.update4j.Configuration;
import org.update4j.UpdateHandler;
import org.update4j.handler.DefaultUpdateHandler;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

/**
 * Bootstrap do MyFinance.
 *
 * Fluxo:
 *  1. Lê launcher.properties para obter a URL do config Update4j no GitHub.
 *  2. Baixa o config XML.
 *  3. Se algum arquivo divergir do checksum → atualiza via Update4j.
 *  4. Inicia o back-end.jar como processo separado (evita conflitos de ClassLoader com Spring Boot).
 *
 * Este JAR é o que fica dentro do .exe gerado pelo jpackage.
 */
public class LauncherApp {

    public static void main(String[] args) throws Exception {
        Properties props = loadProperties();

        String configUrl = props.getProperty("update4j.config.url");
        String appDir    = props.getProperty("app.dir", "app");
        String appJar    = props.getProperty("app.jar.name", "back-end.jar");

        Path appJarPath = Path.of(appDir, appJar).toAbsolutePath();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       MyFinance Launcher v1.0.0          ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("[Launcher] JAR da aplicação: " + appJarPath);

        // Garante que o diretório da aplicação existe
        Files.createDirectories(appJarPath.getParent());

        // Tenta verificar e aplicar atualizações via Update4j
        boolean updated = tryUpdate(configUrl, appJarPath);
        if (updated) {
            System.out.println("[Launcher] Atualização aplicada com sucesso.");
        }

        // Inicia a aplicação principal
        launchApp(appJarPath, args);
    }

    // -------------------------------------------------------------------------
    // Update4j
    // -------------------------------------------------------------------------

    private static boolean tryUpdate(String configUrl, Path appJarPath) {
        if (configUrl == null || configUrl.isBlank() || configUrl.contains("SEU_USUARIO")) {
            System.out.println("[Launcher] URL do config não configurada — pulando verificação de atualização.");
            return false;
        }

        try {
            System.out.println("[Launcher] Verificando atualizações em: " + configUrl);

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .header("User-Agent", "MyFinance-Launcher/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("[Launcher] Config não disponível (HTTP " + response.statusCode() + "). Iniciando sem atualizar.");
                return false;
            }

            Configuration config;
            try (Reader reader = new StringReader(response.body())) {
                config = Configuration.read(reader);
            }

            if (!config.requiresUpdate()) {
                System.out.println("[Launcher] Aplicação já está atualizada.");
                return false;
            }

            System.out.println("[Launcher] Nova versão encontrada! Baixando atualização...");
            config.update(new DefaultUpdateHandler() {
                @Override
                public void updateDownloadFileProgress(org.update4j.FileMetadata file, float frac) {
                    int pct = (int) (frac * 100);
                    System.out.printf("\r[Launcher] Baixando %s... %d%%  ", file.getPath().getFileName(), pct);
                }
                @Override
                public void doneDownloadFile(org.update4j.FileMetadata file, Path path) {
                    System.out.println("\r[Launcher] ✓ " + file.getPath().getFileName() + " baixado.");
                }
            });

            return true;

        } catch (Exception e) {
            System.out.println("[Launcher] Aviso: não foi possível verificar atualizações: " + e.getMessage());
            System.out.println("[Launcher] Continuando com a versão atual...");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Inicialização da aplicação principal
    // -------------------------------------------------------------------------

    private static void launchApp(Path appJarPath, String[] args) throws Exception {
        if (!Files.exists(appJarPath)) {
            System.err.println("[Launcher] ERRO: JAR não encontrado em " + appJarPath);
            System.err.println("[Launcher] Certifique-se de que '" + appJarPath.getFileName() + "' está na pasta '" + appJarPath.getParent() + "'.");
            System.exit(1);
        }

        // Usa o mesmo executável Java desta JVM (garante compatibilidade com jpackage)
        String javaExe = ProcessHandle.current()
                .info()
                .command()
                .orElse("java");

        List<String> command = new ArrayList<>();
        command.add(javaExe);
        // Repassa argumentos JVM adicionais passados ao launcher (ex: -Xmx)
        for (String arg : args) {
            if (arg.startsWith("-X") || arg.startsWith("-D")) {
                command.add(arg);
            }
        }
        command.add("-jar");
        command.add(appJarPath.toString());
        // Repassa argumentos de aplicação
        for (String arg : args) {
            if (!arg.startsWith("-X") && !arg.startsWith("-D")) {
                command.add(arg);
            }
        }

        System.out.println("[Launcher] Iniciando: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();

        // Aguarda a aplicação encerrar e propaga o código de saída
        int exitCode = process.waitFor();
        System.exit(exitCode);
    }

    // -------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = LauncherApp.class.getResourceAsStream("/launcher.properties")) {
            if (is != null) {
                props.load(is);
            }
        }
        return props;
    }
}

