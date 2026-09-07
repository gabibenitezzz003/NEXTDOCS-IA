import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import axios from "axios";
import {
  cliente,
  fijarTokenAcceso,
  guardarTokenRefresco,
  leerTokenRefresco,
  registrarExpiracion,
} from "../api/cliente";
import { fijarTenantProcesos } from "../api/procesos";
import type { Sesion } from "../tipos/api";

interface ContextoSesion {
  sesion: Sesion | null;
  cargando: boolean;
  ingresar: (codigoTenant: string, email: string, clave: string) => Promise<void>;
  salir: () => void;
  tienePermiso: (permiso: string) => boolean;
}

const Contexto = createContext<ContextoSesion | null>(null);

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [cargando, setCargando] = useState(true);

  const salir = useCallback(() => {
    fijarTokenAcceso(null);
    fijarTenantProcesos(null);
    guardarTokenRefresco(null);
    setSesion(null);
  }, []);

  useEffect(() => {
    registrarExpiracion(salir);
  }, [salir]);

  useEffect(() => {
    const tokenRefresco = leerTokenRefresco();
    if (!tokenRefresco) {
      setCargando(false);
      return;
    }
    axios
      .post<Sesion>("/api/v1/autenticacion/refrescar", { tokenRefresco })
      .then(({ data }) => {
        fijarTokenAcceso(data.tokenAcceso);
        fijarTenantProcesos(data.tenantId);
        guardarTokenRefresco(data.tokenRefresco);
        setSesion(data);
      })
      .catch(() => guardarTokenRefresco(null))
      .finally(() => setCargando(false));
  }, []);

  const ingresar = useCallback(async (codigoTenant: string, email: string, clave: string) => {
    const { data } = await cliente.post<Sesion>("/autenticacion/ingresar", {
      codigoTenant,
      email,
      clave,
    });
    fijarTokenAcceso(data.tokenAcceso);
    fijarTenantProcesos(data.tenantId);
    guardarTokenRefresco(data.tokenRefresco);
    setSesion(data);
  }, []);

  const valor = useMemo<ContextoSesion>(
    () => ({
      sesion,
      cargando,
      ingresar,
      salir,
      tienePermiso: (permiso: string) => sesion?.permisos.includes(permiso) ?? false,
    }),
    [sesion, cargando, ingresar, salir],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useSesion(): ContextoSesion {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error("useSesion debe usarse dentro de ProveedorSesion");
  }
  return contexto;
}
