@echo off
REM =========================================
REM Configuración SonarQube
REM =========================================

REM Definir token (NO recomendado dejarlo aquí en proyectos públicos)
REM set SONAR_TOKEN=REEMPLAZA_AQUI_TU_TOKEN
set SONAR_TOKEN=sqp_b4d8d99d4d99e52a476b9cccc00621cbe6960e82

REM Ejecutar análisis Sonar
call gradlew sonar ^
  -Dsonar.projectKey=puto ^
  -Dsonar.projectName=puto ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=%SONAR_TOKEN%

pause
