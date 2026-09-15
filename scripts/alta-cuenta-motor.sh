#!/usr/bin/env bash
# Crea en el core la cuenta de servicio que el motor de procesos usa para leer
# documentos, y deja la clave en .env. La clave se muestra una sola vez, por eso
# el script la guarda en lugar de imprimirla.
set -euo pipefail

CORE=${CORE:-http://localhost:8090}
TENANT=${TENANT:-demo}
EMAIL=${EMAIL:-admin@nextdocs.ai}
CLAVE=${CLAVE:-nextdocs123}
NOMBRE=${NOMBRE:-Motor de procesos}
URL_BASE=${URL_BASE:-http://app:8090}
RAIZ=$(cd "$(dirname "$0")/.." && pwd)
ENV=${ENV:-$RAIZ/.env}

if [ ! -f "$ENV" ]; then
  echo "No existe el archivo de entorno $ENV."
  echo "En produccion el que lee compose es /etc/nextdocs-ia/nextdocs.env; pasalo con ENV=."
  exit 1
fi

if [ ! -w "$ENV" ]; then
  echo "No puedo escribir en $ENV. La clave se muestra una sola vez, asi que no doy de alta"
  echo "la cuenta hasta poder guardarla."
  exit 1
fi

if grep -q "^NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES=" "$ENV" 2>/dev/null; then
  echo "Ya hay una clave en $ENV. Si querés reemplazarla, borrá esa línea y volvé a correr esto."
  exit 1
fi

echo "Voy a guardar la clave en $ENV"

echo "1. Ingreso al core como $EMAIL"
SESION=$(curl -fsS -X POST "$CORE/api/v1/autenticacion/ingresar" \
  -H "Content-Type: application/json" \
  -d "{\"codigoTenant\":\"$TENANT\",\"email\":\"$EMAIL\",\"clave\":\"$CLAVE\"}")

TOKEN=$(printf '%s' "$SESION" | python3 -c "import sys,json;print(json.load(sys.stdin)['tokenAcceso'])")
TENANT_ID=$(printf '%s' "$SESION" | python3 -c "import sys,json;print(json.load(sys.stdin)['tenantId'])")
echo "   tenant: $TENANT_ID"

echo "2. Alta de la cuenta de servicio con alcance documentos.leer"
RESPUESTA=$(curl -fsS -X POST "$CORE/api/v1/administracion/cuentas-servicio" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"$NOMBRE\",\"alcances\":[\"documentos.leer\"],\"diasVigencia\":365}")

echo "3. Guardo la clave en $ENV"
printf '%s' "$RESPUESTA" | TENANT_ID="$TENANT_ID" ENV="$ENV" URL_BASE="$URL_BASE" python3 -c "
import sys, json, os
d = json.load(sys.stdin)
clave = d.get('clave')
if not clave:
    sys.exit('La respuesta no trajo la clave: ' + json.dumps(d)[:300])
destino = os.environ['ENV']
with open(destino, 'a') as f:
    f.write('\n# Cuenta de servicio que el motor usa para leer documentos del core\n')
    f.write('NEXTDOCS_WORKFLOW_DOCUMENTAL_URL_BASE=' + os.environ['URL_BASE'] + '\n')
    f.write('NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES=' + os.environ['TENANT_ID'] + '=' + clave + '\n')
print('   cuenta:', d['cuenta']['nombre'], '| prefijo:', d['cuenta'].get('prefijoClave'))
print('   clave guardada en', destino, '(no se imprime: se muestra una sola vez)')
"

echo
echo "Listo. Ahora recreá el motor para que tome la clave."
echo "  local:      docker compose --profile workflow up -d workflow"
echo "  produccion: docker compose -f /etc/nextdocs-ia/compose.produccion.yml \\"
echo "                --env-file $ENV up -d workflow"
