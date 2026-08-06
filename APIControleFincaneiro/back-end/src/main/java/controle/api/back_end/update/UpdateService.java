package controle.api.back_end.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private static final int RELEASE_NOTES_MAX_LENGTH = 3500;
    private static final long MIN_JAR_SIZE = 10 * 1024 * 1024; // 10 MB mínimo

    @Value("${app.update.github.owner}")
    private String githubOwner;

    @Value("${app.update.github.repo}")
    private String githubRepo;

    @Value("${app.update.jar.asset-name:back-end.jar}")
    private String assetName;

    @Value("${app.update.enabled:true}")
    private boolean updateEnabled;

    @Value("${app.version:0.0.0}")
    private String currentVersion;

    public String getCurrentVersion() {
        return currentVersion;
    }

    // -------------------------------------------------------------------------
    // Verificação de atualização via GitHub Releases API
    // -------------------------------------------------------------------------

    public UpdateInfo checkForUpdate() throws Exception {
        if (!updateEnabled) {
            log.debug("[Update] Verificação de atualizações desabilitada");
            return new UpdateInfo(false, currentVersion, currentVersion, null, null, null);
        }

        log.info("[Update] Verificando atualizações... Versão atual: {}", currentVersion);

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
            log.warn("[Update] GitHub API retornou status {}", response.statusCode());
            return new UpdateInfo(false, currentVersion, currentVersion, null, null, null);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode release = mapper.readTree(response.body());

        String latestTag     = release.path("tag_name").asText("");
        String latestVersion = latestTag.replaceFirst("^v", "");
        String releaseNotes  = normalizeReleaseNotes(release.path("body").asText(""));
        String releaseUrl    = release.path("html_url").asText("");
        String downloadUrl   = extractAssetDownloadUrl(release.path("assets"), assetName);

        boolean hasUpdate = isNewerVersion(latestVersion, currentVersion);

        if (hasUpdate) {
            log.info("[Update] ✓ Nova versão disponível: {} -> {}", currentVersion, latestVersion);
        } else {
            log.info("[Update] Sistema atualizado. Versão mais recente: {}", currentVersion);
        }

        return new UpdateInfo(hasUpdate, currentVersion, latestVersion, releaseNotes, releaseUrl, downloadUrl);
    }

    // -------------------------------------------------------------------------
    // Aplicação da atualização
    // -------------------------------------------------------------------------

    /**
     * Estratégia de atualização sem janela CMD e sem conflito de lock de arquivo:
     *
     * 1. Baixa o novo JAR em %APPDATA%\MyFinance\pending-update.jar
     *    (local gravável, fora da pasta de instalação)
     * 2. Lança MyFinance.exe como novo processo (o launcher verifica e aplica
     *    o pending-update.jar ANTES de iniciar o back-end, portanto sem lock)
     * 3. Encerra esta instância normalmente
     *
     * O launcher (LauncherApp) é responsável por mover pending-update.jar → back-end.jar.
     */
    public void applyUpdate(String downloadUrl) throws Exception {
        log.info("[Update] ========================================");
        log.info("[Update] Iniciando processo de atualização");
        log.info("[Update] URL de download: {}", downloadUrl);
        log.info("[Update] ========================================");

        // Usa user.home/.myfinance/ — mesmo diretório base do banco de dados e uploads.
        // Mais confiável que %APPDATA% que pode variar entre processos no Windows.
        Path myfinanceDir = java.nio.file.Path.of(System.getProperty("user.home"), ".myfinance");
        Files.createDirectories(myfinanceDir);
        Path pendingJar = myfinanceDir.resolve("pending-update.jar");
        Path updateLog = myfinanceDir.resolve("update.log");

        // Remove atualização pendente anterior (se houver)
        if (Files.exists(pendingJar)) {
            log.warn("[Update] Encontrado pending-update.jar antigo. Removendo...");
            try {
                Files.delete(pendingJar);
                log.info("[Update] Arquivo antigo removido com sucesso");
            } catch (Exception e) {
                log.error("[Update] Erro ao remover arquivo antigo: {}", e.getMessage());
            }
        }

        log.info("[Update] Baixando nova versão para: {}", pendingJar);
        logToFile(updateLog, "=== Atualização MyFinance ===");
        logToFile(updateLog, "Data/Hora: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        logToFile(updateLog, "URL: " + downloadUrl);
        logToFile(updateLog, "Destino: " + pendingJar);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "MyFinance-AutoUpdate/1.0")
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(pendingJar));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorMsg = "Falha no download: HTTP " + response.statusCode();
            log.error("[Update] {}", errorMsg);
            logToFile(updateLog, "ERRO: " + errorMsg);
            Files.deleteIfExists(pendingJar);
            throw new RuntimeException(errorMsg);
        }

        // Valida que o arquivo baixado tem conteudo real
        long tamanho = Files.size(pendingJar);
        log.info("[Update] Download concluído. Tamanho: {} MB ({} bytes)",
                 String.format("%.2f", tamanho / 1048576.0), tamanho);
        logToFile(updateLog, "Download concluído: " + String.format("%.2f MB", tamanho / 1048576.0));

        if (tamanho < MIN_JAR_SIZE) {
            String errorMsg = "Arquivo baixado inválido ou incompleto (tamanho: " + tamanho + " bytes, mínimo: " + MIN_JAR_SIZE + " bytes)";
            log.error("[Update] {}", errorMsg);
            logToFile(updateLog, "ERRO: " + errorMsg);
            Files.deleteIfExists(pendingJar);
            throw new RuntimeException(errorMsg);
        }

        log.info("[Update] Validação de integridade: OK");
        logToFile(updateLog, "Validação: OK");
        logToFile(updateLog, "Agendando reinicialização...");

        // Agenda o reinicio via VBScript (silencioso, sem janela CMD, processo independente).
        // O VBScript realiza a cópia do pending-update.jar → back-end.jar com elevation automática
        // via PowerShell, evitando AccessDeniedException em instalações dentro de C:\Program Files\.
        Path exePath = detectMyFinanceExe();
        if (exePath != null) {
            log.info("[Update] MyFinance.exe localizado em: {}", exePath);
            Path vbsPath = myfinanceDir.resolve("restart.vbs");
            String exe = exePath.toString();
            // Determina o destino do JAR a partir da propriedade injetada pelo launcher
            String jarDest = System.getProperty("myfinance.jar.path");
            if (jarDest == null || jarDest.isBlank()) {
                // Fallback: infere a partir do caminho do exe  (<install>/MyFinance.exe → <install>/app/app/back-end.jar)
                jarDest = exePath.getParent().resolve("app").resolve("app").resolve("back-end.jar").toString();
                log.warn("[Update] Propriedade myfinance.jar.path não definida. Usando fallback: {}", jarDest);
            } else {
                log.info("[Update] Destino do JAR: {}", jarDest);
            }

            String src  = pendingJar.toString().replace("'", "''");
            String dst  = jarDest.replace("'", "''");
            String exeQ = exe.replace("\"", "\\\"");
            String logPath = updateLog.toString().replace("'", "''");

            // Estratégia:
            // 1. Tenta copiar diretamente (funciona se o usuário tem permissão).
            // 2. Se falhar, usa PowerShell com "Run As" para elevar e copiar.
            // 3. Após cópia bem-sucedida, apaga o pending-update.jar para que o
            //    launcher não tente copiá-lo novamente (e falhe com AccessDenied).
            // 4. Aguarda 8 segundos para garantir que a instância antiga encerrou completamente
            //    (liberar lock de porta 13308 e fechar conexões do banco).
            // 5. Verifica se a porta 13308 foi liberada (lock de instância única).
            // 6. Reinicia MyFinance.exe.
            String vbs = "WScript.Sleep 8000\r\n"
                    + "Dim fso, src, dst, logFile\r\n"
                    + "src = \"" + src.replace("\"", "\"\"") + "\"\r\n"
                    + "dst = \"" + dst.replace("\"", "\"\"") + "\"\r\n"
                    + "logFile = \"" + logPath.replace("\"", "\"\"") + "\"\r\n"
                    + "Set fso = CreateObject(\"Scripting.FileSystemObject\")\r\n"
                    + "Sub LogMsg(msg)\r\n"
                    + "    On Error Resume Next\r\n"
                    + "    Dim f : Set f = fso.OpenTextFile(logFile, 8, True)\r\n"
                    + "    f.WriteLine msg\r\n"
                    + "    f.Close\r\n"
                    + "End Sub\r\n"
                    + "' Verifica se a porta 13308 foi liberada (lock de instância única)\r\n"
                    + "Function IsPortFree()\r\n"
                    + "    On Error Resume Next\r\n"
                    + "    Dim objWMI, colItems, objItem\r\n"
                    + "    Set objWMI = GetObject(\"winmgmts:\\\\\\\\localhost\\\\root\\\\CIMV2\")\r\n"
                    + "    Set colItems = objWMI.ExecQuery(\"SELECT * FROM Win32_Process WHERE Name = 'java.exe' OR Name = 'javaw.exe'\")\r\n"
                    + "    IsPortFree = (colItems.Count = 0)\r\n"
                    + "    On Error GoTo 0\r\n"
                    + "End Function\r\n"
                    + "' Aguarda até 30 segundos para a instância antiga encerrar\r\n"
                    + "Dim tentativas : tentativas = 0\r\n"
                    + "While tentativas < 15 And Not IsPortFree()\r\n"
                    + "    WScript.Sleep 2000\r\n"
                    + "    tentativas = tentativas + 1\r\n"
                    + "Wend\r\n"
                    + "If tentativas >= 15 Then\r\n"
                    + "    LogMsg \"[VBScript] AVISO: Timeout aguardando encerramento da instância anterior\"\r\n"
                    + "Else\r\n"
                    + "    LogMsg \"[VBScript] Instância anterior encerrada com sucesso\"\r\n"
                    + "End If\r\n"
                    + "LogMsg \"[VBScript] Iniciando aplicação da atualização...\"\r\n"
                    + "If fso.FileExists(src) Then\r\n"
                    + "    On Error Resume Next\r\n"
                    + "    fso.CopyFile src, dst, True\r\n"
                    + "    Dim copyErr : copyErr = Err.Number\r\n"
                    + "    On Error GoTo 0\r\n"
                    + "    If copyErr <> 0 Then\r\n"
                    + "        LogMsg \"[VBScript] Cópia direta falhou (erro \" & copyErr & \"). Tentando com elevação...\"\r\n"
                    + "        ' Copia falhou (sem permissao) — eleva via PowerShell\r\n"
                    + "        Dim psCmd\r\n"
                    + "        psCmd = \"Copy-Item -LiteralPath '" + src + "' -Destination '" + dst + "' -Force; Remove-Item -LiteralPath '" + src + "' -Force\"\r\n"
                    + "        CreateObject(\"Shell.Application\").ShellExecute \"powershell.exe\", \"-NonInteractive -NoProfile -Command \" & Chr(34) & psCmd & Chr(34), \"\", \"runas\", 0\r\n"
                    + "        LogMsg \"[VBScript] PowerShell elevado executado. Aguardando conclusão...\"\r\n"
                    + "        WScript.Sleep 10000\r\n"
                    + "    Else\r\n"
                    + "        LogMsg \"[VBScript] Cópia direta bem-sucedida. Removendo arquivo temporário...\"\r\n"
                    + "        On Error Resume Next\r\n"
                    + "        fso.DeleteFile src, True\r\n"
                    + "        On Error GoTo 0\r\n"
                    + "        LogMsg \"[VBScript] Atualização aplicada com sucesso!\"\r\n"
                    + "    End If\r\n"
                    + "Else\r\n"
                    + "    LogMsg \"[VBScript] ERRO: Arquivo fonte não encontrado: \" & src\r\n"
                    + "End If\r\n"
                    + "LogMsg \"[VBScript] Reiniciando aplicação...\"\r\n"
                    + "CreateObject(\"WScript.Shell\").Run \"\"\"" + exeQ + "\"\"\", 1, False\r\n"
                    + "On Error Resume Next\r\n"
                    + "fso.DeleteFile WScript.ScriptFullName, True\r\n";

            Files.writeString(vbsPath, vbs, java.nio.charset.StandardCharsets.UTF_8);
            new ProcessBuilder("wscript.exe", "/nologo", vbsPath.toString()).start();

            log.info("[Update] Script VBS criado e executado: {}", vbsPath);
            log.info("[Update] Cópia pendente: {} -> {}", pendingJar, jarDest);
            logToFile(updateLog, "Script VBS executado");
            logToFile(updateLog, "Aguardando reinicialização...");
        } else {
            log.error("[Update] MyFinance.exe não localizado — usuário deverá reabrir manualmente");
            logToFile(updateLog, "ERRO: MyFinance.exe não localizado");
        }

        // Encerra esta instância para liberar o lock do JAR
        log.info("[Update] Encerrando instância atual para aplicar atualização...");
        log.info("[Update] ========================================");
        
        // Encerra o JavaFX imediatamente
        Platform.exit();
        
        // Thread agressiva para garantir o encerramento
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            log.info("[Update] Forçando encerramento da aplicação...");
            System.exit(0);
        }).start();
    }

    /**
     * Localiza MyFinance.exe usando duas estratégias:
     * 1. myfinance.jar.path (passado pelo launcher via -D):
     *    <install>/app/app/back-end.jar → subir 3 níveis → <install>/MyFinance.exe
     * 2. java.home (JRE bundlado pelo jpackage):
     *    <install>/runtime/ → subir 1 nível → <install>/MyFinance.exe
     */
    private Path detectMyFinanceExe() {
        // Estratégia 1: via propriedade myfinance.jar.path
        String jarPath = System.getProperty("myfinance.jar.path");
        if (jarPath != null && !jarPath.isBlank()) {
            try {
                Path candidate = Path.of(jarPath).getParent().getParent().getParent().resolve("MyFinance.exe");
                log.debug("[Update] Candidato exe (jar.path): {}", candidate);
                if (Files.exists(candidate)) {
                    log.info("[Update] MyFinance.exe encontrado via jar.path");
                    return candidate;
                }
            } catch (Exception e) {
                log.warn("[Update] Erro ao detectar exe via jar.path: {}", e.getMessage());
            }
        }

        // Estratégia 2: via java.home
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            try {
                Path candidate = Path.of(javaHome).getParent().resolve("MyFinance.exe");
                log.debug("[Update] Candidato exe (java.home): {}", candidate);
                if (Files.exists(candidate)) {
                    log.info("[Update] MyFinance.exe encontrado via java.home");
                    return candidate;
                }
            } catch (Exception e) {
                log.warn("[Update] Erro ao detectar exe via java.home: {}", e.getMessage());
            }
        }

        log.error("[Update] Não foi possível localizar MyFinance.exe");
        return null;
    }

    /**
     * Grava mensagem no arquivo de log de atualização.
     */
    private void logToFile(Path logFile, String message) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String line = "[" + timestamp + "] " + message + "\n";
            Files.writeString(logFile, line,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("[Update] Erro ao gravar no log de atualização: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------


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

    /**
     * Procura o browser_download_url do asset com o nome informado no array assets.
     */
    private String extractAssetDownloadUrl(JsonNode assetsNode, String targetAssetName) {
        if (assetsNode == null || !assetsNode.isArray()) return "";
        for (JsonNode asset : assetsNode) {
            if (targetAssetName.equals(asset.path("name").asText(""))) {
                return asset.path("browser_download_url").asText("");
            }
        }
        return "";
    }

    private String normalizeReleaseNotes(String notes) {
        if (notes == null) return "";
        String normalized = notes.replace("\r\n", "\n").replace("\r", "\n").trim();
        if (normalized.length() > RELEASE_NOTES_MAX_LENGTH) {
            return normalized.substring(0, RELEASE_NOTES_MAX_LENGTH) + "\n...";
        }
        return normalized;
    }
}
