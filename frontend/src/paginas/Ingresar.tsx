import { useEffect, useId, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Logotipo } from "../componentes/Marca";
import { Boton, Campo } from "../componentes/Interfaz";
import { ErrorPanel } from "../componentes/Estados";
import { IconoCandado, IconoCheck, IconoRecargar } from "../componentes/Iconos";
import { AlternarTema } from "../componentes/Tema";
import { useSesion } from "../contextos/ProveedorSesion";
import { mensajeDeError } from "../api/cliente";
import { listarProveedoresOauth, urlInicioOauth } from "../api/federacion";

const PILARES = [
  "Extracción con evidencia por campo y confianza trazable",
  "Validación por reglas versionadas, sin decisiones opacas",
  "Auditoría completa: quién decidió qué, cuándo y por qué",
];

export function Ingresar() {
  const { ingresar, ingresarConCodigo } = useSesion();
  const navegar = useNavigate();
  const [parametros, fijarParametros] = useSearchParams();
  const [codigoTenant, setCodigoTenant] = useState("demo");
  const [email, setEmail] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [canjeando, setCanjeando] = useState(false);
  const solicitudActiva = useRef(false);
  const canjeActivo = useRef(false);
  const id = useId();

  const tenantLimpio = codigoTenant.trim();

  const { data: proveedoresOauth } = useQuery({
    queryKey: ["proveedores-oauth", tenantLimpio.toLowerCase()],
    queryFn: () => listarProveedoresOauth(tenantLimpio),
    enabled: tenantLimpio.length > 0,
    staleTime: 60_000,
    retry: false,
  });

  useEffect(() => {
    const codigo = parametros.get("codigo");
    const errorFederado = parametros.get("errorFederado");
    if (!codigo && !errorFederado) return;
    fijarParametros({}, { replace: true });
    if (errorFederado) {
      setError(errorFederado);
      return;
    }
    if (!codigo || canjeActivo.current) return;
    canjeActivo.current = true;
    setCanjeando(true);
    ingresarConCodigo(codigo)
      .then(() => navegar("/resumen"))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => {
        canjeActivo.current = false;
        setCanjeando(false);
      });
  }, [parametros, fijarParametros, ingresarConCodigo, navegar]);

  function iniciarOauth(codigoProveedor: string) {
    setError(null);
    window.location.assign(urlInicioOauth(tenantLimpio, codigoProveedor));
  }

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
        <div className="hidden w-full max-w-[420px] items-center justify-between gap-espacio-3 lg:flex">
          <p className="text-micro uppercase tracking-widest text-tinta-suave">
            Portal seguro · acceso empresarial
          </p>
          <AlternarTema />
        </div>
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
            {canjeando ? (
              <div
                role="status"
                className="mt-espacio-6 flex items-center justify-center gap-espacio-3 text-pequeno text-tinta-suave"
              >
                <IconoRecargar tamano={16} className="animate-spin" />
                Completando el ingreso con tu proveedor…
              </div>
            ) : null}
            {proveedoresOauth && proveedoresOauth.length > 0 ? (
              <div className="mt-espacio-8">
                <div className="flex items-center gap-espacio-3" aria-hidden="true">
                  <span className="h-px flex-1 bg-borde" />
                  <span className="text-micro uppercase tracking-widest text-tinta-suave">
                    o continuá con
                  </span>
                  <span className="h-px flex-1 bg-borde" />
                </div>
                <div className="mt-espacio-4 space-y-espacio-3">
                  {proveedoresOauth.map((proveedor) => (
                    <Boton
                      key={proveedor.codigo}
                      type="button"
                      variante="secundario"
                      tamano="lg"
                      disabled={enviando || canjeando || tenantLimpio.length === 0}
                      aria-label={`Continuar con ${proveedor.nombre}`}
                      className="w-full"
                      onClick={() => iniciarOauth(proveedor.codigo)}
                    >
                      Continuar con {proveedor.nombre}
                    </Boton>
                  ))}
                </div>
              </div>
            ) : null}
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
