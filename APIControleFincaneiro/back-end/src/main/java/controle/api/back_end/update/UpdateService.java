package controle.api.back_end.update;

import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.regex.*;

@Service
public class UpdateService {

    @Value("${app.update.github.owner}")
    private String githubOwner;

    @Value("${app.update.github.repo}")
    private String githubRepo;

    @Value("${app.update.jar.asset-name:back-end.jar}")
    private String assetName;

    @Value("${app.update.enabled:true}")
    private boolean updateEnabled;

    private final String currentVersion;

    public UpdateService() {
        this.currentVersion = loadCurrentVersion();
    }

    // -------------------------------------------------------------------------
    // Versão atual
    // -------------------------------------------------------------------------

    private String loadCurrentVersion() {
        try (InputStream is = getClass().getResourceAsStream("/version.properties")) {
            if (is == null) return "0.0.0";
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("app.version", "0.0.0");
        } catch (IOException e) {
            return "0.0.0";
        }
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    // -------------------------------------------------------------------------
    // Verificação de atualização via GitHub Releases API
    // -------------------------------------------------------------------------

    public UpdateInfo checkForUpdate() throws Exception {
        if (!updateEnabled) {
            return new UpdateInfo(false, currentVersion, currentVersion, null, null);
        }

        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/releases/latest",
                githubOwner, githubRepo
        );

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MyFinance-AutoUpdate/1.0")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return new UpdateInfo(false, currentVersion, currentVersion, null, null);
        }

        String body = response.body();
        String latestTag     = extractJsonString(body, "tag_name");
        String latestVersion = latestTag.replaceFirst("^v", "");
        String releaseUrl    = extractJsonString(body, "html_url");
        String downloadUrl   = extractAssetDownloadUrl(body, assetName);

        boolean hasUpdate = isNewerVersion(latestVersion, currentVersion);
        return new UpdateInfo(hasUpdate, currentVersion, latestVersion, releaseUrl, downloadUrl);
    }

    // -------------------------------------------------------------------------
    // Aplicação da atualização
    // -------------------------------------------------------------------------

    /**
     * Baixa o novo JAR, gera um script de reinicialização e encerra a aplicação.
     * O script aguarda o processo morrer, substitui o JAR e reinicia.
     */
    public void applyUpdate(String downloadUrl) throws Exception {
        Path currentJar  = detectCurrentJar();
        Path parentDir   = currentJar.getParent() != null ? currentJar.getParent() : Path.of(".");
        Path newJar      = parentDir.resolve("back-end-new.jar");

        System.out.println("[Update] Baixando nova versão para: " + newJar);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "MyFinance-AutoUpdate/1.0")
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(newJar));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(newJar);
            throw new RuntimeException("Falha no download: HTTP " + response.statusCode());
        }

        System.out.println("[Update] Download concluído. Gerando script de reinicialização.");

        // Detecta o executável Java desta JVM (funciona tanto com JDK avulso quanto jpackage)
        String javaExe = ProcessHandle.current()
                .info()
                .command()
                .orElse("javaw");

        Path restartScript = parentDir.resolve("myfinance-restart.bat");
        String bat = "@echo off\r\n"
                + "echo [MyFinance Updater] Aguardando o fechamento da aplicacao...\r\n"
                + "timeout /t 6 /nobreak > nul\r\n"
                + "if exist \"" + currentJar.toAbsolutePath() + ".bak\" del /f \"" + currentJar.toAbsolutePath() + ".bak\"\r\n"
                + "ren \"" + currentJar.toAbsolutePath() + "\" \"" + currentJar.getFileName() + ".bak\"\r\n"
                + "ren \"" + newJar.toAbsolutePath() + "\" \"" + currentJar.getFileName() + "\"\r\n"
                + "echo [MyFinance Updater] Reiniciando...\r\n"
                + "start \"\" \"" + javaExe + "\" -jar \"" + currentJar.toAbsolutePath() + "\"\r\n"
                + "del \"%~f0\"\r\n";  // auto-remove script

        Files.writeString(restartScript, bat);

        // Executa o script minimizado em segundo plano
        new ProcessBuilder("cmd.exe", "/c", "start", "/min", "MyFinance Updater", restartScript.toAbsolutePath().toString())
                .start();

        System.out.println("[Update] Script de reinicialização iniciado. Encerrando aplicação.");

        // Encerra o JavaFX e a JVM após um breve delay
        Platform.exit();
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            System.exit(0);
        }).start();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Tenta detectar o JAR que está sendo executado atualmente.
     * Fallback: ~/.myfinance/back-end.jar
     */
    private Path detectCurrentJar() {
        try {
            URI location = UpdateService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            Path path = Path.of(location);
            if (path.toString().endsWith(".jar")) {
                return path.toAbsolutePath();
            }
        } catch (Exception ignored) {}

        // Fallback para ambiente de desenvolvimento / instalação jpackage
        Path home = Path.of(System.getProperty("user.home"));
        return home.resolve(".myfinance").resolve("back-end.jar");
    }

    /** Retorna true se {@code latest} é mais recente que {@code current}. */
    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] lp = latest.split("[.\\-]");
            String[] cp = current.split("[.\\-]");
            int len = Math.max(lp.length, cp.length);
            for (int i = 0; i < len; i++) {
                int l = i < lp.length ? Integer.parseInt(lp[i].replaceAll("[^0-9]", "0")) : 0;
                int c = i < cp.length ? Integer.parseInt(cp[i].replaceAll("[^0-9]", "0")) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"\\\\]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    /**
     * Procura o browser_download_url do asset com o nome informado dentro
     * do array "assets" do JSON de release do GitHub.
     */
    private String extractAssetDownloadUrl(String json, String targetAssetName) {
        // Localiza o bloco do asset pelo name
        int nameIdx = json.indexOf("\"" + targetAssetName + "\"");
        if (nameIdx < 0) return "";

        // Vai um pouco para trás para pegar o início do objeto do asset
        int objectStart = json.lastIndexOf("{", nameIdx);
        if (objectStart < 0) objectStart = nameIdx;

        // Pega o próximo bloco de objeto (até a próxima ocorrência de "name": após nameIdx)
        int nextName = json.indexOf("\"name\"", nameIdx + 1);
        String segment = nextName > 0
                ? json.substring(objectStart, nextName)
                : json.substring(objectStart);

        Pattern p = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(segment);
        return m.find() ? m.group(1) : "";
    }
}

