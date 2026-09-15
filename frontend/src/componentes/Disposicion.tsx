import type { ComponentType, ReactNode } from "react";
import { useEffect, useId, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Isotipo, Logotipo } from "./Marca";
import { Boton, BotonIcono } from "./Interfaz";
import {
  IconoDocumentos,
  IconoExcepciones,
  IconoInfo,
  IconoPanel,
  IconoProceso,
  IconoResumen,
  IconoSalir,
  IconoCerrar,
  IconoFiltro,
} from "./Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { useSesion } from "../contextos/ProveedorSesion";
import { SelectorIdioma } from "./Idioma";
import { AlternarTema } from "./Tema";

interface EntradaNavegacion {
  a: string;
  texto: string;
  permiso: string;
  icono: ComponentType<{ tamano?: number }>;
}

const GRUPOS: { titulo: string; entradas: EntradaNavegacion[] }[] = [
  {
    titulo: "disposicion.grupoOperacion",
    entradas: [
      {
        a: "/resumen",
        texto: "disposicion.navResumen",
        permiso: "documentos.leer",
        icono: IconoResumen,
      },
      {
        a: "/documentos",
        texto: "disposicion.navDocumentos",
        permiso: "documentos.leer",
        icono: IconoDocumentos,
      },
      {
        a: "/excepciones",
        texto: "disposicion.navExcepciones",
        permiso: "excepciones.leer",
        icono: IconoExcepciones,
      },
    ],
  },
  {
    titulo: "disposicion.grupoProcesos",
    entradas: [
      {
        a: "/procesos",
        texto: "disposicion.navProcesos",
        permiso: "tenant.administrar",
        icono: IconoProceso,
      },
    ],
  },
  {
    titulo: "disposicion.grupoAnalisis",
    entradas: [
      {
        a: "/panel",
        texto: "disposicion.navPanel",
        permiso: "documentos.leer",
        icono: IconoPanel,
      },
    ],
  },
  {
    titulo: "disposicion.grupoConfiguracion",
    entradas: [
      {
        a: "/tipos-propuestos",
        texto: "disposicion.navTiposNuevos",
        permiso: "tenant.administrar",
        icono: IconoInfo,
      },
    ],
  },
];

function tituloDeSeccion(ruta: string) {
  return GRUPOS.flatMap((grupo) => grupo.entradas).find(
    (entrada) => ruta === entrada.a || ruta.startsWith(`${entrada.a}/`),
  )?.texto;
}

