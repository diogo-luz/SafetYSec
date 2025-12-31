#!/bin/bash

# 1. Definir o package
PACKAGE="pt.isec.diogo.safetysec"

echo "🚀 A compilar e instalar..."

# 2. Compilar e Instalar
./gradlew installDebug

# Verificar se o build correu bem
if [ $? -eq 0 ]; then
    echo "✅ Build com sucesso! A abrir a app..."
    
    # 3. Forçar paragem da app anterior
    adb shell am force-stop $PACKAGE
    
    # 4. Iniciar a MainActivity
    adb shell am start -n $PACKAGE/.MainActivity
else
    echo "❌ Erro na compilação. Verifica o log acima."
fi