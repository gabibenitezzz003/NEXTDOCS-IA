import type { ComponentType, ReactNode } from "react";
import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Logotipo } from "./Marca";
import {
  IconoDocumentos,
  IconoExcepciones,
  IconoPanel,
  IconoPlantillas,
  IconoResumen,
  IconoSalir,
} from "./Iconos";
import { useSesion } from "../contextos/ProveedorSesion";

interface EntradaNavegacion {
  a: string;
  texto: string;
  permiso: string;
  icono: ComponentType<{ tamano?: number }>;
}

const GRUPOS: { titulo: string; entradas: EntradaNavegacion[] }[] = [
  {
    titulo: "Operacion",
    entradas: [
      { a: "/resumen", texto: "Resumen", permiso: "documentos.leer", icono: IconoResumen },
      { a: "/documentos", texto: "Documentos", permiso: "documentos.leer", icono: IconoDocumentos },
      { a: "/excepciones", texto: "Excepciones", permiso: "excepciones.leer", icono: IconoExcepciones },
    ],
  },
  {
    titulo: "Analisis",
    entradas: [{ a: "/panel", texto: "Panel de control", permiso: "documentos.leer", icono: IconoPanel }],
  },
  {
    titulo: "Configuracion",
    entradas: [
      { a: "/plantillas", texto: "Plantillas", permiso: "plantillas.leer", icono: IconoPlantillas },
    ],
  },
];

const TITULOS: Record<string, string> = {
  "/resumen": "Resumen",
  "/panel": "Panel de control",
  "/documentos": "Documentos",
  "/excepciones": "Excepciones",
  "/plantillas": "Plantillas",
};

export function Disposicion() {
  const { sesion, salir, tienePermiso } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();

  const iniciales = (sesion?.nombre ?? "?")
    .split(" ")
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join("");

  const grupos = GRUPOS.map((grupo) => ({
    ...grupo,
    entradas: grupo.entradas.filter((entrada) => tienePermiso(entrada.permiso)),
  })).filter((grupo) => grupo.entradas.length);

  return (
    <div className="flex h-full bg-lienzo">
      <aside className="superficie-oscura flex w-[248px] shrink-0 flex-col">
        <div className="px-5 py-5">
          <Logotipo claro />
        </div>

        <nav className="barra-desplazamiento-fina flex-1 space-y-6 overflow-y-auto px-3 pb-4">
          {grupos.map((grupo) => (
            <div key={grupo.titulo}>
              <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/30">
                {grupo.titulo}
              </p>
              <div className="space-y-0.5">
                {grupo.entradas.map((entrada) => {
                  const Icono = entrada.icono;
                  return (
                    <NavLink
                      key={entrada.a}
                      to={entrada.a}
                      className={({ isActive }) =>
                        `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition ${
                          isActive
                            ? "bg-violeta font-semibold text-white shadow-violeta"
                            : "font-medium text-white/60 hover:bg-white/[0.07] hover:text-white"
                        }`
                      }
                    >
                      {({ isActive }) => (
                        <>
                          <span className={isActive ? "text-white" : "text-white/45 group-hover:text-white/80"}>
                            <Icono tamano={18} />
                          </span>
                          {entrada.texto}
                        </>
                      )}
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <MenuUsuario
          iniciales={iniciales}
          nombre={sesion?.nombre ?? ""}
          organizacion={sesion?.nombreTenant ?? ""}
          email={sesion?.email ?? ""}
          alSalir={() => {
            salir();
            navegar("/ingresar");
          }}
        />
      </aside>

      <main className="barra-desplazamiento-fina flex-1 overflow-y-auto">
        <div key={ubicacion.pathname} className="aparecer min-h-full">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

function MenuUsuario({
  iniciales,
  nombre,
  organizacion,
  email,
  alSalir,
}: {
  iniciales: string;
  nombre: string;
  organizacion: string;
  email: string;
  alSalir: () => void;
}) {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!abierto) {
      return;
    }
    function alClicFuera(evento: MouseEvent) {
      if (contenedor.current && !contenedor.current.contains(evento.target as Node)) {
        setAbierto(false);
      }
    }
    document.addEventListener("mousedown", alClicFuera);
    return () => document.removeEventListener("mousedown", alClicFuera);
  }, [abierto]);

  return (
    <div ref={contenedor} className="relative border-t border-white/8 p-3">
      {abierto ? (
        <div className="aparecer absolute bottom-full left-3 right-3 mb-2 overflow-hidden rounded-xl border border-borde bg-white shadow-flotante">
          <div className="border-b border-borde px-3.5 py-3">
            <p className="truncate text-sm font-semibold text-tinta">{nombre}</p>
            <p className="truncate text-xs text-tinta-suave">{email}</p>
          </div>
          <button
            type="button"
            onClick={alSalir}
            className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-sm font-medium text-tinta transition hover:bg-rojo-tenue hover:text-rojo"
          >
            <IconoSalir tamano={16} />
            Cerrar sesion
          </button>
        </div>
      ) : null}

      <button
        type="button"
        onClick={() => setAbierto((previo) => !previo)}
        aria-expanded={abierto}
        className={`flex w-full items-center gap-3 rounded-xl px-2 py-2 text-left transition ${
          abierto ? "bg-white/[0.08]" : "hover:bg-white/[0.06]"
        }`}
      >
        <span className="degradado-marca flex size-9 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white">
          {iniciales}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-white">{nombre}</span>
          <span className="block truncate text-xs text-white/45">{organizacion}</span>
        </span>
      </button>
    </div>
  );
}

export function Encabezado({
  titulo,
  descripcion,
  acciones,
}: {
  titulo: string;
  descripcion?: string;
  acciones?: ReactNode;
}) {
  const ubicacion = useLocation();
  const seccion = TITULOS[ubicacion.pathname];

  return (
    <header className="sticky top-0 z-20 border-b border-borde bg-white/85 px-8 py-5 backdrop-blur-xl">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0">
          {seccion && seccion !== titulo ? (
            <p className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-tinta-tenue">
              {seccion}
            </p>
          ) : null}
          <h1 className="font-titulo text-[26px] leading-tight text-tinta">{titulo}</h1>
          {descripcion ? (
            <p className="mt-1 max-w-2xl text-sm text-tinta-suave">{descripcion}</p>
          ) : null}
        </div>
        {acciones ? <div className="flex flex-wrap items-center gap-2">{acciones}</div> : null}
      </div>
    </header>
  );
}

export function Contenido({ children }: { children: ReactNode }) {
  return <div className="px-8 py-6">{children}</div>;
}
