@echo off
set "source=D:\trae\ae2_utilix\build\libs\ae2_utilix-1.0.jar"
set "target=D:\pcl2\.minecraft\versions\1.12.2-Forge_14.23.5.2864\mods\"

if not exist "%source%" (
    echo 错误：源文件不存在 - %source%
    exit /b 1
)

copy /y "%source%" "%target%"
if %errorlevel% equ 0 (
    echo 复制成功！
) else (
    echo 复制失败，请检查路径或权限。
)