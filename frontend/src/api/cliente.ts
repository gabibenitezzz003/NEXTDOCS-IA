import axios, { AxiosError, type AxiosInstance } from "axios";
import { idiomaVigente, traducir } from "../i18n";
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
  configuracion.headers["Accept-Language"] = idiomaVigente();
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
        ? traducir("errores.limitePeticionesReintenta", { espera })
        : traducir("errores.limitePeticiones");
    }
    const cuerpo = error.response?.data;
    if (cuerpo?.campos) {
      return Object.entries(cuerpo.campos)
        .map(([campo, detalle]) => `${campo}: ${detalle}`)
        .join(". ");
    }
    if (cuerpo?.mensaje) {
      const detalles = (cuerpo.detalles ?? [])
        .filter((detalle) => typeof detalle === "string" && detalle.trim())
        .slice(0, 4);
      return detalles.length
        ? `${cuerpo.mensaje} ${detalles.join(" · ")}`
        : cuerpo.mensaje;
    }
    if (!error.response) {
      return traducir("errores.sinConexionServidor");
    }
    return porEstado(error.response.status);
  }
  if (error instanceof Error && error.message) {
    return traducir(error.message);
  }
  return traducir("errores.inesperado");
}

export function tituloDeError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) return traducir("errores.tituloSinConexion");
    const estado = error.response.status;
    if (estado === 401) return traducir("errores.tituloSesionExpirada");
    if (estado === 403) return traducir("errores.tituloSinPermiso");
    if (estado === 404) return traducir("errores.tituloNoEncontrado");
    if (estado === 409) return traducir("errores.tituloConflicto");
    if (estado === 429) return traducir("errores.tituloDemasiadasPeticiones");
    if (estado >= 500) return traducir("errores.tituloErrorServidor");
    if (estado >= 400) return traducir("errores.tituloPeticionRechazada");
  }
  return traducir("errores.tituloInesperado");
}

function porEstado(estado: number): string {
  if (estado === 401) {
    return traducir("errores.sesionInvalida");
  }
  if (estado === 403) {
    return traducir("errores.sinPermiso");
  }
  if (estado === 404) {
    return traducir("errores.direccionDesconocida");
  }
  if (estado === 502 || estado === 503 || estado === 504) {
    return traducir("errores.servicioNoResponde", { estado });
  }
  if (estado >= 500) {
    return traducir("errores.errorServicio", { estado });
  }
  return traducir("errores.peticionRechazada", { estado });
}
