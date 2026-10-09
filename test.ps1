<#
.SYNOPSIS
    Starts a local dedicated server with the mod and clients that join it, each in its own window.

.EXAMPLE
    .\test.cmd                       # server + one client (Developer)
    .\test.cmd -Players Alex,Steve   # server + two clients, e.g. to test two riders
    .\test.cmd -ServerOnly           # just the server
    .\test.cmd -NoServer             # clients joining a server that's already running
    .\test.cmd -Lag 300 -Jitter 50   # clients join through a proxy adding 300 ms ping (+/-50 ms)
    .\test.cmd -Lag 200 -LagSpike 1500 -Players Alex,Steve -LagPlayers Alex
                                     # only Alex lags, and freezes for 1.5 s every 10 s
    .\test.cmd -Release              # the release jar in the real (obfuscated) game instead of the dev game

Server files live in run/server, extra players in run/players/<name>. With -Release everything lives
under run/obfuscated instead, so release testing never touches your dev worlds.
Players listed here are made server operators so they can use /give, /gamemode etc.
The lag options need Python (scripts/lagproxy.py), the proxy listens on Port + 1.
#>
param(
    [string[]]$Players = @("Developer"),
    [int]$Port = 25565,
    [switch]$ServerOnly,
    [switch]$NoServer,
    # Test the release jar in the real game, to catch problems that only show up outside the dev environment
    [switch]$Release,
    # Round trip delay in ms added to clients' connections
    [int]$Lag = 0,
    # Random +/- ms added to the delay in each direction
    [int]$Jitter = 0,
    # Freeze the connection for this many ms every 10 seconds
    [int]$LagSpike = 0,
    # Which players get the lag, defaults to everyone
    [string[]]$LagPlayers = @()
)

$ErrorActionPreference = "Stop"

# test.cmd runs this with -File, which passes "Alex,Steve" as one string instead of a list
$Players = @($Players | ForEach-Object { $_ -split "," } | Where-Object { $_ })
$LagPlayers = @($LagPlayers | ForEach-Object { $_ -split "," } | Where-Object { $_ })
$root = $PSScriptRoot
if ($Release) {
    $serverDir = Join-Path $root "run\obfuscated\server"
    $serverTask = "runObfServer"
    $clientTask = "runObfClient"
    $buildTask = "reobfJar"
} else {
    $serverDir = Join-Path $root "run\server"
    $serverTask = "runServer"
    $clientTask = "runClient"
    $buildTask = "classes"
}

