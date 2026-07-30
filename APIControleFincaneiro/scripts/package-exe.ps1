# package-exe.ps1
# Gera o instalador .exe do MyFinance usando jpackage (JDK 21+).
#
# Pré-requisitos:
#   - JDK 21+ no PATH (com suporte a jpackage)
#   - WiX Toolset instalado (para gerar .exe no Windows)
#     https://wixtoolset.org/releases/
#   - Maven no PATH
#
# Uso:
#   cd scripts
#   .\package-exe.ps1
#
$ErrorActionPreference = "Stop"

$ROOT         = Split-Path -Parent $PSScriptRoot
$BACKEND_DIR  = Join-Path $ROOT "back-end"
$LAUNCHER_DIR = Join-Path $ROOT "launcher"
$DIST_DIR     = Join-Path $ROOT "dist"
$INPUT_DIR    = Join-Path $DIST_DIR "input"
$APP_DIR      = Join-Path $INPUT_DIR "app"

Write-Host "=== MyFinance - Empacotamento .exe ===" -ForegroundColor Cyan

# 1. Build do back-end (fat JAR)
Write-Host ""
Write-Host "[1/4] Compilando back-end..." -ForegroundColor Yellow
Push-Location $BACKEND_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build do back-end." }
Pop-Location

# 2. Build do launcher
Write-Host ""
Write-Host "[2/4] Compilando launcher..." -ForegroundColor Yellow
Push-Location $LAUNCHER_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build do launcher." }
Pop-Location

# 3. Monta a estrutura de entrada para o jpackage
Write-Host ""
Write-Host "[3/4] Montando estrutura de distribuicao..." -ForegroundColor Yellow

if (Test-Path $DIST_DIR) { Remove-Item $DIST_DIR -Recurse -Force }
New-Item -ItemType Directory -Path $APP_DIR -Force | Out-Null

$LAUNCHER_JAR = Get-ChildItem "$LAUNCHER_DIR\target" -Filter "launcher.jar" | Select-Object -First 1
if (-not $LAUNCHER_JAR) { throw "launcher.jar nao encontrado. Verifique o build do launcher." }
Copy-Item $LAUNCHER_JAR.FullName "$INPUT_DIR\launcher.jar"

$BACKEND_JAR = Get-ChildItem "$BACKEND_DIR\target" -Filter "*.jar" |
               Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
               Select-Object -First 1
if (-not $BACKEND_JAR) { throw "back-end JAR nao encontrado. Verifique o build do back-end." }
Copy-Item $BACKEND_JAR.FullName "$APP_DIR\back-end.jar"

Write-Host "  Launcher : $($LAUNCHER_JAR.Name)"
Write-Host "  App JAR  : $($BACKEND_JAR.Name) -> app\back-end.jar"

# 4. jpackage
Write-Host ""
Write-Host "[4/4] Gerando instalador com jpackage..." -ForegroundColor Yellow

$OUTPUT = Join-Path $ROOT "instalador"
if (Test-Path $OUTPUT) { Remove-Item $OUTPUT -Recurse -Force }
New-Item -ItemType Directory -Path $OUTPUT -Force | Out-Null

$ICON_PATH = Join-Path $BACKEND_DIR "src\main\resources\static\assets\glaceonIcon .png"

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

if (Test-Path $ICON_PATH) {
    Write-Host "  Aviso: icone encontrado como .png. jpackage requer .ico no Windows."
    Write-Host "  Para usar icone personalizado, converta para .ico e ajuste --icon no script."
} else {
    Write-Host "  Icone nao encontrado - usando padrao do sistema."
}

& jpackage @jpArgs

if ($LASTEXITCODE -ne 0) { throw "jpackage falhou. Verifique se o JDK 21+ e WiX Toolset estao instalados." }

Write-Host ""
Write-Host "Instalador gerado em: $OUTPUT" -ForegroundColor Green
Write-Host "Distribua o arquivo MyFinance-1.0.0.exe para os usuarios." -ForegroundColor Green
