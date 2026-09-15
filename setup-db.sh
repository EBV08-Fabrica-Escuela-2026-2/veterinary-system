#!/bin/bash
# ============================================
# SETUP COMPLETO - PostgreSQL + Base Veterinaria
# ============================================
# Copiá y pegá este script completo en Terminal
# ============================================

set -e

echo "🦴 PASO 1: Deteniendo PostgreSQL 12 anterior..."
sudo launchctl stop com.edb.launchd.postgresql-12 2>/dev/null || echo "  (ya detenido o no encontrado)"

echo ""
echo "📦 PASO 2: Instalando PostgreSQL 16 con Homebrew..."
brew install postgresql@16

echo ""
echo "🚀 PASO 3: Iniciando PostgreSQL 16..."
brew services start postgresql@16

echo ""
echo "⏳ Esperando 3 segundos a que el server arranque..."
sleep 3

echo ""
echo "✅ PASO 4: Verificando conexión..."
psql postgres -c "SELECT version();"

echo ""
echo "🗄️  PASO 5: Creando base de datos y usuario..."
psql postgres <<EOF
CREATE USER vetsa_user WITH PASSWORD 'vetsa_password';
CREATE DATABASE veterinaria OWNER vetsa_user;
GRANT ALL PRIVILEGES ON DATABASE veterinaria TO vetsa_user;
EOF

echo ""
echo "📋 PASO 6: Cargando estructura y datos de prueba..."
psql -U vetsa_user -d veterinaria -f "$(dirname "$0")/src/main/resources/db/migration/V1__create_tables_and_seed_data.sql"

echo ""
echo "🔍 PASO 7: Verificando datos cargados..."
psql -U vetsa_user -d veterinaria -c "SELECT id, nombre, telefono FROM veterinario;"
psql -U vetsa_user -d veterinaria -c "SELECT v.nombre AS veterinario, s.nombre AS servicio, s.precio FROM servicio s JOIN veterinario v ON s.veterinario_id = v.id ORDER BY v.nombre;"

echo ""
echo "🎉 ¡TODO LISTO!"
echo "============================================"
echo "  Base de datos: veterinaria"
echo "  Usuario:       vetsa_user"
echo "  Contraseña:    vetsa_password"
echo "  Puerto:        5432"
echo "============================================"
echo ""
echo "Para arrancar el backend:"
echo "  cd $(dirname "$0")"
echo "  ./mvnw spring-boot:run"
echo ""
