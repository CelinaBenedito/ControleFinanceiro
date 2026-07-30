# publish-release.ps1
# Publica um novo release no GitHub com:
#   - O back-end.jar como asset para download pela aplicacao
#   - O update4j-config.xml atualizado no repositorio (para o launcher)
#
# Pre-requisitos:
#   - GitHub CLI (gh) instalado e autenticado: https://cli.github.com
#   - Maven no PATH
#   - Git configurado
#
# Uso:
#   .\publish-release.ps1 -Version "1.1.0" -Notes "Descricao do que mudou"

param(
    [Parameter(Mandatory=$true)]
    [string]$Version,

    [string]$Notes = "Nova versao do MyFinance"
)

$ErrorActionPreference = "Stop"

$ROOT        = Split-Path -Parent $PSScriptRoot
$BACKEND_DIR = Join-Path $ROOT "back-end"
$TAG         = "v$Version"

Write-Host "=== MyFinance - Publicando Release $TAG ===" -ForegroundColor Cyan

# 1. Atualiza version.properties
Write-Host ""
Write-Host "[1/5] Atualizando version.properties para $Version..." -ForegroundColor Yellow
$versionFile = Join-Path $BACKEND_DIR "src\main\resources\version.properties"
Set-Content -Path $versionFile -Value "app.version=$Version" -Encoding UTF8

# 2. Build do back-end
Write-Host ""
Write-Host "[2/5] Compilando back-end..." -ForegroundColor Yellow
Push-Location $BACKEND_DIR
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Falha no build." }
Pop-Location

$JAR = Get-ChildItem "$BACKEND_DIR\target" -Filter "*.jar" |
       Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
       Select-Object -First 1

if (-not $JAR) { throw "JAR nao encontrado em $BACKEND_DIR\target" }

$JAR_PATH    = $JAR.FullName
$JAR_SIZE    = (Get-Item $JAR_PATH).Length
$JAR_SIZE_MB = [math]::Round($JAR_SIZE / 1048576, 2)

Write-Host "  JAR: $($JAR.Name) ($JAR_SIZE_MB MB)"

# 3. Calcula checksum SHA-256 para o Update4j config
Write-Host ""
Write-Host "[3/5] Calculando checksum SHA-256..." -ForegroundColor Yellow
$hash = (Get-FileHash -Path $JAR_PATH -Algorithm SHA256).Hash.ToLower()
Write-Host "  SHA-256: $hash"

# 4. Gera update4j-config.xml
Write-Host ""
Write-Host "[4/5] Gerando update4j-config.xml..." -ForegroundColor Yellow

$appPropsPath = "$BACKEND_DIR\src\main\resources\application.properties"
$appProps = Get-Content $appPropsPath

$ownerLine = $appProps | Where-Object { $_ -match "^app\.update\.github\.owner=" }
$repoLine  = $appProps | Where-Object { $_ -match "^app\.update\.github\.repo=" }

$owner = ($ownerLine -replace "^app\.update\.github\.owner=", "").Trim()
$repo  = ($repoLine  -replace "^app\.update\.github\.repo=", "").Trim()

if ($owner -eq "SEU_USUARIO_GITHUB" -or $repo -eq "SEU_REPOSITORIO" -or $owner -eq "" -or $repo -eq "") {
    Write-Host "  ERRO: Configure app.update.github.owner e app.update.github.repo em application.properties!" -ForegroundColor Red
    throw "GitHub owner/repo nao configurados."
}

Write-Host "  Repositorio: $owner/$repo"

$downloadBase = "https://github.com/$owner/$repo/releases/download/$TAG"
$configPath   = Join-Path $ROOT "update4j-config.xml"
$timestamp    = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")

$xml  = '<?xml version="1.0" encoding="UTF-8"?>' + "`r`n"
$xml += "<!-- Gerado automaticamente por publish-release.ps1 | Versao: $Version | Data: $timestamp -->`r`n"
$xml += "<configuration timestamp=`"$timestamp`">`r`n"
$xml += "`r`n"
$xml += "    <base uri=`"$downloadBase/`"`r`n"
$xml += "          path=`"app/`" />`r`n"
$xml += "`r`n"
$xml += "    <properties>`r`n"
$xml += "        <property key=`"app.version`" value=`"$Version`" />`r`n"
$xml += "    </properties>`r`n"
$xml += "`r`n"
$xml += "    <files>`r`n"
$xml += "        <file path=`"back-end.jar`"`r`n"
$xml += "              uri=`"back-end.jar`"`r`n"
$xml += "              size=`"$JAR_SIZE`"`r`n"
$xml += "              checksum=`"$hash`"`r`n"
$xml += "              os=`"ANY`" />`r`n"
$xml += "    </files>`r`n"
$xml += "`r`n"
$xml += "</configuration>`r`n"

Set-Content -Path $configPath -Value $xml -Encoding UTF8
Write-Host "  Config salvo em: $configPath"

# 5. Commit, push e cria o release no GitHub
Write-Host ""
Write-Host "[5/5] Publicando no GitHub..." -ForegroundColor Yellow

Push-Location $ROOT

$RELEASE_JAR = Join-Path $BACKEND_DIR "target\back-end.jar"
Copy-Item $JAR_PATH $RELEASE_JAR -Force

& git add "update4j-config.xml" "back-end/src/main/resources/version.properties"
& git commit -m "chore: release $TAG - atualiza version.properties e update4j-config.xml"
& git push origin HEAD

& gh release create $TAG "$RELEASE_JAR#back-end.jar" --title "MyFinance $TAG" --notes $Notes --latest

if ($LASTEXITCODE -ne 0) { throw "Falha ao criar o release no GitHub." }

Pop-Location

Write-Host ""
Write-Host "Release $TAG publicado com sucesso!" -ForegroundColor Green
Write-Host "Os usuarios receberao a notificacao de atualizacao automaticamente." -ForegroundColor Green
Write-Host "Download URL: $downloadBase/back-end.jar" -ForegroundColor DarkGray
