import axios, { AxiosError, type AxiosInstance } from "axios";
import type { ErrorApi, Sesion } from "../tipos/api";

const CLAVE_REFRESCO = "nextdocs.tokenRefresco";

let tokenAcceso: string | null = null;
let versionSesion = 0;
let refrescoEnCurso: Promise<string | null> | null = null;
let alExpirarSesion: (() => void) | null = null;

export const cliente = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
});

export function fijarTokenAcceso(token: string | null) {
  versionSesion++;
  refrescoEnCurso = null;
  tokenAcceso = token;
}

export function tokenAccesoActual(): string | null {
  return tokenAcceso;
}

export function guardarTokenRefresco(token: string | null) {
  if (token) {
    sessionStorage.setItem(CLAVE_REFRESCO, token);
    return;
  }
  sessionStorage.removeItem(CLAVE_REFRESCO);
}

export function leerTokenRefresco(): string | null {
  return sessionStorage.getItem(CLAVE_REFRESCO);
}

export function registrarExpiracion(manejador: () => void) {
  alExpirarSesion = manejador;
}

cliente.interceptors.request.use((configuracion) => {
  const contexto = configuracion as typeof configuracion & { _versionSesion?: number };
  contexto._versionSesion = versionSesion;
  if (tokenAcceso) {
    configuracion.headers.Authorization = `Bearer ${tokenAcceso}`;
  }
  return configuracion;
});

async function refrescar(): Promise<string | null> {
  const versionInicial = versionSesion;
  const tokenRefresco = leerTokenRefresco();
  if (!tokenRefresco) {
    return null;
  }
  try {
    const { data } = await axios.post<Sesion>("/api/v1/autenticacion/refrescar", { tokenRefresco });
    if (versionInicial !== versionSesion) return null;
    tokenAcceso = data.tokenAcceso;
    guardarTokenRefresco(data.tokenRefresco);
    return data.tokenAcceso;
  } catch {
    return null;
  }
}

export function instalarRefrescoDeSesion(instancia: AxiosInstance) {
  instancia.interceptors.request.use((configuracion) => {
    const contexto = configuracion as typeof configuracion & {
      _versionSesion?: number;
    };
    contexto._versionSesion = versionSesion;
    return configuracion;
  });

  instancia.interceptors.response.use(
    (respuesta) => respuesta,
    async (error: AxiosError<ErrorApi>) => {
      const original = error.config as
        | (typeof error.config & { _reintentado?: boolean; _versionSesion?: number })
        | undefined;
      if (error.response?.status === 401 && original && !original._reintentado) {
        if (original._versionSesion !== versionSesion) return Promise.reject(error);
        original._reintentado = true;
        const solicitud = refrescoEnCurso ?? refrescar();
        refrescoEnCurso = solicitud;
        const nuevo = await solicitud;
        if (refrescoEnCurso === solicitud) refrescoEnCurso = null;
        if (original._versionSesion !== versionSesion) return Promise.reject(error);
        if (nuevo) {
          original.headers.Authorization = `Bearer ${nuevo}`;
          return instancia(original);
        }
        alExpirarSesion?.();
      }
      return Promise.reject(error);
    },
  );
}

instalarRefrescoDeSesion(cliente);

export function mensajeDeError(error: unknown): string {
  if (axios.isAxiosError<ErrorApi>(error)) {
    if (error.response?.status === 429) {
      const espera = error.response.headers["retry-after"];
      return espera
        ? `Se supero el limite de peticiones. Reintenta en ${espera} segundos`
        : "Se supero el limite de peticiones";
    }
    const cuerpo = error.response?.data;
    if (cuerpo?.campos) {
      return Object.entries(cuerpo.campos)
        .map(([campo, detalle]) => `${campo}: ${detalle}`)
        .join(". ");
    }
    if (cuerpo?.mensaje) {
      return cuerpo.mensaje;
    }
    if (!error.response) {
      return "No se pudo contactar al servidor. Verifica que el backend este levantado";
    }
    return porEstado(error.response.status);
  }
  return "Ocurrio un error inesperado";
}

export function tituloDeError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) return "Sin conexión";
    const estado = error.response.status;
    if (estado === 401) return "Sesión expirada";
    if (estado === 403) return "Sin permiso";
    if (estado === 404) return "No encontrado";
    if (estado === 409) return "Conflicto";
    if (estado === 429) return "Demasiadas peticiones";
    if (estado >= 500) return "Error del servidor";
    if (estado >= 400) return "Petición rechazada";
  }
  return "Error inesperado";
}

function porEstado(estado: number): string {
  if (estado === 401) {
    return "La sesion no es valida para este servicio. Volve a entrar";
  }
  if (estado === 403) {
    return "Tu usuario no tiene permiso para esta operacion";
  }
  if (estado === 404) {
    return "El servicio no reconoce esta direccion (404). Puede estar desactualizado";
  }
  if (estado === 502 || estado === 503 || estado === 504) {
    return `El servicio no esta respondiendo (${estado}). Reintenta en unos segundos`;
  }
  if (estado >= 500) {
    return `El servicio respondio con un error (${estado})`;
  }
  return `La peticion fue rechazada (${estado})`;
}
