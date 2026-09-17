import { useEffect, useId, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Logotipo } from "../componentes/Marca";
import { Boton, Campo, Pastilla } from "../componentes/Interfaz";
import { ErrorPanel } from "../componentes/Estados";
import {
  IconoCandado,
  IconoCheck,
  IconoDocumentos,
  IconoGoogle,
  IconoMicrosoft,
  IconoRecargar,
} from "../componentes/Iconos";
import { AlternarTema } from "../componentes/Tema";
import { SelectorIdioma } from "../componentes/Idioma";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { useSesion } from "../contextos/ProveedorSesion";
import { mensajeDeError } from "../api/cliente";
import {
  listarProveedoresOauth,
  urlInicioOauth,
  type ProveedorOauth,
} from "../api/federacion";

const PILARES = ["ingresar.pilar1", "ingresar.pilar2", "ingresar.pilar3"];

const PROVEEDORES_DEFECTO: ProveedorOauth[] = [
  { codigo: "GOOGLE", nombre: "Google" },
  { codigo: "MICROSOFT", nombre: "Microsoft" },
];

const CAMPOS_DEMO = [
  { etiqueta: "ingresar.campoProveedor", valor: "Fenix Logistics S.A.", ancho: "96%", retraso: 700 },
  { etiqueta: "CUIT", valor: "30-71589423-1", ancho: "84%", retraso: 850 },
  { etiqueta: "ingresar.campoTotal", valor: "USD 12.480,00", ancho: "72%", retraso: 1000 },
];

function IconoProveedor({ codigo, tamano = 18 }: { codigo: string; tamano?: number }) {
  if (codigo === "GOOGLE") return <IconoGoogle tamano={tamano} />;
  if (codigo === "MICROSOFT") return <IconoMicrosoft tamano={tamano} />;
  return null;
}

