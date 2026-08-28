import axios, { AxiosError } from "axios";
import type { ErrorApi, Sesion } from "../tipos/api";

const CLAVE_REFRESCO = "nextdocs.tokenRefresco";

let tokenAcceso: string | null = null;
let alExpirarSesion: (() => void) | null = null;

export const cliente = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
});

export function fijarTokenAcceso(token: string | null) {
  tokenAcceso = token;
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
  if (tokenAcceso) {
    configuracion.headers.Authorization = `Bearer ${tokenAcceso}`;
  }
  return configuracion;
});

let refrescoEnCurso: Promise<string | null> | null = null;

async function refrescar(): Promise<string | null> {
  const tokenRefresco = leerTokenRefresco();
  if (!tokenRefresco) {
    return null;
  }
  try {
    const { data } = await axios.post<Sesion>("/api/v1/autenticacion/refrescar", { tokenRefresco });
    fijarTokenAcceso(data.tokenAcceso);
    guardarTokenRefresco(data.tokenRefresco);
    return data.tokenAcceso;
  } catch {
    return null;
  }
}

cliente.interceptors.response.use(
  (respuesta) => respuesta,
  async (error: AxiosError<ErrorApi>) => {
    const original = error.config as (typeof error.config & { _reintentado?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._reintentado) {
      original._reintentado = true;
      refrescoEnCurso = refrescoEnCurso ?? refrescar();
      const nuevo = await refrescoEnCurso;
      refrescoEnCurso = null;
      if (nuevo) {
        original.headers.Authorization = `Bearer ${nuevo}`;
        return cliente(original);
      }
      alExpirarSesion?.();
    }
    return Promise.reject(error);
  },
);

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
  }
  return "Ocurrio un error inesperado";
}
