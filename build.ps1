param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments)
$ErrorActionPreference = 'Stop'
$previousJavaHome = $env:JAVA_HOME
$previousLocation = Get-Location
try {
    $jdkRoot = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    $jdkCandidate = Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-17*' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe')) } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdkCandidate) { $env:JAVA_HOME = $jdkCandidate.FullName }
    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
        throw 'Configure JAVA_HOME to a Java 17 JDK before building.'
    }
    Set-Location -LiteralPath $PSScriptRoot
    if (-not $GradleArguments) { $GradleArguments = @('build') }
    & .\gradlew.bat @GradleArguments --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    Set-Location -LiteralPath $previousLocation
}
