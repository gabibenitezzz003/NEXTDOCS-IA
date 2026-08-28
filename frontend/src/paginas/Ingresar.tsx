import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Logotipo } from "../componentes/Marca";
import { useSesion } from "../contextos/ProveedorSesion";
import { mensajeDeError } from "../api/cliente";

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
    <div className="grid h-full lg:grid-cols-[1.1fr_1fr]">
      <div className="hidden flex-col justify-between bg-grafito p-12 lg:flex">
        <Logotipo claro />
        <div>
          <h2 className="font-titulo text-4xl leading-tight text-white">
            Tus documentos
            <br />
            <span className="texto-degradado">saben que hacer despues.</span>
          </h2>
          <p className="mt-5 max-w-md text-sm leading-relaxed text-white/60">
            Inteligencia documental y automatizacion de procesos. Captura, extraccion, validacion y
            gobernanza sobre una plataforma propia.
          </p>
        </div>
        <p className="text-xs text-white/35">NEXT DOC AI</p>
      </div>

      <div className="flex items-center justify-center bg-lienzo px-6 py-12">
        <form onSubmit={enviar} className="w-full max-w-sm">
          <div className="lg:hidden">
            <Logotipo />
          </div>
          <h1 className="mt-8 font-titulo text-2xl text-tinta lg:mt-0">Ingresar al portal</h1>
          <p className="mt-1 text-sm text-tinta-suave">Usa las credenciales de tu organizacion.</p>

          {error ? (
            <div
              role="alert"
              className="mt-5 rounded-lg border border-rojo/25 bg-rojo-tenue px-3.5 py-2.5 text-sm text-rojo"
            >
              {error}
            </div>
          ) : null}

          <label className="mt-5 block">
            <span className="text-sm font-medium text-tinta">Organizacion</span>
            <input
              value={codigoTenant}
              onChange={(evento) => setCodigoTenant(evento.target.value)}
              required
              autoComplete="organization"
              className="mt-1.5 w-full rounded-lg border border-borde bg-white px-3 py-2 text-sm outline-none transition focus:border-violeta focus:ring-2 focus:ring-violeta/20"
            />
          </label>

          <label className="mt-4 block">
            <span className="text-sm font-medium text-tinta">Email</span>
            <input
              type="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
              required
              autoComplete="username"
              className="mt-1.5 w-full rounded-lg border border-borde bg-white px-3 py-2 text-sm outline-none transition focus:border-violeta focus:ring-2 focus:ring-violeta/20"
            />
          </label>

          <label className="mt-4 block">
            <span className="text-sm font-medium text-tinta">Clave</span>
            <input
              type="password"
              value={clave}
              onChange={(evento) => setClave(evento.target.value)}
              required
              autoComplete="current-password"
              className="mt-1.5 w-full rounded-lg border border-borde bg-white px-3 py-2 text-sm outline-none transition focus:border-violeta focus:ring-2 focus:ring-violeta/20"
            />
          </label>

          <button
            type="submit"
            disabled={enviando}
            className="degradado-marca mt-6 w-full rounded-lg py-2.5 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
          >
            {enviando ? "Ingresando..." : "Ingresar"}
          </button>
        </form>
      </div>
    </div>
  );
}
