import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";

export type Tema = "light" | "dark";

const CLAVE = "nd-tema";

interface ContextoTema {
  tema: Tema;
  alternar: () => void;
  fijar: (tema: Tema) => void;
}

const Contexto = createContext<ContextoTema | null>(null);

function esTema(valor: string | null): valor is Tema {
  return valor === "light" || valor === "dark";
}

/*
 * El tema inicial ya lo aplicó el script de index.html sobre <html>, así que se
 * lee de ahí en vez de recalcularlo: evita que el primer render discrepe del
 * atributo que el navegador usó para pintar.
 */
function temaInicial(): Tema {
  if (typeof document === "undefined") return "light";
  const aplicado = document.documentElement.getAttribute("data-nd-theme");
  return esTema(aplicado) ? aplicado : "light";
}

export function ProveedorTema({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(temaInicial);

  useEffect(() => {
    document.documentElement.setAttribute("data-nd-theme", tema);
    try {
      localStorage.setItem(CLAVE, tema);
    } catch {
      // Modo privado o almacenamiento bloqueado: el tema vale para esta sesión.
    }
  }, [tema]);

  // Sólo se sigue al sistema mientras la persona no haya elegido explícitamente.
  useEffect(() => {
    let elegido = false;
    try {
      elegido = esTema(localStorage.getItem(CLAVE));
    } catch {
      elegido = false;
    }
    if (elegido) return;
    const consulta = window.matchMedia("(prefers-color-scheme: dark)");
    function alCambiar(evento: MediaQueryListEvent) {
      setTema(evento.matches ? "dark" : "light");
    }
    consulta.addEventListener("change", alCambiar);
    return () => consulta.removeEventListener("change", alCambiar);
  }, []);

  const fijar = useCallback((siguiente: Tema) => setTema(siguiente), []);
  const alternar = useCallback(
    () => setTema((previo) => (previo === "dark" ? "light" : "dark")),
    [],
  );

  const valor = useMemo(() => ({ tema, alternar, fijar }), [tema, alternar, fijar]);

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useTema() {
  const contexto = useContext(Contexto);
  if (!contexto) throw new Error("useTema requiere ProveedorTema");
  return contexto;
}
