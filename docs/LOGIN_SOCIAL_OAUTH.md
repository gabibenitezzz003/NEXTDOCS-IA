# Login social (Google y Microsoft)

NEXT DOC AI permite ingresar con cuentas de Google y Microsoft (Entra ID) además del ingreso local con email y clave. El flujo reutiliza el sistema de federación de identidad por tenant: el IdP externo autentica, el core verifica el `id_token` por JWKS, aprovisiona o vincula el usuario y emite la sesión normal (access + refresh JWT). El microservicio workflow no cambia: sigue validando el JWT del core.

## Flujo

1. El portal lista los proveedores OAuth del tenant: `GET /api/v1/federacion/oauth/proveedores?tenant=<codigo>` (público, solo devuelve `codigo` y `nombre`).
2. El usuario elige un proveedor y el navegador va a `GET /api/v1/federacion/oauth/{tenant}/{proveedor}/iniciar?retorno=<origen>` → 302 al IdP con `state` firmado (HMAC-SHA256 con el secreto JWT, vigencia 5 minutos) y `nonce`.
3. El IdP redirige a `GET /api/v1/federacion/oauth/callback?code=...&state=...`. El backend verifica la firma y vigencia del `state`, canjea el `code` en el token endpoint del proveedor, verifica el `id_token` (firma JWKS, emisor, audiencia y `nonce`), aplica las reglas del proveedor (dominios permitidos, JIT, vínculo por email, rol por defecto) y genera un código de un solo uso.
4. El backend redirige al portal: `{portal}/ingresar?codigo=<codigo>` (o `?errorFederado=<mensaje>`). El frontend canjea el código en `POST /api/v1/federacion/canje` y obtiene la `Sesion` normal.

El `retorno` del paso 2 debe ser un origen declarado en `origenesEmbedPermitidos` del proveedor; cualquier otro origen se rechaza con 403. Si no se envía `retorno`, se usa `nextdocs.federacion.urlBasePortal`.

## Registro de las aplicaciones

### Google Cloud Console

1. `console.cloud.google.com` → proyecto nuevo.
2. **APIs & Services → OAuth consent screen** → External → nombre del producto y email de soporte.
3. **Credentials → Create Credentials → OAuth client ID** → tipo **Web application**.
4. Authorized redirect URIs:
   - Local: `http://localhost:8090/api/v1/federacion/oauth/callback`
   - Producción: `https://<dominio>/api/v1/federacion/oauth/callback`
5. Guardar **Client ID** y **Client Secret**.

Emisor `https://accounts.google.com`, JWKS `https://www.googleapis.com/oauth2/v3/certs`, autorización `https://accounts.google.com/o/oauth2/v2/auth`, token `https://oauth2.googleapis.com/token`, alcances `openid email profile`.

### Microsoft Entra

1. `entra.microsoft.com` → **App registrations → New registration**.
2. Tipo de cuenta: un solo directorio o cualquier directorio según quién deba ingresar.
3. **Add a Redirect URI** → plataforma **Web** → la misma URL de callback que en Google (local y producción).
4. **Certificates & secrets → New client secret** → copiar la columna **Value** (no el Secret ID; el Value se muestra una sola vez).
5. Guardar **Application (client) ID** y **Directory (tenant) ID**.

Con el tenant `TID` de Entra, los endpoints v2.0 son: emisor `https://login.microsoftonline.com/{TID}/v2.0`, JWKS `https://login.microsoftonline.com/{TID}/discovery/v2.0/keys`, autorización `https://login.microsoftonline.com/{TID}/oauth2/v2.0/authorize`, token `https://login.microsoftonline.com/{TID}/oauth2/v2.0/token`, alcances `openid email profile`.

En cuentas Microsoft personales el `id_token` puede no traer `email` y sí `preferred_username`; en ese caso configurar `claimEmail` como `preferred_username`.

## Configuración del proveedor por tenant

Los proveedores se crean por API con permiso `tenant.administrar` (no hay migración con secretos). Ejemplo Google:

```json
POST /api/v1/federacion/proveedores
{
  "codigo": "google",
  "nombre": "Google",
  "origen": "OIDC",
  "emisor": "https://accounts.google.com",
  "urlJwks": "https://www.googleapis.com/oauth2/v3/certs",
  "audiencia": "<client-id>",
  "claimSujeto": "sub",
  "claimEmail": "email",
  "claimNombre": "name",
  "permitirJit": true,
  "permitirVinculoPorEmail": true,
  "verificaEmail": true,
  "codigoRolPorDefecto": "ADMINISTRADOR",
  "dominiosPermitidos": "@empresa.com,@gmail.com",
  "origenesEmbedPermitidos": "http://localhost:5175,https://<dominio>",
  "segundosVigenciaCodigo": 60,
  "clienteId": "<client-id>",
  "clienteSecreto": "<client-secret>",
  "urlAutorizacion": "https://accounts.google.com/o/oauth2/v2/auth",
  "urlToken": "https://oauth2.googleapis.com/token",
  "alcances": "openid email profile"
}
```