function TarjetaDocumento() {
  const { t } = useIdioma();
  return (
    <div
      aria-hidden="true"
      className="flotar relative w-full max-w-[360px] rounded-panel border border-grafito-borde bg-grafito-alto/70 p-espacio-5 shadow-elevado backdrop-blur-sm"
    >
      <div className="flex items-center justify-between gap-espacio-3">
        <div className="flex min-w-0 items-center gap-espacio-3">
          <span className="flex size-espacio-8 shrink-0 items-center justify-center rounded-control bg-violeta/25 text-violeta-claro">
            <IconoDocumentos tamano={16} />
          </span>
          <div className="min-w-0">
            <p className="truncate text-pequeno font-semibold text-blanco">
              factura-0042.pdf
            </p>
            <p className="text-micro text-grafito-texto">{t("ingresar.extraccionCompleta")}</p>
          </div>
        </div>
        <Pastilla tono="exito" solido>
          {t("ingresar.validado")}
        </Pastilla>
      </div>
      <div className="mt-espacio-4 space-y-espacio-3">
        {CAMPOS_DEMO.map((campo) => (
          <div key={campo.etiqueta}>
            <div className="flex items-baseline justify-between gap-espacio-2 text-micro">
              <span className="text-grafito-texto">
                {campo.etiqueta === "CUIT" ? campo.etiqueta : t(campo.etiqueta)}
              </span>
              <span className="font-semibold text-blanco">{campo.valor}</span>
            </div>
            <div className="destello relative mt-espacio-1 h-espacio-1 overflow-hidden rounded-insignia bg-grafito-claro">
              <div
                className="barra-progreso h-full rounded-insignia bg-violeta"
                style={
                  {
                    width: campo.ancho,
                    "--retraso": `${campo.retraso}ms`,
                  } as React.CSSProperties
                }
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function Ingresar() {
  const { ingresar, ingresarConCodigo, registrar } = useSesion();
  const { t } = useIdioma();
  const navegar = useNavigate();
  const [parametros, fijarParametros] = useSearchParams();
  const [modo, setModo] = useState<"ingresar" | "registro">("ingresar");
  const [codigoTenant, setCodigoTenant] = useState(
    () => localStorage.getItem("nextdocs.ultimoTenant") ?? "",
  );
  const [email, setEmail] = useState("");
  const [clave, setClave] = useState("");
  const [nombreOrganizacion, setNombreOrganizacion] = useState("");
  const [codigoOrganizacion, setCodigoOrganizacion] = useState("");
  const [nombreAdministrador, setNombreAdministrador] = useState("");
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

  const proveedoresVisibles =
    proveedoresOauth && proveedoresOauth.length > 0
      ? proveedoresOauth
      : PROVEEDORES_DEFECTO;

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
    if (
      tenantLimpio.length > 0 &&
      proveedoresOauth &&
      !proveedoresOauth.some((p) => p.codigo === codigoProveedor)
    ) {
      setError(t("ingresar.accesoNoHabilitado", { tenant: tenantLimpio }));
      return;
    }
    window.location.assign(urlInicioOauth(tenantLimpio, codigoProveedor));
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (solicitudActiva.current) return;
    solicitudActiva.current = true;
    setError(null);
    setEnviando(true);
    try {
      if (modo === "registro") {
        await registrar({
          nombreOrganizacion: nombreOrganizacion.trim(),
          codigoOrganizacion: codigoOrganizacion.trim(),
          nombreAdministrador: nombreAdministrador.trim(),
          emailAdministrador: email.trim(),
          claveAdministrador: clave,
        });
        localStorage.setItem("nextdocs.ultimoTenant", codigoOrganizacion.trim());
      } else {
        await ingresar(codigoTenant.trim(), email.trim(), clave);
        localStorage.setItem("nextdocs.ultimoTenant", codigoTenant.trim());
      }
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
        className="relative hidden min-w-0 flex-col justify-between gap-espacio-12 overflow-hidden bg-grafito p-espacio-12 text-blanco lg:flex xl:p-espacio-16"
      >
        <div
          aria-hidden="true"
          className="aurora-a pointer-events-none absolute -top-40 -left-32 size-[520px] rounded-full bg-violeta/35 blur-[130px]"
        />
        <div
          aria-hidden="true"
          className="aurora-b pointer-events-none absolute -right-40 -bottom-48 size-[560px] rounded-full bg-violeta-claro/25 blur-[140px]"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-[0.06]"
          style={{
            backgroundImage:
              "linear-gradient(to right, #fff 1px, transparent 1px), linear-gradient(to bottom, #fff 1px, transparent 1px)",
            backgroundSize: "56px 56px",
          }}
        />
        <div className="relative">
          <Logotipo claro />
        </div>
        <div className="relative max-w-[590px]">
          <h2 className="subir font-titulo text-[clamp(32px,3.7vw,52px)] leading-tight">
            {t("ingresar.tituloMarca")}
          </h2>
          <div
            aria-hidden="true"
            className="mt-espacio-5 h-espacio-1 w-28 rounded-insignia bg-violeta"
          />
          <p
            className="subir mt-espacio-5 text-cuerpo text-grafito-texto"
            style={{ "--retraso": "80ms" } as React.CSSProperties}
          >
            {t("ingresar.subtituloMarca")}
          </p>
          <ul className="mt-espacio-8 space-y-espacio-4">
            {PILARES.map((pilar, indice) => (
              <li
                key={pilar}
                className="subir flex items-start gap-espacio-3"
                style={
                  { "--retraso": `${160 + indice * 90}ms` } as React.CSSProperties
                }
              >
                <span
                  aria-hidden="true"
                  className="flex size-espacio-5 shrink-0 items-center justify-center rounded-insignia bg-violeta text-blanco"
                >
                  <IconoCheck tamano={13} />
                </span>
                <span className="text-pequeno">{t(pilar)}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="subir relative" style={{ "--retraso": "420ms" } as React.CSSProperties}>
          <TarjetaDocumento />
        </div>
        <p className="relative flex items-center gap-espacio-2 text-pequeno text-grafito-texto">
          <span aria-hidden="true">
            <IconoCandado tamano={16} />
          </span>
          {t("ingresar.sesionCifrada")}
        </p>
      </section>

      <div className="flex min-w-0 flex-col items-center gap-espacio-8 px-espacio-6 py-espacio-8 lg:px-espacio-12 lg:py-espacio-16 xl:px-espacio-20">
        <div className="w-full max-w-[420px] lg:hidden">
          <Logotipo />
        </div>
        <div className="hidden w-full max-w-[420px] items-center justify-between gap-espacio-3 lg:flex">
          <p className="text-micro uppercase tracking-widest text-tinta-suave">
            {t("ingresar.portalSeguro")}
          </p>
          <div className="flex items-center gap-espacio-2">
            <SelectorIdioma />
            <AlternarTema />
          </div>
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
              className="subir font-titulo text-titulo-pagina text-tinta lg:text-titulo-destacado"
            >
              {modo === "registro" ? t("registro.titulo") : t("ingresar.titulo")}
            </h1>
            <p
              id={`${id}-descripcion`}
              className="subir mt-espacio-2 text-cuerpo text-tinta-suave"
              style={{ "--retraso": "60ms" } as React.CSSProperties}
            >
              {modo === "registro" ? t("registro.descripcion") : t("ingresar.descripcion")}
            </p>
            <div
              id={`${id}-error`}
              className="flex min-h-espacio-6 flex-col justify-center py-espacio-3"
            >
              {error ? (
                <ErrorPanel
                  titulo={modo === "registro" ? t("registro.errorTitulo") : t("ingresar.errorTitulo")}
                  mensaje={error}
                />
              ) : null}
            </div>
            <div
              className="subir space-y-espacio-4"
              style={{ "--retraso": "120ms" } as React.CSSProperties}
            >
              {modo === "registro" ? (
                <>
                  <Campo
                    etiqueta={t("registro.nombreOrganizacion")}
                    value={nombreOrganizacion}
                    onChange={(evento) => setNombreOrganizacion(evento.target.value)}
                    required
                    disabled={enviando}
                    autoComplete="organization"
                    placeholder="Fenix Logistics S.A."
                  />
                  <Campo
                    etiqueta={t("registro.codigoOrganizacion")}
                    ayuda={t("registro.ayudaCodigo")}
                    value={codigoOrganizacion}
                    onChange={(evento) => setCodigoOrganizacion(evento.target.value)}
                    required
                    disabled={enviando}
                    autoComplete="off"
                    placeholder={t("registro.placeholderCodigo")}
                  />
                  <Campo
                    etiqueta={t("registro.nombreAdministrador")}
                    value={nombreAdministrador}
                    onChange={(evento) => setNombreAdministrador(evento.target.value)}
                    required
                    disabled={enviando}
                    autoComplete="name"
                  />
                </>
              ) : (
                <Campo
                  etiqueta={t("ingresar.organizacion")}
                  ayuda={t("ingresar.organizacionAyuda")}
                  value={codigoTenant}
                  onChange={(evento) => setCodigoTenant(evento.target.value)}
                  required
                  disabled={enviando}
                  autoComplete="off"
                  placeholder={t("ingresar.organizacionPlaceholder")}
                />
              )}
              <Campo
                etiqueta={modo === "registro" ? t("registro.email") : t("ingresar.email")}
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                required
                disabled={enviando}
                autoComplete="email"
                placeholder="nombre@empresa.com"
              />
              <Campo
                etiqueta={modo === "registro" ? t("registro.clave") : t("ingresar.clave")}
                ayuda={modo === "registro" ? t("registro.ayudaClave") : undefined}
                type="password"
                value={clave}
                onChange={(evento) => setClave(evento.target.value)}
                required
                disabled={enviando}
                autoComplete={modo === "registro" ? "new-password" : "current-password"}
                placeholder="••••••••"
              />
            </div>
            <Boton
              type="submit"
              variante="primario"
              tamano="lg"
              cargando={enviando}
              aria-label={enviando
                ? modo === "registro" ? t("registro.registrando") : t("ingresar.ingresando")
                : modo === "registro" ? t("registro.registrar") : t("ingresar.ingresar")}
              className="subir destello mt-espacio-6 w-full overflow-hidden"
              style={{ "--retraso": "180ms" } as React.CSSProperties}
            >
              {modo === "registro" ? t("registro.registrar") : t("ingresar.ingresar")}
            </Boton>
            <p
              className="subir mt-espacio-4 text-center text-pequeno"
              style={{ "--retraso": "200ms" } as React.CSSProperties}
            >
              <button
                type="button"
                className="font-semibold text-violeta transition-colors hover:text-violeta-claro"
                onClick={() => {
                  setModo(modo === "registro" ? "ingresar" : "registro");
                  setError(null);
                }}
              >
                {modo === "registro"
                  ? t("ingresar.volverAIngresar")
                  : `${t("ingresar.sinOrganizacion")} ${t("ingresar.crearOrganizacion")}`}
              </button>
            </p>
            {canjeando ? (
              <div
                role="status"
                className="aparecer mt-espacio-6 flex items-center justify-center gap-espacio-3 text-pequeno text-tinta-suave"
              >
                <IconoRecargar tamano={16} className="animate-spin" />
                {t("ingresar.completandoProveedor")}
              </div>
            ) : null}
            <div
              className="subir mt-espacio-8"
              style={{ "--retraso": "240ms" } as React.CSSProperties}
            >
              <div className="flex items-center gap-espacio-3" aria-hidden="true">
                <span className="h-px flex-1 bg-borde" />
                <span className="text-micro uppercase tracking-widest text-tinta-suave">
                  {t("ingresar.oContinuaCon")}
                </span>
                <span className="h-px flex-1 bg-borde" />
              </div>
              <div className="mt-espacio-4 space-y-espacio-3">
                {proveedoresVisibles.map((proveedor) => (
                  <Boton
                    key={proveedor.codigo}
                    type="button"
                    variante="secundario"
                    tamano="lg"
                    disabled={enviando || canjeando}
                    aria-label={t("ingresar.continuarCon", { proveedor: proveedor.nombre })}
                    className="elevar w-full"
                    onClick={() => iniciarOauth(proveedor.codigo)}
                  >
                    <IconoProveedor codigo={proveedor.codigo} />
                    {t("ingresar.continuarCon", { proveedor: proveedor.nombre })}
                  </Boton>
                ))}
              </div>
            </div>
          </form>
        </div>
        <p role="status" aria-atomic="true" className="sr-only">
          {enviando ? t("ingresar.ingresando") : ""}
        </p>
        <footer className="w-full max-w-[420px] space-y-espacio-2 text-center text-pequeno text-tinta-suave">
          <p className="lg:hidden">
            {t("ingresar.sesionCifrada")}
          </p>
          <p>{t("ingresar.pie")}</p>
        </footer>
      </div>
    </main>
  );
}
