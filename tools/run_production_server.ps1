param([string]$JavaPath = 'java')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$serverRoot = Join-Path $projectRoot 'migration\production-server'
$forgeArguments = 'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt'
if (-not (Test-Path -LiteralPath (Join-Path $serverRoot $forgeArguments))) {
    throw 'Install Forge 1.20.1-47.4.10 into migration/production-server first.'
}
$javaVersion = & $JavaPath -version 2>&1 | Out-String
if ($javaVersion -notmatch 'version "17\.') { throw 'This server requires Java 17.' }
$properties = Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') -Raw
$modVersion = [regex]::Match($properties, '(?m)^mod_version=(.+)$').Groups[1].Value.Trim()
$playerJar = Join-Path $projectRoot "build\libs\tp-$modVersion.jar"
$mods = Join-Path $serverRoot 'mods'
New-Item -ItemType Directory -Path $mods -Force | Out-Null
Copy-Item -LiteralPath $playerJar -Destination $mods -Force
# The server itself creates eula.txt. This helper never accepts or changes the EULA.
Push-Location $serverRoot
try {
    & $JavaPath '-Xmx3G' '@user_jvm_args.txt' ('@' + $forgeArguments) 'nogui'
    $serverExitCode = $LASTEXITCODE
} finally { Pop-Location }
exit $serverExitCode