export function Disposicion() {
  const { sesion, salir, tienePermiso } = useSesion();
  const { t } = useIdioma();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const dialogo = useRef<HTMLDialogElement>(null);
  const [navegacionAbierta, setNavegacionAbierta] = useState(false);
  const idNavegacion = useId();
  const claveSeccion = tituloDeSeccion(ubicacion.pathname);
  const seccion = claveSeccion ? t(claveSeccion) : undefined;

  useEffect(() => {
    dialogo.current?.close();
  }, [ubicacion.pathname]);

  useEffect(() => {
    const escritorio = window.matchMedia("(min-width: 48rem)");
    function alCambiar() {
      if (escritorio.matches) dialogo.current?.close();
    }
    escritorio.addEventListener("change", alCambiar);
    return () => escritorio.removeEventListener("change", alCambiar);
  }, []);

  useEffect(() => {
    if (!navegacionAbierta) return;
    const anterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = anterior;
    };
  }, [navegacionAbierta]);

  const iniciales = (sesion?.nombre ?? "?")
    .split(" ")
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join("");

  const grupos = GRUPOS.map((grupo) => ({
    ...grupo,
    entradas: grupo.entradas.filter((entrada) => tienePermiso(entrada.permiso)),
  })).filter((grupo) => grupo.entradas.length);

  const identidad = {
    iniciales,
    nombre: sesion?.nombre ?? "",
    organizacion: sesion?.nombreTenant ?? sesion?.codigoTenant ?? "",
    email: sesion?.email ?? "",
  };

  return (
    <div className="grid min-h-dvh min-w-0 grid-cols-[minmax(0,1fr)] bg-lienzo md:grid-cols-[var(--ancho-barra-lateral-compacta)_minmax(0,1fr)] xl:grid-cols-[var(--layout-sidebar-desktop)_minmax(0,1fr)]">
      <a
        href="#contenido-principal"
        className="sr-only z-50 rounded-control bg-superficie p-espacio-3 text-tinta focus:not-sr-only focus:fixed focus:left-espacio-4 focus:top-espacio-4"
      >
        {t("disposicion.irAlContenido")}
      </a>
      <aside
        aria-label={t("disposicion.barraLateral")}
        className="sticky top-0 hidden h-dvh min-h-0 flex-col bg-superficie-navegacion text-blanco md:flex"
      >
        <div className="flex h-(--layout-topbar-height) shrink-0 items-center justify-center border-b border-borde-navegacion xl:justify-start xl:px-espacio-4">
          <div className="hidden xl:block">
            <Logotipo claro />
          </div>
          <div className="xl:hidden" role="img" aria-label="NEXT DOC AI">
            <Isotipo claro />
          </div>
        </div>
        <Navegacion grupos={grupos} compactable />
        <IdentidadLateral {...identidad} compactable />
      </aside>
      <div className="flex min-w-0 flex-col">
        <header
          aria-label={t("disposicion.barraSuperior")}
          className="sticky top-0 z-30 flex h-(--layout-topbar-height) shrink-0 items-center justify-between gap-espacio-3 border-b border-borde bg-superficie px-espacio-4 md:px-espacio-8"
        >
          <div className="flex min-w-0 items-center gap-espacio-3">
            <BotonIcono
              variante="secundario"
              className="md:hidden"
              aria-label={t("disposicion.abrirNavegacion")}
              aria-expanded={navegacionAbierta}
              aria-controls={idNavegacion}
              aria-haspopup="dialog"
              onClick={() => {
                dialogo.current?.showModal();
                setNavegacionAbierta(true);
              }}
            >
              <IconoFiltro />
            </BotonIcono>
            <div className="min-w-0">
              <p className="truncate font-titulo text-pequeno font-bold text-tinta">
                {seccion ?? "NEXT DOC AI"}
              </p>
              {identidad.organizacion ? (
                <p
                  className="truncate text-pequeno text-tinta-suave"
                  title={identidad.organizacion}
                >
                  {identidad.organizacion}
                </p>
              ) : null}
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-espacio-2">
            <SelectorIdioma />
            <AlternarTema />
            <MenuUsuario
              {...identidad}
              alSalir={() => {
                salir();
                navegar("/ingresar");
              }}
            />
          </div>
        </header>
        <main
          id="contenido-principal"
          tabIndex={-1}
          className="min-w-0 flex-1 overflow-x-auto"
        >
          <div key={ubicacion.pathname} className="aparecer min-w-0">
            <Outlet />
          </div>
        </main>
      </div>
      <dialog
        ref={dialogo}
        id={idNavegacion}
        aria-label={t("disposicion.navegacionMovil")}
        onClose={() => setNavegacionAbierta(false)}
        onClick={(evento) => {
          if (evento.target === evento.currentTarget) dialogo.current?.close();
        }}
        className="fixed inset-y-0 left-0 m-0 h-dvh max-h-none w-(--layout-sidebar-desktop) max-w-[calc(100%-var(--spacing-espacio-8))] border-0 bg-superficie-navegacion p-0 text-blanco shadow-panel-lateral backdrop:bg-grafito/50"
      >
        <div className="flex h-full min-h-0 flex-col">
          <div className="flex h-(--layout-topbar-height) shrink-0 items-center justify-between gap-espacio-2 border-b border-borde-navegacion px-espacio-3">
            <span className="font-titulo text-pequeno font-bold">
              NEXT DOC AI
            </span>
            <BotonIcono
              variante="secundario"
              aria-label={t("disposicion.cerrarNavegacion")}
              onClick={() => dialogo.current?.close()}
            >
              <IconoCerrar />
            </BotonIcono>
          </div>
          <Navegacion
            grupos={grupos}
            alNavegar={() => dialogo.current?.close()}
          />
          <IdentidadLateral {...identidad} />
        </div>
      </dialog>
    </div>
  );
}

