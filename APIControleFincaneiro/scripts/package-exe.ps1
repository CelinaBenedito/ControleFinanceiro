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
$OUTPUT       = Join-Path $ROOT "instalador"

Write-Host "=== MyFinance - Empacotamento .exe ===" -ForegroundColor Cyan

# ----------------------------------------------------------
# Funcao: converte PNG para ICO usando .NET System.Drawing
# ----------------------------------------------------------
function Convert-PngToIco {
    param([string]$PngPath, [string]$IcoPath)

    Add-Type -AssemblyName System.Drawing

    $sizes = @(16, 32, 48, 256)
    $ms    = [System.IO.MemoryStream]::new()
    $bw    = [System.IO.BinaryWriter]::new($ms)

    $pngDataList = @()
    foreach ($sz in $sizes) {
        $bmp = [System.Drawing.Bitmap]::new($PngPath)
        $resized = [System.Drawing.Bitmap]::new($bmp, $sz, $sz)
        $imgStream = [System.IO.MemoryStream]::new()
        $resized.Save($imgStream, [System.Drawing.Imaging.ImageFormat]::Png)
        $pngDataList += ,($imgStream.ToArray())
        $resized.Dispose()
        $bmp.Dispose()
        $imgStream.Dispose()
    }

    # ICO header
    $bw.Write([uint16]0)                     # Reserved
    $bw.Write([uint16]1)                     # Type: 1 = ICO
    $bw.Write([uint16]$pngDataList.Count)    # Number of images

    # Calcula o offset inicial dos dados (header 6 + 16 bytes por entrada)
    $dataOffset = 6 + 16 * $pngDataList.Count

    for ($i = 0; $i -lt $pngDataList.Count; $i++) {
        $sz   = $sizes[$i]
        $data = $pngDataList[$i]
        $dim  = if ($sz -eq 256) { 0 } else { $sz }   # 256 = 0 no formato ICO
        $bw.Write([byte]$dim)      # Width
        $bw.Write([byte]$dim)      # Height
        $bw.Write([byte]0)         # Color count
        $bw.Write([byte]0)         # Reserved
        $bw.Write([uint16]1)       # Color planes
        $bw.Write([uint16]32)      # Bits per pixel
        $bw.Write([uint32]$data.Length)
        $bw.Write([uint32]$dataOffset)
        $dataOffset += $data.Length
    }

    # Dados das imagens
    foreach ($data in $pngDataList) {
        $bw.Write($data)
    }

    $bw.Flush()
    [System.IO.File]::WriteAllBytes($IcoPath, $ms.ToArray())
    $bw.Dispose()
    $ms.Dispose()
}

# ----------------------------------------------------------
# 1. Build do back-end
# ----------------------------------------------------------
Write-Host ""
Write-Host "[1/4] Compilando back-end..." -ForegroundColor Yellow
Push-Location $BACKEND_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build do back-end." }
Pop-Location

# ----------------------------------------------------------
# 2. Build do launcher
# ----------------------------------------------------------
Write-Host ""
Write-Host "[2/4] Compilando launcher..." -ForegroundColor Yellow
Push-Location $LAUNCHER_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build do launcher." }
Pop-Location

# ----------------------------------------------------------
# 3. Monta a estrutura de entrada para o jpackage
#
# Estrutura resultante no diretorio de instalacao:
#   <install>/app/launcher.jar    <- ponto de entrada
#   <install>/app/app/back-end.jar <- aplicacao principal
# ----------------------------------------------------------
Write-Host ""
Write-Host "[3/4] Montando estrutura de distribuicao..." -ForegroundColor Yellow

if (Test-Path $DIST_DIR)   { Remove-Item $DIST_DIR   -Recurse -Force }
if (Test-Path $OUTPUT)     { Remove-Item $OUTPUT     -Recurse -Force }

$APP_SUBDIR = Join-Path $INPUT_DIR "app"
New-Item -ItemType Directory -Path $APP_SUBDIR -Force | Out-Null

$LAUNCHER_JAR = Get-ChildItem "$LAUNCHER_DIR\target" -Filter "launcher.jar" | Select-Object -First 1
if (-not $LAUNCHER_JAR) { throw "launcher.jar nao encontrado. Execute o build do launcher primeiro." }
Copy-Item $LAUNCHER_JAR.FullName "$INPUT_DIR\launcher.jar"

$BACKEND_JAR = Get-ChildItem "$BACKEND_DIR\target" -Filter "*.jar" |
               Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
               Select-Object -First 1
if (-not $BACKEND_JAR) { throw "back-end JAR nao encontrado. Execute o build do back-end primeiro." }
Copy-Item $BACKEND_JAR.FullName "$APP_SUBDIR\back-end.jar"

Write-Host "  launcher.jar -> input\launcher.jar"
Write-Host "  back-end.jar -> input\app\back-end.jar"

# ----------------------------------------------------------
# 4. Converte icone PNG -> ICO
# ----------------------------------------------------------
$PNG_ICON = Join-Path $BACKEND_DIR "src\main\resources\static\assets\glaceonIcon .png"
$ICO_ICON = Join-Path $DIST_DIR "icon.ico"
$iconArgs = @()

if (Test-Path $PNG_ICON) {
    try {
        Write-Host "  Convertendo icone PNG para ICO..." -NoNewline
        Convert-PngToIco -PngPath $PNG_ICON -IcoPath $ICO_ICON
        Write-Host " OK" -ForegroundColor Green
        $iconArgs = @("--icon", $ICO_ICON)
    } catch {
        Write-Host " FALHOU (usando padrao do sistema)" -ForegroundColor Yellow
        Write-Host "  Detalhes: $_"
    }
} else {
    Write-Host "  Icone nao encontrado - usando padrao do sistema."
}

# ----------------------------------------------------------
# 5. jpackage
# ----------------------------------------------------------
Write-Host ""
Write-Host "[4/4] Gerando instalador com jpackage..." -ForegroundColor Yellow

New-Item -ItemType Directory -Path $OUTPUT -Force | Out-Null

$jpArgs = @(
    "--type",        "exe",
    "--name",        "MyFinance",
    "--app-version", "1.0.0",
    "--vendor",      "MyFinance",
    "--description", "Controle Financeiro Pessoal",
    "--input",       $INPUT_DIR,
    "--dest",        $OUTPUT,
    "--main-jar",    "launcher.jar",
    "--main-class",  "controle.api.launcher.LauncherApp",
    "--win-dir-chooser",
    "--win-menu",
    "--win-shortcut",
    "--win-shortcut-prompt",
    "--java-options", "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.util=ALL-UNNAMED"
) + $iconArgs

Write-Host "  Argumentos jpackage:"
$jpArgs | ForEach-Object { Write-Host "    $_" }

& jpackage @jpArgs

if ($LASTEXITCODE -ne 0) {
    throw "jpackage falhou. Verifique se WiX Toolset v4 esta instalado: https://wixtoolset.org/releases/"
}

Write-Host ""
Write-Host "Instalador gerado em: $OUTPUT" -ForegroundColor Green
Write-Host "Arquivo: MyFinance-1.0.0.exe" -ForegroundColor Green
Write-Host ""
Write-Host "Dica de debug: se o app nao abrir, verifique o log em:" -ForegroundColor DarkGray
Write-Host "  %APPDATA%\MyFinance\launcher.log" -ForegroundColor DarkGray
