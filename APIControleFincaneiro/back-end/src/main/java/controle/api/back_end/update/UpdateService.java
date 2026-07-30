package controle.api.back_end.update;

import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
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
        // Usa user.home/.myfinance/ — mesmo diretório base do banco de dados e uploads.
        // Mais confiável que %APPDATA% que pode variar entre processos no Windows.
        Path myfinanceDir = java.nio.file.Path.of(System.getProperty("user.home"), ".myfinance");
        Files.createDirectories(myfinanceDir);
        Path pendingJar = myfinanceDir.resolve("pending-update.jar");

        System.out.println("[Update] Baixando nova versao para: " + pendingJar);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "MyFinance-AutoUpdate/1.0")
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(pendingJar));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(pendingJar);
            throw new RuntimeException("Falha no download: HTTP " + response.statusCode());
        }

        // Valida que o arquivo baixado tem conteudo real
        long tamanho = Files.size(pendingJar);
        if (tamanho < 1024) {
            Files.deleteIfExists(pendingJar);
            throw new RuntimeException("Arquivo baixado invalido (tamanho: " + tamanho + " bytes).");
        }

        System.out.println("[Update] Download concluido (" + (tamanho / 1048576) + " MB). Agendando reinicio...");

        // Agenda o reinicio via VBScript (silencioso, sem janela CMD, processo independente).
        // O VBScript realiza a cópia do pending-update.jar → back-end.jar com elevation automática
        // via PowerShell, evitando AccessDeniedException em instalações dentro de C:\Program Files\.
        Path exePath = detectMyFinanceExe();
        if (exePath != null) {
            Path vbsPath = myfinanceDir.resolve("restart.vbs");
            String exe = exePath.toString();
            // Determina o destino do JAR a partir da propriedade injetada pelo launcher
            String jarDest = System.getProperty("myfinance.jar.path");
            if (jarDest == null || jarDest.isBlank()) {
                // Fallback: infere a partir do caminho do exe  (<install>/MyFinance.exe → <install>/app/app/back-end.jar)
                jarDest = exePath.getParent().resolve("app").resolve("app").resolve("back-end.jar").toString();
            }
            String src  = pendingJar.toString().replace("'", "''");
            String dst  = jarDest.replace("'", "''");
            String exeQ = exe.replace("\"", "\\\"");

            // Estratégia:
            // 1. Tenta copiar diretamente (funciona se o usuário tem permissão).
            // 2. Se falhar, usa PowerShell com "Run As" para elevar e copiar.
            // 3. Após cópia bem-sucedida, apaga o pending-update.jar para que o
            //    launcher não tente copiá-lo novamente (e falhe com AccessDenied).
            // 4. Reinicia MyFinance.exe.
            String vbs = "WScript.Sleep 3000\r\n"
                    + "Dim fso, src, dst\r\n"
                    + "src = \"" + src.replace("\"", "\"\"") + "\"\r\n"
                    + "dst = \"" + dst.replace("\"", "\"\"") + "\"\r\n"
                    + "Set fso = CreateObject(\"Scripting.FileSystemObject\")\r\n"
                    + "If fso.FileExists(src) Then\r\n"
                    + "    On Error Resume Next\r\n"
                    + "    fso.CopyFile src, dst, True\r\n"
                    + "    Dim copyErr : copyErr = Err.Number\r\n"
                    + "    On Error GoTo 0\r\n"
                    + "    If copyErr <> 0 Then\r\n"
                    + "        ' Copia falhou (sem permissao) — eleva via PowerShell\r\n"
                    + "        Dim psCmd\r\n"
                    + "        psCmd = \"Copy-Item -LiteralPath '" + src + "' -Destination '" + dst + "' -Force; Remove-Item -LiteralPath '" + src + "' -Force\"\r\n"
                    + "        CreateObject(\"Shell.Application\").ShellExecute \"powershell.exe\", \"-NonInteractive -NoProfile -Command \" & Chr(34) & psCmd & Chr(34), \"\", \"runas\", 0\r\n"
                    + "        WScript.Sleep 10000\r\n"
                    + "    Else\r\n"
                    + "        On Error Resume Next\r\n"
                    + "        fso.DeleteFile src, True\r\n"
                    + "        On Error GoTo 0\r\n"
                    + "    End If\r\n"
                    + "End If\r\n"
                    + "CreateObject(\"WScript.Shell\").Run \"\"\"" + exeQ + "\"\"\", 1, False\r\n"
                    + "On Error Resume Next\r\n"
                    + "fso.DeleteFile WScript.ScriptFullName, True\r\n";
            Files.writeString(vbsPath, vbs, java.nio.charset.StandardCharsets.UTF_8);
            new ProcessBuilder("wscript.exe", "/nologo", vbsPath.toString()).start();
            System.out.println("[Update] Reinicio agendado via wscript para: " + exe);
            System.out.println("[Update] Copia pendente: " + pendingJar + " -> " + jarDest);
        } else {
            System.out.println("[Update] MyFinance.exe nao localizado — usuario devera reabrir manualmente.");
        }

        // Encerra esta instância para liberar o lock do JAR
        System.out.println("[Update] Encerrando instancia atual.");
        Platform.exit();
        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
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
                System.out.println("[Update] Candidato exe (jar.path): " + candidate);
                if (Files.exists(candidate)) return candidate;
            } catch (Exception ignored) {}
        }

        // Estratégia 2: via java.home
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            try {
                Path candidate = Path.of(javaHome).getParent().resolve("MyFinance.exe");
                System.out.println("[Update] Candidato exe (java.home): " + candidate);
                if (Files.exists(candidate)) return candidate;
            } catch (Exception ignored) {}
        }

        return null;
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
