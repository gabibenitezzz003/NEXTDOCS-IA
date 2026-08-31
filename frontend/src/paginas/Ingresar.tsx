import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Logotipo } from "../componentes/Marca";
import { Boton, Campo } from "../componentes/Interfaz";
import { IconoCandado, IconoCheck, IconoInfo } from "../componentes/Iconos";
import { useSesion } from "../contextos/ProveedorSesion";
import { mensajeDeError } from "../api/cliente";

const PILARES = [
  "Extraccion con evidencia por campo y confianza trazable",
  "Validacion por reglas versionadas, sin decisiones opacas",
  "Auditoria completa: quien decidio que, cuando y por que",
];

export function Ingresar() {
  const { ingresar } = useSesion();
  const navegar = useNavigate();
  const [codigoTenant, setCodigoTenant] = useState("demo");
  const [email, setEmail] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await ingresar(codigoTenant.trim(), email.trim(), clave);
      navegar("/resumen");
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="grid h-full lg:grid-cols-[1.08fr_1fr]">
      <div className="superficie-oscura relative hidden flex-col justify-between overflow-hidden p-12 lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.09]"
          style={{
            backgroundImage:
              "linear-gradient(rgba(255,255,255,.6) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.6) 1px, transparent 1px)",
            backgroundSize: "56px 56px",
            maskImage: "radial-gradient(70% 60% at 30% 40%, #000 0%, transparent 100%)",
          }}
        />

        <div className="relative">
          <Logotipo claro tamano={44} escala={1.45} />
        </div>

        <div className="relative max-w-lg">
          <h2 className="font-titulo text-[clamp(38px,3.7vw,60px)] leading-[1.04] text-white">
            Tus documentos
            <br />
            <span className="texto-degradado">saben que hacer despues.</span>
          </h2>
          <p className="mt-7 max-w-md text-[17px] leading-relaxed text-white/55">
            Inteligencia documental y automatizacion de procesos sobre una plataforma propia.
          </p>

          <ul className="mt-10 space-y-4">
            {PILARES.map((pilar) => (
              <li key={pilar} className="flex items-start gap-3">
                <span className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full bg-violeta/25 text-violeta-claro">
                  <IconoCheck tamano={13} />
                </span>
                <span className="text-[15px] leading-relaxed text-white/70">{pilar}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="relative flex items-center gap-2 text-xs text-white/30">
          <IconoCandado tamano={14} />
          Sesion cifrada · aislamiento por organizacion
        </div>
      </div>

      <div className="flex items-center justify-center bg-white px-6 py-12">
        <form onSubmit={enviar} className="w-full max-w-[420px]">
          <div className="lg:hidden">
            <Logotipo tamano={40} escala={1.3} />
          </div>

          <h1 className="mt-8 font-titulo text-[34px] leading-tight text-tinta lg:mt-0">
            Ingresar al portal
          </h1>
          <p className="mt-2 text-[15px] text-tinta-suave">Usa las credenciales de tu organizacion.</p>

          {error ? (
            <div
              role="alert"
              className="aparecer mt-6 flex items-start gap-2.5 rounded-xl border border-rojo-borde bg-rojo-tenue px-3.5 py-3 text-sm text-rojo"
            >
              <span className="mt-px shrink-0">
                <IconoInfo tamano={16} />
              </span>
              {error}
            </div>
          ) : null}

          <div className="mt-6 space-y-4">
            <Campo
              etiqueta="Organizacion"
              value={codigoTenant}
              onChange={(evento) => setCodigoTenant(evento.target.value)}
              required
              autoComplete="organization"
              placeholder="demo"
            />
            <Campo
              etiqueta="Email"
              type="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
              required
              autoComplete="username"
              placeholder="nombre@empresa.com"
            />
            <Campo
              etiqueta="Clave"
              type="password"
              value={clave}
              onChange={(evento) => setClave(evento.target.value)}
              required
              autoComplete="current-password"
              placeholder="••••••••"
            />
          </div>

          <Boton
            type="submit"
            variante="primario"
            tamano="lg"
            disabled={enviando}
            className="mt-7 w-full"
          >
            {enviando ? "Ingresando..." : "Ingresar"}
          </Boton>

          <p className="mt-6 text-center text-xs text-tinta-tenue">
            NEXT DOC AI · plataforma documental independiente
          </p>
        </form>
      </div>
    </div>
  );
}
