import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useSesion } from "./ProveedorSesion";
import { cliente } from "../api/cliente";
import {
  esIdioma,
  fijarIdiomaActual,
  idiomaVigente,
  traducir,
  type Idioma,
} from "../i18n";

interface ContextoIdioma {
  idioma: Idioma;
  cambiarIdioma: (idioma: Idioma) => void;
  t: (ruta: string, params?: Record<string, string | number>) => string;
}

const Contexto = createContext<ContextoIdioma | null>(null);

export function ProveedorIdioma({ children }: { children: ReactNode }) {
  const { sesion } = useSesion();
  const [idioma, setIdioma] = useState<Idioma>(idiomaVigente());

  useEffect(() => {
    if (esIdioma(sesion?.idioma)) setIdioma(sesion.idioma);
  }, [sesion?.idioma]);

  useEffect(() => {
    fijarIdiomaActual(idioma);
    document.documentElement.lang = idioma;
  }, [idioma]);

  const cambiarIdioma = useCallback(
    (nuevo: Idioma) => {
      setIdioma(nuevo);
      if (sesion) {
        void cliente.put("/administracion/perfil/idioma", { idioma: nuevo }).catch(() => {});
      }
    },
    [sesion],
  );

  const t = useCallback(
    (ruta: string, params?: Record<string, string | number>) => traducir(ruta, params, idioma),
    [idioma],
  );

  const valor = useMemo<ContextoIdioma>(
    () => ({ idioma, cambiarIdioma, t }),
    [idioma, cambiarIdioma, t],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useIdioma(): ContextoIdioma {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error("useIdioma debe usarse dentro de ProveedorIdioma");
  }
  return contexto;
}
