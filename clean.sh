#!/bin/bash
# Script para limpar ficheiros gerados pelo programa

echo "Script simples para limpar os files de class e binários"

if [ -d "data/series" ]; then
    rm -f data/series/*.dat
    echo "Ficheiros .dat removidos"
fi

find src -name "*.class" -type f -delete
echo "Ficheiros .class removidos"

echo  "Limpeza feita"