function Navegacion({
  grupos,
  compactable = false,
  alNavegar,
}: {
  grupos: typeof GRUPOS;
  compactable?: boolean;
  alNavegar?: () => void;
}) {
  const { t } = useIdioma();
  return (
    <nav
      aria-label={t("disposicion.navegacionPrincipal")}
      className="barra-desplazamiento-fina min-h-0 flex-1 space-y-espacio-6 overflow-y-auto px-espacio-2 py-espacio-6"
    >
      {grupos.map((grupo) => (
        <div key={grupo.titulo}>
          <p
            className={`mb-espacio-2 px-espacio-2 text-micro uppercase tracking-wider text-texto-navegacion-secundario ${compactable ? "sr-only xl:not-sr-only" : ""}`}
          >
            {t(grupo.titulo)}
          </p>
          <ul className="space-y-espacio-1">
            {grupo.entradas.map((entrada) => {
              const Icono = entrada.icono;
              const texto = t(entrada.texto);
              return (
                <li key={entrada.a}>
                  <NavLink
                    to={entrada.a}
                    title={texto}
                    onClick={alNavegar}
                    className={({ isActive }) =>
                      `flex min-h-control-mediano items-center gap-espacio-2 rounded-control px-espacio-2 py-espacio-2 text-pequeno transition-colors focus-visible:outline-violeta-claro ${compactable ? "justify-center xl:justify-start" : ""} ${isActive ? "bg-accion-primaria font-semibold text-blanco" : "text-texto-navegacion-secundario hover:bg-superficie-navegacion-activa hover:text-blanco"}`
                    }
                  >
                    <span aria-hidden="true" className="shrink-0">
                      <Icono tamano={18} />
                    </span>
                    <span
                      className={compactable ? "sr-only xl:not-sr-only" : ""}
                    >
                      {texto}
                    </span>
                  </NavLink>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );
}

function IdentidadLateral({
  iniciales,
  nombre,
  organizacion,
  compactable = false,
}: {
  iniciales: string;
  nombre: string;
  organizacion: string;
  compactable?: boolean;
}) {
  return (
    <div
      className={`flex shrink-0 items-center gap-espacio-2 border-t border-borde-navegacion px-espacio-3 py-espacio-4 ${compactable ? "justify-center xl:justify-start" : ""}`}
      title={[nombre, organizacion].filter(Boolean).join(" · ")}
    >
      <span
        aria-hidden="true"
        className="flex size-espacio-8 shrink-0 items-center justify-center rounded-insignia bg-superficie-navegacion-activa text-pequeno font-semibold text-blanco"
      >
        {iniciales}
      </span>
      <div className={`min-w-0 ${compactable ? "sr-only xl:not-sr-only" : ""}`}>
        <p className="truncate text-pequeno font-semibold text-blanco">
          {nombre}
        </p>
        {organizacion ? (
          <p className="truncate text-micro text-texto-navegacion-secundario">
            {organizacion}
          </p>
        ) : null}
      </div>
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
  const disparador = useRef<HTMLDivElement>(null);
  const id = useId();
  const ubicacion = useLocation();
  const { t } = useIdioma();

  function cerrarConFoco() {
    setAbierto(false);
    disparador.current?.querySelector("button")?.focus();
  }

  useEffect(() => {
    setAbierto(false);
  }, [ubicacion.pathname]);

  useEffect(() => {
    if (!abierto) {
      return;
    }
    function alClicFuera(evento: MouseEvent) {
      if (
        contenedor.current &&
        !contenedor.current.contains(evento.target as Node)
      ) {
        setAbierto(false);
      }
    }
    document.addEventListener("mousedown", alClicFuera);
    return () => document.removeEventListener("mousedown", alClicFuera);
  }, [abierto]);

  return (
    <div
      ref={contenedor}
      className="relative shrink-0"
      onKeyDown={(evento) => {
        if (evento.key === "Escape" && abierto) {
          evento.preventDefault();
          evento.stopPropagation();
          cerrarConFoco();
        }
      }}
      onBlur={(evento) => {
        if (!evento.currentTarget.contains(evento.relatedTarget))
          setAbierto(false);
      }}
    >
      <div ref={disparador}>
        <Boton
          type="button"
          variante="secundario"
          aria-label={nombre ? t("disposicion.menuUsuarioNombre", { nombre }) : t("disposicion.menuUsuario")}
          aria-expanded={abierto}
          aria-controls={id}
          onClick={() => setAbierto((previo) => !previo)}
        >
          <span
            aria-hidden="true"
            className="flex size-espacio-6 items-center justify-center rounded-insignia bg-violeta-tenue text-micro font-bold text-accion-tonal-texto"
          >
            {iniciales}
          </span>
          <span className="hidden max-w-40 truncate sm:inline">
            {nombre || email}
          </span>
        </Boton>
      </div>
      {abierto ? (
        <div
          id={id}
          role="region"
          aria-label={t("disposicion.opcionesUsuario")}
          className="absolute right-0 top-full mt-espacio-2 w-72 max-w-[calc(100vw-var(--spacing-espacio-8))] rounded-panel border border-borde bg-superficie p-espacio-4 shadow-superficie-elevada"
        >
          <div className="mb-espacio-3 border-b border-borde pb-espacio-3">
            <p className="text-pequeno font-semibold text-tinta break-words">
              {nombre}
            </p>
            {email ? (
              <p className="text-pequeno text-tinta-suave break-all">{email}</p>
            ) : null}
            {organizacion ? (
              <p className="mt-espacio-1 text-pequeno text-tinta-suave break-words">
                {organizacion}
              </p>
            ) : null}
          </div>
          <div className="flex flex-wrap gap-espacio-2">
            <Boton
              type="button"
              variante="peligro"
              tamano="sm"
              onClick={alSalir}
            >
              <IconoSalir tamano={16} />
              {t("comun.cerrarSesion")}
            </Boton>
            <Boton type="button" tamano="sm" onClick={cerrarConFoco}>
              {t("comun.cerrarMenu")}
            </Boton>
          </div>
        </div>
      ) : null}
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
  const { t } = useIdioma();
  const claveSeccion = tituloDeSeccion(ubicacion.pathname);
  const seccion = claveSeccion ? t(claveSeccion) : undefined;

  return (
    <header className="min-w-0 px-espacio-4 py-espacio-5 md:px-espacio-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0">
          {seccion && seccion !== titulo ? (
            <p className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-neutro-texto">
              {seccion}
            </p>
          ) : null}
          <h1 className="font-titulo text-titulo-pagina text-tinta break-words">
            {titulo}
          </h1>
          {descripcion ? (
            <p className="mt-1 max-w-2xl text-sm text-tinta-suave">
              {descripcion}
            </p>
          ) : null}
        </div>
        {acciones ? (
          <div className="flex flex-wrap items-center gap-2">{acciones}</div>
        ) : null}
      </div>
    </header>
  );
}

export function Contenido({ children }: { children: ReactNode }) {
  return (
    <div className="min-w-0 px-espacio-4 py-espacio-6 md:px-espacio-8">
      {children}
    </div>
  );
}
