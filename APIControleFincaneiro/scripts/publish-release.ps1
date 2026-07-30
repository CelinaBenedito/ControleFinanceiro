# ==============================================================================
# publish-release.ps1
# Publica um novo release no GitHub com:
#   - O back-end.jar como asset para download pela aplicação
#   - O update4j-config.xml atualizado no repositório (para o launcher)
#
# Pré-requisitos:
#   - GitHub CLI (gh) instalado e autenticado: https://cli.github.com
#   - Maven no PATH
#   - Git configurado
#
# Uso:
#   .\publish-release.ps1 -Version "1.1.0" -Notes "Descrição do que mudou"
# ==============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$Version,

    [string]$Notes = "Nova versão do MyFinance"
)

$ErrorActionPreference = "Stop"

$ROOT        = Split-Path -Parent $PSScriptRoot
$BACKEND_DIR = Join-Path $ROOT "back-end"
$TAG         = "v$Version"

Write-Host "=== MyFinance — Publicando Release $TAG ===" -ForegroundColor Cyan

# ── 1. Atualiza version.properties ──────────────────────────────────────────
Write-Host "`n[1/5] Atualizando version.properties para $Version..." -ForegroundColor Yellow
$versionFile = Join-Path $BACKEND_DIR "src\main\resources\version.properties"
Set-Content -Path $versionFile -Value "app.version=$Version"

# ── 2. Build do back-end ────────────────────────────────────────────────────
Write-Host "`n[2/5] Compilando back-end..." -ForegroundColor Yellow
Push-Location $BACKEND_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build." }
Pop-Location

$JAR = Get-ChildItem "$BACKEND_DIR\target" -Filter "*.jar" |
       Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
       Select-Object -First 1

if (-not $JAR) { throw "JAR não encontrado em $BACKEND_DIR\target" }

$JAR_PATH = $JAR.FullName
$JAR_SIZE = (Get-Item $JAR_PATH).Length

Write-Host "  JAR: $($JAR.Name) ($([math]::Round($JAR_SIZE/1MB, 2)) MB)"

# ── 3. Calcula checksum SHA-256 para o Update4j config ──────────────────────
Write-Host "`n[3/5] Calculando checksum SHA-256..." -ForegroundColor Yellow
$hash = (Get-FileHash -Path $JAR_PATH -Algorithm SHA256).Hash.ToLower()
Write-Host "  SHA-256: $hash"

# ── 4. Gera update4j-config.xml ─────────────────────────────────────────────
Write-Host "`n[4/5] Gerando update4j-config.xml..." -ForegroundColor Yellow

# Lê owner/repo do application.properties
$appProps = Get-Content "$BACKEND_DIR\src\main\resources\application.properties"
$owner = ($appProps | Select-String "app.update.github.owner=(.+)").Matches.Groups[1].Value.Trim()
$repo  = ($appProps | Select-String "app.update.github.repo=(.+)").Matches.Groups[1].Value.Trim()

if ($owner -eq "SEU_USUARIO_GITHUB" -or $repo -eq "SEU_REPOSITORIO") {
    Write-Host "  AVISO: Configure app.update.github.owner e app.update.github.repo em application.properties!" -ForegroundColor Red
    throw "GitHub owner/repo não configurados."
}

$downloadBase = "https://github.com/$owner/$repo/releases/download/$TAG"
$configPath   = Join-Path $ROOT "update4j-config.xml"
$timestamp    = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")

$xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<!--
    Arquivo de configuração do Update4j — gerado automaticamente por publish-release.ps1
    Versão: $Version | Data: $timestamp
    NÃO edite manualmente. Use publish-release.ps1 para gerar uma nova versão.
-->
<configuration timestamp="$timestamp">

    <!--
        base uri  : URL base de onde os arquivos serão baixados
        base path : diretório local onde os arquivos serão salvos
                    (relativo ao diretório do launcher)
    -->
    <base uri="$downloadBase/"
          path="app/" />

    <properties>
        <property key="app.version" value="$Version" />
    </properties>

    <files>
        <file path="back-end.jar"
              uri="back-end.jar"
              size="$JAR_SIZE"
              checksum="$hash"
              os="ANY" />
    </files>

</configuration>
"@

Set-Content -Path $configPath -Value $xml -Encoding UTF8
Write-Host "  Config salvo em: $configPath"

# ── 5. Commit, push e cria o release no GitHub ──────────────────────────────
Write-Host "`n[5/5] Publicando no GitHub..." -ForegroundColor Yellow

Push-Location $ROOT

# Renomeia o JAR para back-end.jar (nome fixo no release, independente da versão Maven)
$RELEASE_JAR = Join-Path $BACKEND_DIR "target\back-end.jar"
Copy-Item $JAR_PATH $RELEASE_JAR -Force

# Commit do config atualizado
& git add "update4j-config.xml" "back-end/src/main/resources/version.properties"
& git commit -m "chore: release $TAG — atualiza version.properties e update4j-config.xml"
& git push origin HEAD

# Cria o release com o JAR como asset
& gh release create $TAG `
    "$RELEASE_JAR#back-end.jar" `
    --title "MyFinance $TAG" `
    --notes $Notes `
    --latest

if ($LASTEXITCODE -ne 0) { throw "Falha ao criar o release no GitHub." }

Pop-Location

Write-Host "`n✅ Release $TAG publicado com sucesso!" -ForegroundColor Green
Write-Host "   Os usuários receberão a notificação de atualização automaticamente." -ForegroundColor Green
Write-Host "   Download URL: $downloadBase/back-end.jar" -ForegroundColor DarkGray

