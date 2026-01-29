@echo off
set "PYTHON_EXE=%APPDATA%\chaiNNer\python\python\python.exe"

echo Checking chaiNNer Python environment...
if not exist "%PYTHON_EXE%" (
    echo [ERROR] chaiNNer Python not found in %APPDATA%. 
    echo Please make sure chaiNNer is installed and using integrated Python.
    pause
    exit /b
)

echo.
echo 1. Protecting core dependencies (NumPy)...
"%PYTHON_EXE%" -m pip install numpy==1.24.4

echo.
echo 2. Installing DNG support (rawpy ^& imageio) without touching NumPy...
"%PYTHON_EXE%" -m pip install rawpy tifffile imageio --no-deps

echo.
echo 3. Verifying installation...
"%PYTHON_EXE%" -c "import numpy; import rawpy; import tifffile; import cv2; print('SUCCESS: NumPy ' + numpy.__version__ + ' and rawpy are working together!')"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Something went wrong. Trying to repair NumPy one last time...
    "%PYTHON_EXE%" -m pip install --force-reinstall numpy==1.24.4
)

echo.
echo Done! You can now restart chaiNNer.
pause