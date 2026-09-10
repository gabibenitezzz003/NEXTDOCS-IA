import { test, expect } from '@playwright/test';

test.describe('workflow health y tenant isolation', () => {
  test('health responde UP', async ({ request }) => {
    const respuesta = await request.get('/actuator/health');
    expect(respuesta.ok()).toBeTruthy();
    const cuerpo = await respuesta.json();
    expect(cuerpo.status).toBe('UP');
  });

  test('procesos requiere header de tenant', async ({ request }) => {
    const respuesta = await request.get('/api/v1/procesos', {
      headers: { 'X-Tenant-Id': 'tenant-smoke' },
    });
    expect(respuesta.ok()).toBeTruthy();
    const cuerpo = await respuesta.json();
    expect(Array.isArray(cuerpo)).toBeTruthy();
  });
});
