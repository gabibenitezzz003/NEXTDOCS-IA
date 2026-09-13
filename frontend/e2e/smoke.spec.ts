import { test, expect } from '@playwright/test';
import { createHmac } from 'crypto';

const SECRETO =
  process.env.NEXTDOCS_JWT_SECRETO ?? 'cambiar-este-secreto-en-produccion-minimo-32-bytes';

function tokenAcceso(tenantId: string): string {
  const b64 = (valor: object) => Buffer.from(JSON.stringify(valor)).toString('base64url');
  const cabecera = b64({ alg: 'HS256', typ: 'JWT' });
  const carga = b64({ iss: 'nextdocs-ai', sub: 'e2e', tenantId, tipo: 'acceso' });
  const firma = createHmac('sha256', SECRETO).update(`${cabecera}.${carga}`).digest('base64url');
  return `${cabecera}.${carga}.${firma}`;
}

test.describe('workflow health y tenant isolation', () => {
  test('health responde UP', async ({ request }) => {
    const respuesta = await request.get('/actuator/health');
    expect(respuesta.ok()).toBeTruthy();
    const cuerpo = await respuesta.json();
    expect(cuerpo.status).toBe('UP');
  });

  test('procesos rechaza el header de tenant sin JWT', async ({ request }) => {
    const respuesta = await request.get('/api/v1/procesos', {
      headers: { 'X-Tenant-Id': 'tenant-smoke' },
    });
    expect(respuesta.status()).toBe(401);
  });

  test('procesos responde con el tenant del JWT', async ({ request }) => {
    const respuesta = await request.get('/api/v1/procesos', {
      headers: { Authorization: `Bearer ${tokenAcceso('tenant-smoke')}` },
    });
    expect(respuesta.ok()).toBeTruthy();
    const cuerpo = await respuesta.json();
    expect(Array.isArray(cuerpo)).toBeTruthy();
  });
});
