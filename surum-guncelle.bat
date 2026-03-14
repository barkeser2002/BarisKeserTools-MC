@echo off
chcp 65001 >nul
color 0A
echo ===================================================
echo     BarisKeserTools Otomatik Surum Guncelleme
echo ===================================================
echo.

set /p new_version=Lutfen yeni surumu girin (Orn: 1.3.5): 

if "%new_version%"=="" (
    echo Surum bos birakilamaz! Islemi iptal ediliyor...
    pause
    exit /b
)

echo.
echo [1/3] Dosyalar guncelleniyor (build.gradle.kts ve plugin.yml)...

:: BOM olmadan UTF-8 olarak kaydetmek icin PowerShell ayari
powershell -Command "$utf8NoBom = New-Object System.Text.UTF8Encoding $false; $content = Get-Content build.gradle.kts -Raw -Encoding UTF8; $content = $content -replace '(?m)^version = \"".*\""', 'version = \""%new_version%\""'; [IO.File]::WriteAllText('build.gradle.kts', $content, $utf8NoBom)"

powershell -Command "$utf8NoBom = New-Object System.Text.UTF8Encoding $false; $content = Get-Content src\main\resources\plugin.yml -Raw -Encoding UTF8; $content = $content -replace '(?m)^version: .*', 'version: %new_version%'; [IO.File]::WriteAllText('src\main\resources\plugin.yml', $content, $utf8NoBom)"

echo.
echo [2/3] Git'e ekleniyor ve Commitleniyor...
git add build.gradle.kts src\main\resources\plugin.yml
git commit -m "chore(release): Surum v%new_version% olarak guncellendi"

:: Guncellemelerin Github Action'da release olusturması muhtemel oldugu icin tag de atiyoruz
git tag v%new_version%

echo.
echo [3/3] Github'a yukleniyor (Kodlar ve Tag'ler)...
git push origin main
git push origin v%new_version%

echo.
echo ===================================================
echo Islem Basariyla Tamamlandi! 
echo Yeni surum (v%new_version%) Github'a gonderildi.
echo ===================================================
pause
