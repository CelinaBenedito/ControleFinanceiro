# ==============================================================================
# package-exe.ps1
# Gera o instalador .exe do MyFinance usando jpackage (JDK 21+).
#
# Pré-requisitos:
#   - JDK 21+ no PATH (com suporte a jpackage)
#   - WiX Toolset instalado (para gerar .msi/.exe no Windows)
#     https://wixtoolset.org/releases/
#   - Maven no PATH
#
# Uso:
#   cd scripts
#   .\package-exe.ps1
# ==============================================================================

$ErrorActionPreference = "Stop"

$ROOT        = Split-Path -Parent $PSScriptRoot
$BACKEND_DIR = Join-Path $ROOT "back-end"
$LAUNCHER_DIR= Join-Path $ROOT "launcher"
$DIST_DIR    = Join-Path $ROOT "dist"
$INPUT_DIR   = Join-Path $DIST_DIR "input"
$APP_DIR     = Join-Path $INPUT_DIR "app"

Write-Host "=== MyFinance — Empacotamento .exe ===" -ForegroundColor Cyan

# ── 1. Build do back-end (fat JAR) ──────────────────────────────────────────
Write-Host "`n[1/4] Compilando back-end..." -ForegroundColor Yellow
Push-Location $BACKEND_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build do back-end." }
Pop-Location

# ── 2. Build do launcher ────────────────────────────────────────────────────
Write-Host "`n[2/4] Compilando launcher..." -ForegroundColor Yellow
Push-Location $LAUNCHER_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build do launcher." }
Pop-Location

# ── 3. Monta a estrutura de entrada para o jpackage ─────────────────────────
Write-Host "`n[3/4] Montando estrutura de distribuição..." -ForegroundColor Yellow

if (Test-Path $DIST_DIR) { Remove-Item $DIST_DIR -Recurse -Force }
New-Item -ItemType Directory -Path $APP_DIR -Force | Out-Null

# Launcher vai na raiz do input (será o ponto de entrada do .exe)
$LAUNCHER_JAR = Get-ChildItem "$LAUNCHER_DIR\target" -Filter "launcher.jar" | Select-Object -First 1
Copy-Item $LAUNCHER_JAR.FullName "$INPUT_DIR\launcher.jar"

# back-end.jar fica na subpasta app\ (o launcher sabe procurar lá)
$BACKEND_JAR = Get-ChildItem "$BACKEND_DIR\target" -Filter "*.jar" |
               Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
               Select-Object -First 1
Copy-Item $BACKEND_JAR.FullName "$APP_DIR\back-end.jar"

Write-Host "  Launcher : $($LAUNCHER_JAR.Name)"
Write-Host "  App JAR  : $($BACKEND_JAR.Name) -> app\back-end.jar"

# ── 4. jpackage ─────────────────────────────────────────────────────────────
Write-Host "`n[4/4] Gerando instalador com jpackage..." -ForegroundColor Yellow

$ICON = Join-Path $BACKEND_DIR "src\main\resources\static\assets\glaceonIcon .png"
$OUTPUT = Join-Path $ROOT "instalador"

$jpArgs = @(
    "--type", "exe",
    "--name", "MyFinance",
    "--app-version", "1.0.0",
    "--vendor", "MyFinance",
    "--description", "Controle Financeiro Pessoal",
    "--input", $INPUT_DIR,
    "--dest", $OUTPUT,
    "--main-jar", "launcher.jar",
    "--main-class", "controle.api.launcher.LauncherApp",
    "--win-dir-chooser",
    "--win-menu",
    "--win-shortcut",
    "--win-shortcut-prompt",
    "--java-options", "-Xmx512m"
)

# Adiciona ícone se existir
if (Test-Path $ICON) {
    # jpackage requer .ico no Windows — converta se necessário
    $ICO = Join-Path $DIST_DIR "icon.ico"
    Write-Host "  Aviso: converta o ícone para .ico manualmente e ajuste o script se desejar ícone personalizado."
} else {
    Write-Host "  Ícone não encontrado — usando padrão do sistema."
}

& jpackage @jpArgs

if ($LASTEXITCODE -ne 0) { throw "jpackage falhou. Verifique se o JDK 21+ e WiX estão instalados." }

Write-Host "`n✅ Instalador gerado em: $OUTPUT" -ForegroundColor Green
Write-Host "   Distribua o arquivo MyFinance-1.0.0.exe para os usuários." -ForegroundColor Green

