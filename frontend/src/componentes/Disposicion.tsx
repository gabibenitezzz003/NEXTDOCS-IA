import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Logotipo } from "./Marca";
import { useSesion } from "../contextos/ProveedorSesion";

const NAVEGACION = [
  { a: "/resumen", texto: "Resumen", permiso: "documentos.leer" },
  { a: "/panel", texto: "Panel de control", permiso: "documentos.leer" },
  { a: "/documentos", texto: "Documentos", permiso: "documentos.leer" },
  { a: "/excepciones", texto: "Excepciones", permiso: "excepciones.leer" },
  { a: "/plantillas", texto: "Plantillas", permiso: "plantillas.leer" },
];

export function Disposicion() {
  const { sesion, salir, tienePermiso } = useSesion();
  const navegar = useNavigate();

  const iniciales = (sesion?.nombre ?? "?")
    .split(" ")
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join("");

  return (
    <div className="flex h-full">
      <aside className="flex w-60 shrink-0 flex-col bg-grafito">
        <div className="border-b border-grafito-borde px-5 py-5">
          <Logotipo claro />
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {NAVEGACION.filter((entrada) => tienePermiso(entrada.permiso)).map((entrada) => (
            <NavLink
              key={entrada.a}
              to={entrada.a}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2 text-sm transition ${
                  isActive
                    ? "bg-violeta text-white shadow-[0_0_0_1px_rgba(255,255,255,0.08)]"
                    : "text-white/65 hover:bg-white/8 hover:text-white"
                }`
              }
            >
              {entrada.texto}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-grafito-borde px-4 py-4">
          <div className="flex items-center gap-3">
            <div className="degradado-marca flex size-9 shrink-0 items-center justify-center rounded-full text-xs font-semibold text-white">
              {iniciales}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm text-white">{sesion?.nombre}</p>
              <p className="truncate text-xs text-white/50">{sesion?.nombreTenant}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              salir();
              navegar("/ingresar");
            }}
            className="mt-3 w-full rounded-lg border border-white/15 px-3 py-1.5 text-xs text-white/70 transition hover:border-white/30 hover:text-white"
          >
            Cerrar sesion
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
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
  acciones?: React.ReactNode;
}) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-4 border-b border-borde bg-white px-8 py-6">
      <div>
        <h1 className="font-titulo text-2xl text-tinta">{titulo}</h1>
        {descripcion ? <p className="mt-1 text-sm text-tinta-suave">{descripcion}</p> : null}
      </div>
      {acciones ? <div className="flex items-center gap-2">{acciones}</div> : null}
    </header>
  );
}
