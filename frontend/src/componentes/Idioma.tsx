import { useEffect, useId, useRef, useState } from "react";
import { Boton } from "./Interfaz";
import { IconoCheck } from "./Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { IDIOMAS, NOMBRES_IDIOMA, type Idioma } from "../i18n";

export function SelectorIdioma({ className = "" }: { className?: string }) {
  const { idioma, cambiarIdioma, t } = useIdioma();
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);
  const id = useId();

  useEffect(() => {
    if (!abierto) return;
    function alClicFuera(evento: MouseEvent) {
      if (contenedor.current && !contenedor.current.contains(evento.target as Node)) {
        setAbierto(false);
      }
    }
    document.addEventListener("mousedown", alClicFuera);
    return () => document.removeEventListener("mousedown", alClicFuera);
  }, [abierto]);

  function elegir(nuevo: Idioma) {
    setAbierto(false);
    cambiarIdioma(nuevo);
  }

  return (
    <div
      ref={contenedor}
      className={`relative shrink-0 ${className}`}
      onKeyDown={(evento) => {
        if (evento.key === "Escape" && abierto) {
          evento.preventDefault();
          evento.stopPropagation();
          setAbierto(false);
        }
      }}
      onBlur={(evento) => {
        if (!evento.currentTarget.contains(evento.relatedTarget)) setAbierto(false);
      }}
    >
      <Boton
        type="button"
        variante="secundario"
        aria-label={t("comun.idioma")}
        aria-expanded={abierto}
        aria-controls={id}
        aria-haspopup="listbox"
        onClick={() => setAbierto((previo) => !previo)}
      >
        <span aria-hidden="true" className="text-micro font-bold uppercase">
          {idioma}
        </span>
        <span className="hidden sm:inline">{NOMBRES_IDIOMA[idioma]}</span>
      </Boton>
      {abierto ? (
        <ul
          id={id}
          role="listbox"
          aria-label={t("comun.idioma")}
          className="absolute right-0 top-full z-50 mt-espacio-2 w-44 rounded-panel border border-borde bg-superficie py-espacio-1 shadow-superficie-elevada"
        >
          {IDIOMAS.map((opcion) => (
            <li
              key={opcion}
              role="option"
              aria-selected={opcion === idioma}
              onClick={() => elegir(opcion)}
              onKeyDown={(evento) => {
                if (evento.key === "Enter" || evento.key === " ") {
                  evento.preventDefault();
                  elegir(opcion);
                }
              }}
              tabIndex={0}
              className={`flex cursor-pointer items-center justify-between gap-espacio-2 px-espacio-3 py-espacio-2 text-pequeno ${
                opcion === idioma
                  ? "bg-violeta-tenue font-semibold text-accion-tonal-texto"
                  : "text-tinta hover:bg-lienzo"
              }`}
            >
              {NOMBRES_IDIOMA[opcion]}
              {opcion === idioma ? <IconoCheck tamano={14} /> : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
