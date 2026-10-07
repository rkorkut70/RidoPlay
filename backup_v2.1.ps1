Add-Type -AssemblyName System.IO.Compression.FileSystem

$timestamp = Get-Date -Format "yyyyMMdd_HHmm"
$backupDir = "c:\anti-gravity\yedekler\otomuzik"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$tempDir = Join-Path $env:TEMP "muzik_src_backup_$timestamp"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

Write-Host "Kopyalanıyor: Kaynak dosyalar..."
# Root configuration files
Copy-Item "c:\anti-gravity\muzik\build.gradle.kts" $tempDir
Copy-Item "c:\anti-gravity\muzik\settings.gradle.kts" $tempDir
Copy-Item "c:\anti-gravity\muzik\gradle.properties" $tempDir
Copy-Item "c:\anti-gravity\muzik\gradlew" $tempDir
Copy-Item "c:\anti-gravity\muzik\gradlew.bat" $tempDir
Copy-Item "c:\anti-gravity\muzik\.gitignore" $tempDir

# Gradle wrapper
Copy-Item -Recurse "c:\anti-gravity\muzik\gradle" (Join-Path $tempDir "gradle")

# App sources
$tempApp = Join-Path $tempDir "app"
New-Item -ItemType Directory -Force -Path $tempApp | Out-Null
Copy-Item "c:\anti-gravity\muzik\app\build.gradle.kts" $tempApp
if (Test-Path "c:\anti-gravity\muzik\app\proguard-rules.pro") {
    Copy-Item "c:\anti-gravity\muzik\app\proguard-rules.pro" $tempApp
}
Copy-Item -Recurse "c:\anti-gravity\muzik\app\src" (Join-Path $tempApp "src")

# Zipleri oluştur
$zipTimestamped = Join-Path $backupDir "muzik_src_v2.1_$timestamp.zip"
$zipLatest = Join-Path $backupDir "muzik_src_v2.1.zip"

if (Test-Path $zipTimestamped) { Remove-Item -Force $zipTimestamped }
if (Test-Path $zipLatest) { Remove-Item -Force $zipLatest }

Write-Host "Zip dosyası oluşturuluyor: $zipTimestamped"
[System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $zipTimestamped, [System.IO.Compression.CompressionLevel]::Optimal, $false)
Copy-Item -Force $zipTimestamped $zipLatest

# APK kopyalama
$apkSource = "c:\anti-gravity\muzik\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkSource) {
    Write-Host "APK kopyalanıyor..."
    $apkV21 = Join-Path $backupDir "RidoPlay-v2.1.apk"
    $apkLatest = Join-Path $backupDir "RidoPlay_Latest.apk"
    $apkRoot = "c:\anti-gravity\muzik\RidoPlay-v2.1.apk"

    Copy-Item -Force $apkSource $apkV21
    Copy-Item -Force $apkSource $apkLatest
    Copy-Item -Force $apkSource $apkRoot
} else {
    Write-Error "APK bulunamadı: $apkSource"
}

# Temizlik
Remove-Item -Recurse -Force $tempDir

Write-Host "Yedekleme başarıyla tamamlandı!"
Get-ChildItem $backupDir | Where-Object { $_.Name -like "*v2.1*" -or $_.Name -like "*Latest*" } | Select-Object Name, Length, LastWriteTime