# The UUID an offline mode server gives a player name (Java's UUID.nameUUIDFromBytes)
function Get-OfflineUuid([string]$name) {
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $b = $md5.ComputeHash([System.Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$name"))
    $b[6] = ($b[6] -band 0x0f) -bor 0x30
    $b[8] = ($b[8] -band 0x3f) -bor 0x80
    $hex = -join ($b | ForEach-Object { $_.ToString("x2") })
    return "{0}-{1}-{2}-{3}-{4}" -f $hex.Substring(0, 8), $hex.Substring(8, 4), $hex.Substring(12, 4), $hex.Substring(16, 4), $hex.Substring(20, 12)
}

function Test-Port([int]$port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect("127.0.0.1", $port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

# Opens a new window running a gradle task, left open afterwards so errors stay readable
function Start-GradleWindow([string]$title, [string]$gradleArgs) {
    $command = "`$Host.UI.RawUI.WindowTitle = '$title'; Set-Location '$root'; .\gradlew.bat $gradleArgs"
    return Start-Process powershell -ArgumentList "-NoExit", "-Command", $command -PassThru
}

function Initialize-Server {
    New-Item -ItemType Directory -Force $serverDir | Out-Null

    $eula = Join-Path $serverDir "eula.txt"
    if (-not ((Test-Path $eula) -and (Select-String -Path $eula -Pattern "eula=true" -Quiet))) {
        Write-Host "The server needs the Minecraft EULA accepted: https://account.mojang.com/documents/minecraft_eula"
        $answer = Read-Host "Do you accept it? (y/n)"
        if ($answer -notmatch "^[yY]") {
            throw "EULA not accepted, can't start the server."
        }
        Set-Content -Path $eula -Value "eula=true" -Encoding ASCII
    }

    # Dev clients aren't logged in to Mojang, so the server has to run in offline mode
    $props = Join-Path $serverDir "server.properties"
    if (-not (Test-Path $props)) {
        Set-Content -Path $props -Encoding ASCII -Value @(
            "online-mode=false",
            "server-port=$Port",
            "spawn-protection=0",
            "motd=Harmony test server"
        )
    } elseif (Select-String -Path $props -Pattern "^online-mode=true" -Quiet) {
        Write-Warning "run\server\server.properties has online-mode=true, dev clients won't be able to join."
    }

    $opsFile = Join-Path $serverDir "ops.json"
    $ops = @()
    if (Test-Path $opsFile) {
        # ForEach-Object unwraps the array, Windows PowerShell returns it as a single item
        $ops = @(Get-Content $opsFile -Raw | ConvertFrom-Json | ForEach-Object { $_ })
    }
    $changed = $false
    foreach ($player in $Players) {
        if (-not ($ops | Where-Object { $_.name -eq $player })) {
            $ops += [pscustomobject]@{ uuid = (Get-OfflineUuid $player); name = $player; level = 4 }
            $changed = $true
        }
    }
    if ($changed) {
        # Written without a BOM, which Minecraft's JSON reader can't handle
        [System.IO.File]::WriteAllText($opsFile, (ConvertTo-Json -InputObject @($ops)))
    }
}

Set-Location $root

if (-not $NoServer) {
    Initialize-Server
}

# Compile once up front so the windows below don't all try to build at the same time
Write-Host "Building..."
& .\gradlew.bat $buildTask -q
if ($LASTEXITCODE -ne 0) {
    throw "Build failed."
}

if (-not $NoServer) {
    if (Test-Port $Port) {
        throw "Something is already using port $Port. Is a server already running? Use -NoServer to just start clients."
    }

    $server = Start-GradleWindow "Harmony server" $serverTask
    Write-Host "Waiting for the server to start on port $Port..."
    $deadline = (Get-Date).AddMinutes(5)
    while (-not (Test-Port $Port)) {
        if ($server.HasExited) {
            throw "The server window closed before the server started."
        }
        if ((Get-Date) -gt $deadline) {
            throw "Server didn't start within 5 minutes, check the server window."
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "Server is up."
}

$useLag = ($Lag -gt 0 -or $Jitter -gt 0 -or $LagSpike -gt 0) -and -not $ServerOnly
$proxyPort = $Port + 1
if ($useLag) {
    if (Test-Port $proxyPort) {
        throw "Port $proxyPort is already in use, is a lag proxy from an earlier run still open? Close its window first."
    }
    $proxyArgs = "--listen $proxyPort --target 127.0.0.1:$Port --lag $Lag --jitter $Jitter --spike $LagSpike"
    $command = "`$Host.UI.RawUI.WindowTitle = 'Harmony lag proxy'; python -I '$root\scripts\lagproxy.py' $proxyArgs"
    $proxy = Start-Process powershell -ArgumentList "-NoExit", "-Command", $command -PassThru
    while (-not (Test-Port $proxyPort)) {
        if ($proxy.HasExited) {
            throw "The lag proxy window closed before it started."
        }
        Start-Sleep -Milliseconds 500
    }
    Write-Host "Lag proxy is up on port $proxyPort."
}

if (-not $ServerOnly) {
    foreach ($player in $Players) {
        $connectPort = $Port
        if ($useLag -and ($LagPlayers.Count -eq 0 -or $LagPlayers -contains $player)) {
            $connectPort = $proxyPort
        }
        $gradleArgs = "$clientTask -Pconnect=localhost:$connectPort"
        # Developer uses the base folder, anyone else gets their own folder in players/
        if ($player -ne "Developer") {
            $gradleArgs += " -Pplayer=$player"
        }
        Start-GradleWindow "Harmony client - $player" $gradleArgs | Out-Null
        if ($connectPort -eq $proxyPort) {
            Write-Host "Started client for $player (lagged)."
        } else {
            Write-Host "Started client for $player."
        }
    }
}
