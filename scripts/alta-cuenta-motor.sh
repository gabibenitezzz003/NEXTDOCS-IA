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
RAIZ=$(cd "$(dirname "$0")/.." && pwd)
ENV="$RAIZ/.env"

if grep -q "^NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES=" "$ENV" 2>/dev/null; then
  echo "Ya hay una clave en .env. Si querés reemplazarla, borrá esa línea y volvé a correr esto."
  exit 1
fi

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

echo "3. Guardo la clave en .env"
printf '%s' "$RESPUESTA" | TENANT_ID="$TENANT_ID" ENV="$ENV" python3 -c "
import sys, json, os
d = json.load(sys.stdin)
clave = d.get('clave')
if not clave:
    sys.exit('La respuesta no trajo la clave: ' + json.dumps(d)[:300])
with open(os.environ['ENV'], 'a') as f:
    f.write('\n# Cuenta de servicio que el motor usa para leer documentos del core\n')
    f.write('NEXTDOCS_WORKFLOW_DOCUMENTAL_CONECTOR=NEXTDOCS\n')
    f.write('NEXTDOCS_WORKFLOW_DOCUMENTAL_URL_BASE=http://app:8090\n')
    f.write('NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES=' + os.environ['TENANT_ID'] + '=' + clave + '\n')
print('   cuenta:', d['cuenta']['nombre'], '| prefijo:', d['cuenta'].get('prefijoClave'))
print('   clave guardada en .env (no se imprime: se muestra una sola vez)')
"

echo
echo "Listo. Ahora levantá el motor con la nueva configuración:"
echo "  docker compose --profile workflow up -d workflow"
