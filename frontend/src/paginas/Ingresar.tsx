import { useId, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Logotipo } from "../componentes/Marca";
import { Boton, Campo } from "../componentes/Interfaz";
import { ErrorPanel } from "../componentes/Estados";
import { IconoCandado, IconoCheck } from "../componentes/Iconos";
import { useSesion } from "../contextos/ProveedorSesion";
import { mensajeDeError } from "../api/cliente";

const PILARES = [
  "Extracción con evidencia por campo y confianza trazable",
  "Validación por reglas versionadas, sin decisiones opacas",
  "Auditoría completa: quién decidió qué, cuándo y por qué",
];

export function Ingresar() {
  const { ingresar } = useSesion();
  const navegar = useNavigate();
  const [codigoTenant, setCodigoTenant] = useState("demo");
  const [email, setEmail] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const solicitudActiva = useRef(false);
  const id = useId();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (solicitudActiva.current) return;
    solicitudActiva.current = true;
    setError(null);
    setEnviando(true);
    try {
      await ingresar(codigoTenant.trim(), email.trim(), clave);
      navegar("/resumen");
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      solicitudActiva.current = false;
      setEnviando(false);
    }
  }

  return (
    <main className="grid min-h-dvh min-w-0 bg-superficie lg:grid-cols-[1.08fr_minmax(0,1fr)]">
      <section
        aria-label="NEXT DOC AI"
        className="hidden min-w-0 flex-col justify-between gap-espacio-12 bg-grafito p-espacio-12 text-blanco lg:flex xl:p-espacio-16"
      >
        <Logotipo claro />
        <div className="max-w-[590px]">
          <h2 className="font-titulo text-[clamp(32px,3.7vw,52px)] leading-tight">
            Tus documentos saben qué hacer después.
          </h2>
          <div
            aria-hidden="true"
            className="mt-espacio-5 h-espacio-1 w-28 rounded-insignia bg-violeta"
          />
          <p className="mt-espacio-5 text-cuerpo text-grafito-texto">
            Inteligencia documental y automatización de procesos sobre una
            plataforma propia.
          </p>
          <ul className="mt-espacio-8 space-y-espacio-4">
            {PILARES.map((pilar) => (
              <li key={pilar} className="flex items-start gap-espacio-3">
                <span
                  aria-hidden="true"
                  className="flex size-espacio-5 shrink-0 items-center justify-center rounded-insignia bg-violeta text-blanco"
                >
                  <IconoCheck tamano={13} />
                </span>
                <span className="text-pequeno">{pilar}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="flex items-center gap-espacio-2 text-pequeno text-grafito-texto">
          <span aria-hidden="true">
            <IconoCandado tamano={16} />
          </span>
          Sesión cifrada · aislamiento por organización
        </p>
      </section>

      <div className="flex min-w-0 flex-col items-center gap-espacio-8 px-espacio-6 py-espacio-8 lg:px-espacio-12 lg:py-espacio-16 xl:px-espacio-20">
        <div className="w-full max-w-[420px] lg:hidden">
          <Logotipo />
        </div>
        <p className="hidden w-full max-w-[420px] text-micro uppercase tracking-widest text-tinta-suave lg:block">
          Portal seguro · acceso empresarial
        </p>
        <div className="flex w-full max-w-[420px] flex-1 items-start sm:items-center">
          <form
            onSubmit={enviar}
            aria-labelledby={`${id}-titulo`}
            aria-describedby={
              error ? `${id}-descripcion ${id}-error` : `${id}-descripcion`
            }
            aria-busy={enviando}
            className="w-full min-w-0"
          >
            <h1
              id={`${id}-titulo`}
              className="font-titulo text-titulo-pagina text-tinta lg:text-titulo-destacado"
            >
              Ingresar al portal
            </h1>
            <p
              id={`${id}-descripcion`}
              className="mt-espacio-2 text-cuerpo text-tinta-suave"
            >
              Usá las credenciales de tu organización.
            </p>
            <div
              id={`${id}-error`}
              className="flex min-h-espacio-6 flex-col justify-center py-espacio-3"
            >
              {error ? (
                <ErrorPanel
                  titulo="No pudimos iniciar sesión"
                  mensaje={error}
                />
              ) : null}
            </div>
            <div className="space-y-espacio-4">
              <Campo
                etiqueta="Organización"
                value={codigoTenant}
                onChange={(evento) => setCodigoTenant(evento.target.value)}
                required
                disabled={enviando}
                autoComplete="off"
                placeholder="demo"
              />
              <Campo
                etiqueta="Email"
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                required
                disabled={enviando}
                autoComplete="email"
                placeholder="nombre@empresa.com"
              />
              <Campo
                etiqueta="Clave"
                type="password"
                value={clave}
                onChange={(evento) => setClave(evento.target.value)}
                required
                disabled={enviando}
                autoComplete="current-password"
                placeholder="••••••••"
              />
            </div>
            <Boton
              type="submit"
              variante="primario"
              tamano="lg"
              cargando={enviando}
              aria-label={enviando ? "Ingresando…" : "Ingresar"}
              className="mt-espacio-6 w-full"
            >
              Ingresar
            </Boton>
          </form>
        </div>
        <p role="status" aria-atomic="true" className="sr-only">
          {enviando ? "Ingresando…" : ""}
        </p>
        <footer className="w-full max-w-[420px] space-y-espacio-2 text-center text-pequeno text-tinta-suave">
          <p className="lg:hidden">
            Sesión cifrada · aislamiento por organización
          </p>
          <p>NEXT DOC AI · plataforma documental independiente</p>
        </footer>
      </div>
    </main>
  );
}