Notas:

- `clienteId`, `clienteSecreto`, `urlAutorizacion` y `urlToken` habilitan el login social solo si van los cuatro juntos; `audiencia` debe coincidir con el `clienteId` (el `aud` del `id_token`).
- `dominiosPermitidos` acepta entradas con o sin `@` (se normalizan a `@dominio`); vacío permite cualquier dominio.
- `origenesEmbedPermitidos` debe incluir el origen del portal si el frontend envía `retorno`.
- `verificaEmail` declara que el IdP garantiza el email verificado (Google, Microsoft). Solo esos proveedores resuelven la identidad y el email **entre todos los tenants**: quien ya tiene cuenta entra a su propio tenant, sin importar desde qué organización vino el intento.
- En el MVP el `codigoRolPorDefecto` es `ADMINISTRADOR`: cualquier persona que entre por Google o Microsoft ve y ejecuta todo. Cuando se divida por roles, bajarlo a `REVISOR` y promover usuarios a mano.
- **Login social sin código de organización**: `GET /api/v1/federacion/oauth/{proveedor}/iniciar` resuelve el proveedor único activo y firma el `state` con la marca "sin tenant". En el callback, un email nuevo se aprovisiona en un **tenant personal propio** (código `u-<usuario>-<hash>`, rol ADMINISTRADOR, catálogo base sembrado) — cada cuenta social tiene su portal aislado. Con código de organización (`/oauth/{tenant}/{proveedor}/iniciar`), el usuario nuevo cae en ese tenant con el rol por defecto del proveedor.
- El secreto se guarda en la base y **nunca** sale en las respuestas de la API (`ProveedorIdentidadModel` no lo serializa).
- Rotar un secreto hoy requiere actualizar la fila (`eliminar` es baja lógica y el código queda reservado); un endpoint de actualización queda como pendiente.
- El `client_secret` también puede venir de un gestor de secretos: cargarlo en la fila del proveedor al momento del alta, no en el repositorio ni en el frontend.

## Variables de entorno

| Variable | Uso | Default local |
|---|---|---|
| `NEXTDOCS_FEDERACION_URL_BASE_API` | Base pública de la API para construir el `redirect_uri` | `http://localhost:8090` |
| `NEXTDOCS_FEDERACION_URL_BASE_PORTAL` | Portal por defecto cuando `iniciar` no recibe `retorno` | `http://localhost:5175` |
| `NEXTDOCS_FEDERACION_EXIGIR_HTTPS` | Exige HTTPS en emisor/JWKS/URLs OAuth | `true` |
| `NEXTDOCS_FEDERACION_TIMEOUT_MS` | Timeout de llamadas al IdP (JWKS y token) | `8000` |

El `redirect_uri` registrado en el IdP debe ser exactamente `{URL_BASE_API}/api/v1/federacion/oauth/callback`; una diferencia de esquema, host o puerto produce `redirect_uri_mismatch`.

## Producción

- Google y Microsoft exigen HTTPS con dominio real para la redirect URI (salvo `localhost`). Sin dominio no hay login social en producción — es el mismo bloqueo que Certbot/nginx.
- Configurar `NEXTDOCS_FEDERACION_URL_BASE_API=https://<dominio>` y `NEXTDOCS_FEDERACION_URL_BASE_PORTAL=https://<dominio>`, registrar la URI HTTPS en ambos IdP y agregar el origen a `origenesEmbedPermitidos` del proveedor.

## Errores frecuentes

| Síntoma | Causa probable |
|---|---|
| `redirect_uri_mismatch` en el IdP | La URI registrada no coincide byte a byte con `urlBaseApi + callback` |
| `El proveedor rechazo el canje del codigo: HTTP 401` | `client_secret` inválido; en Entra suele ser haber copiado el Secret ID en vez del Value |
| `El dominio @x no esta habilitado` | `dominiosPermitidos` no incluye el dominio del email federado |
| `El token federado no corresponde a esta solicitud` | `nonce` del `id_token` distinto al emitido (replay o mezcla de flujos) |
| `El estado de la federacion no es valido o vencio` | `state` alterado, de otro flujo o con más de 5 minutos |
| `El origen X no esta permitido` | `retorno` no listado en `origenesEmbedPermitidos` |
| El proveedor no aparece en el login | Falta alguno de los 4 campos OAuth o `activo=false` |
