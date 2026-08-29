import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { Boton, Pastilla } from "../componentes/Interfaz";
import { IconoCerrar, IconoDescargar, IconoRecargar } from "../componentes/Iconos";
import { BarraConfianza, InsigniaEstado, InsigniaPresencia, InsigniaSeveridad } from "../componentes/Insignias";
import { formatearFecha } from "./Documentos";
import { mensajeDeError } from "../api/cliente";
import {
  cerrar,
  obtenerDetalle,
  reprocesar,
  revisar,
  seleccionarCandidato,
  urlOriginal,
} from "../api/documentos";
import { useSesion } from "../contextos/ProveedorSesion";

type Pestana = "campos" | "hallazgos" | "asociacion" | "actividad";

export function VisorDocumento({
  documentoId,
  alCerrar,
}: {
  documentoId: string;
  alCerrar: () => void;
}) {
  const { tienePermiso } = useSesion();
  const clienteConsultas = useQueryClient();
  const [pestana, setPestana] = useState<Pestana>("campos");
  const [correcciones, setCorrecciones] = useState<Record<string, string>>({});
  const [motivo, setMotivo] = useState("");
  const [aviso, setAviso] = useState<{ tono: "ok" | "error"; texto: string } | null>(null);

  const consulta = useQuery({
    queryKey: ["documento", documentoId],
    queryFn: () => obtenerDetalle(documentoId),
  });

  function invalidar() {
    clienteConsultas.invalidateQueries({ queryKey: ["documento", documentoId] });
    clienteConsultas.invalidateQueries({ queryKey: ["documentos"] });
    clienteConsultas.invalidateQueries({ queryKey: ["excepciones"] });
    clienteConsultas.invalidateQueries({ queryKey: ["resumen"] });
  }

  const decidir = useMutation({
    mutationFn: (decision: string) =>
      revisar(documentoId, {
        decision,
        motivo: motivo.trim() || undefined,
        correcciones: Object.keys(correcciones).length ? correcciones : undefined,
      }),
    onSuccess: (revision) => {
      setAviso({ tono: "ok", texto: `Documento ${revision.estadoNuevo}` });
      setCorrecciones({});
      setMotivo("");
      invalidar();
    },
    onError: (error) => setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  const reproceso = useMutation({
    mutationFn: () => reprocesar(documentoId),
    onSuccess: () => {
      setAviso({ tono: "ok", texto: "Documento reencolado para reproceso" });
      invalidar();
    },
    onError: (error) => setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  const cierre = useMutation({
    mutationFn: () => cerrar(documentoId),
    onSuccess: () => {
      setAviso({ tono: "ok", texto: "Documento cerrado" });
      invalidar();
    },
    onError: (error) => setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  const eleccion = useMutation({
    mutationFn: (candidatoId: string) =>
      seleccionarCandidato(documentoId, candidatoId, motivo.trim() || "Seleccion desde el portal"),
    onSuccess: () => {
      setAviso({ tono: "ok", texto: "Candidato asociado" });
      invalidar();
    },
    onError: (error) => setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  async function abrirOriginal() {
    try {
      window.open(await urlOriginal(documentoId), "_blank", "noopener");
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  }

  const detalle = consulta.data;
  const documento = detalle?.documento;
  const puedeRevisar = tienePermiso("documentos.revisar");
  const trabajando = decidir.isPending || reproceso.isPending || cierre.isPending;

  return (
    <div
      className="velo fixed inset-0 z-50 flex justify-end bg-grafito/45 backdrop-blur-[2px]"
      role="dialog"
      aria-modal="true"
    >
      <button type="button" aria-label="Cerrar" className="flex-1 cursor-default" onClick={alCerrar} />
      <section className="entrar-lateral flex w-full max-w-3xl flex-col border-l border-borde bg-lienzo shadow-flotante">
        <header className="flex items-start justify-between gap-4 border-b border-borde bg-white px-6 py-5">
          <div className="min-w-0">
            <p className="truncate font-titulo text-xl text-tinta">
              {documento?.nombre ?? "Documento"}
            </p>
            <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-tinta-suave">
              {documento ? <InsigniaEstado estado={documento.estado} /> : null}
              {documento?.codigoPlantilla ? (
                <span>
                  {documento.codigoPlantilla} v{documento.numeroVersionPlantilla}
                </span>
              ) : null}
              {documento?.sujetoIdObjeto ? (
                <span>
                  {documento.sujetoOrigen} · {documento.sujetoTipoObjeto} {documento.sujetoIdObjeto}
                </span>
              ) : null}
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Boton tamano="sm" onClick={abrirOriginal}>
              <IconoDescargar tamano={14} />
              Ver original
            </Boton>
            <Boton variante="fantasma" tamano="sm" onClick={alCerrar} aria-label="Cerrar">
              <IconoCerrar tamano={16} />
            </Boton>
          </div>
        </header>

        <div className="barra-desplazamiento-fina flex-1 overflow-y-auto px-6 py-5">
          {aviso ? (
            <div
              role="status"
              className={`aparecer mb-4 rounded-xl border px-4 py-3 text-sm ${
                aviso.tono === "ok"
                  ? "border-exito-borde bg-exito-tenue text-exito"
                  : "border-rojo-borde bg-rojo-tenue text-rojo"
              }`}
            >
              {aviso.texto}
            </div>
          ) : null}

          {consulta.isPending ? (
            <Cargando filas={5} />
          ) : consulta.isError ? (
            <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
          ) : !detalle ? null : (
            <>
              <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
                <Metrica etiqueta="Proveedor" valor={detalle.extraccion?.proveedor ?? "—"} />
                <Metrica etiqueta="Modelo" valor={detalle.extraccion?.modelo ?? "—"} />
                <Metrica
                  etiqueta="Validacion"
                  valor={detalle.validacion?.resultado ?? "—"}
                  tono={detalle.validacion?.resultado === "APROBADO" ? "ok" : "alerta"}
                />
                <Metrica
                  etiqueta="Autoaprobado"
                  valor={detalle.validacion ? (detalle.validacion.autoaprobado ? "Si" : "No") : "—"}
                />
              </div>

              <nav className="mb-4 inline-flex rounded-xl border border-borde bg-white p-1 shadow-plano">
                {(
                  [
                    ["campos", `Campos (${detalle.extraccion?.valores.length ?? 0})`],
                    ["hallazgos", `Hallazgos (${detalle.validacion?.hallazgos.length ?? 0})`],
                    ["asociacion", `Asociacion (${detalle.candidatos.length})`],
                    ["actividad", `Actividad (${detalle.revisiones.length})`],
                  ] as [Pestana, string][]
                ).map(([clave, texto]) => (
                  <button
                    key={clave}
                    type="button"
                    onClick={() => setPestana(clave)}
                    className={`rounded-lg px-3.5 py-1.5 text-xs font-semibold transition ${
                      pestana === clave
                        ? "bg-grafito text-white shadow-plano"
                        : "text-tinta-suave hover:text-tinta"
                    }`}
                  >
                    {texto}
                  </button>
                ))}
              </nav>

              {pestana === "campos" ? (
                <PanelCampos
                  detalle={detalle}
                  correcciones={correcciones}
                  editable={puedeRevisar}
                  alCorregir={(clave, valor) =>
                    setCorrecciones((actuales) => {
                      const copia = { ...actuales };
                      if (valor === null) {
                        delete copia[clave];
                      } else {
                        copia[clave] = valor;
                      }
                      return copia;
                    })
                  }
                />
              ) : null}

              {pestana === "hallazgos" ? <PanelHallazgos detalle={detalle} /> : null}

              {pestana === "asociacion" ? (
                <PanelAsociacion
                  detalle={detalle}
                  puedeElegir={puedeRevisar}
                  eligiendo={eleccion.isPending}
                  alElegir={(candidatoId) => eleccion.mutate(candidatoId)}
                />
              ) : null}

              {pestana === "actividad" ? <PanelActividad detalle={detalle} /> : null}
            </>
          )}
        </div>

        {puedeRevisar && documento ? (
          <footer className="border-t border-borde bg-white px-6 py-4">
            {Object.keys(correcciones).length ? (
              <p className="mb-2.5 inline-flex items-center gap-1.5 rounded-lg bg-violeta-tenue px-2.5 py-1 text-xs font-semibold text-violeta ring-1 ring-inset ring-violeta-borde">
                {Object.keys(correcciones).length} campo(s) corregido(s) sin guardar
              </p>
            ) : null}
            <input
              value={motivo}
              onChange={(evento) => setMotivo(evento.target.value)}
              placeholder="Motivo de la decision (obligatorio para rechazar, observar o corregir)"
              className="h-10 w-full rounded-xl border border-borde bg-white px-3 text-sm text-tinta outline-none transition placeholder:text-tinta-tenue focus:border-violeta focus:ring-[3px] focus:ring-violeta/15"
            />
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("APROBADO")}
                onClick={() => decidir.mutate("APROBAR")}
                className="h-9.5 rounded-xl bg-exito px-4 text-sm font-semibold text-white shadow-[0_4px_14px_-3px_rgba(15,157,88,0.45)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:bg-borde-fuerte disabled:text-white/70 disabled:shadow-none"
              >
                Aprobar
              </button>
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("OBSERVADO")}
                onClick={() => decidir.mutate("OBSERVAR")}
                className="h-9.5 rounded-xl bg-alerta px-4 text-sm font-semibold text-white shadow-[0_4px_14px_-3px_rgba(194,118,10,0.45)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:bg-borde-fuerte disabled:text-white/70 disabled:shadow-none"
              >
                Observar
              </button>
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("RECHAZADO")}
                onClick={() => decidir.mutate("RECHAZAR")}
                className="h-9.5 rounded-xl bg-rojo px-4 text-sm font-semibold text-white shadow-[0_4px_14px_-3px_rgba(255,30,30,0.45)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:bg-borde-fuerte disabled:text-white/70 disabled:shadow-none"
              >
                Rechazar
              </button>
              <span className="ml-auto flex gap-2">
                <Boton tamano="md" disabled={trabajando} onClick={() => reproceso.mutate()}>
                  <IconoRecargar tamano={14} />
                  Reprocesar
                </Boton>
                <Boton
                  tamano="md"
                  disabled={trabajando || !documento.transicionesPosibles.includes("CERRADO")}
                  onClick={() => cierre.mutate()}
                >
                  Cerrar documento
                </Boton>
              </span>
            </div>
            {documento.transicionesPosibles.length === 0 ? (
              <p className="mt-2.5 text-xs text-tinta-suave">
                Este documento esta en un estado final y no admite mas transiciones.
              </p>
            ) : null}
          </footer>
        ) : null}
      </section>
    </div>
  );
}

function Metrica({
  etiqueta,
  valor,
  tono,
}: {
  etiqueta: string;
  valor: string;
  tono?: "ok" | "alerta";
}) {
  const color = tono === "ok" ? "text-exito" : tono === "alerta" ? "text-alerta" : "text-tinta";
  return (
    <div className="rounded-xl border border-borde bg-white px-3.5 py-2.5 shadow-plano">
      <p className="text-[10px] font-semibold uppercase tracking-wider text-tinta-tenue">{etiqueta}</p>
      <p className={`mt-1 truncate font-titulo text-sm ${color}`}>{valor}</p>
    </div>
  );
}

function PanelCampos({
  detalle,
  correcciones,
  editable,
  alCorregir,
}: {
  detalle: NonNullable<ReturnType<typeof useQuery<Awaited<ReturnType<typeof obtenerDetalle>>>>["data"]>;
  correcciones: Record<string, string>;
  editable: boolean;
  alCorregir: (clave: string, valor: string | null) => void;
}) {
  if (!detalle.extraccion) {
    return (
      <Vacio
        titulo="Sin extraccion"
        detalle="Todavia no se ejecuto ninguna extraccion sobre este documento."
      />
    );
  }
  return (
    <div className="overflow-hidden rounded-2xl border border-borde bg-white shadow-tarjeta">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-borde bg-lienzo/70">
            {["Campo", "Valor", "Presencia", "Confianza"].map((columna) => (
              <th
                key={columna}
                className="px-4 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-tinta-suave"
              >
                {columna}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-borde">
          {detalle.extraccion.valores.map((valor) => {
            const corregido = correcciones[valor.claveCampo];
            return (
              <tr key={valor.id} className="transition hover:bg-lienzo/50">
                <td className="px-4 py-2.5">
                  <p className="font-medium text-tinta">{valor.etiqueta ?? valor.claveCampo}</p>
                  <p className="text-xs text-tinta-suave">{valor.claveCampo}</p>
                </td>
                <td className="px-4 py-2.5">
                  {editable ? (
                    <input
                      value={corregido ?? valor.valorNormalizado ?? ""}
                      onChange={(evento) => {
                        const nuevo = evento.target.value;
                        alCorregir(valor.claveCampo, nuevo === (valor.valorNormalizado ?? "") ? null : nuevo);
                      }}
                      className={`w-full rounded-lg border px-2.5 py-1.5 text-sm outline-none transition ${
                        corregido !== undefined
                          ? "border-violeta bg-violeta-tenue font-medium text-violeta"
                          : "border-transparent hover:border-borde focus:border-violeta focus:ring-[3px] focus:ring-violeta/15"
                      }`}
                    />
                  ) : (
                    <span>{valor.valorNormalizado ?? "—"}</span>
                  )}
                  {valor.corregidoManualmente ? (
                    <p className="mt-0.5 text-xs text-violeta">corregido manualmente</p>
                  ) : null}
                </td>
                <td className="px-4 py-2.5">
                  <InsigniaPresencia presencia={valor.presencia} />
                </td>
                <td className="px-4 py-2.5">
                  <BarraConfianza valor={valor.confianza} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function PanelHallazgos({
  detalle,
}: {
  detalle: NonNullable<ReturnType<typeof useQuery<Awaited<ReturnType<typeof obtenerDetalle>>>>["data"]>;
}) {
  const hallazgos = detalle.validacion?.hallazgos ?? [];
  if (!hallazgos.length) {
    return (
      <p className="rounded-2xl border border-exito-borde bg-exito-tenue px-4 py-8 text-center text-sm font-medium text-exito">
        La validacion no encontro hallazgos.
      </p>
    );
  }
  return (
    <ul className="space-y-2">
      {hallazgos.map((hallazgo) => (
        <li key={hallazgo.id} className="rounded-xl border border-borde bg-white px-4 py-3 shadow-plano">
          <div className="flex flex-wrap items-center gap-2">
            <InsigniaSeveridad severidad={hallazgo.severidad} />
            <span className="font-mono text-xs text-tinta-suave">{hallazgo.codigoRegla}</span>
            {hallazgo.claveCampo ? (
              <span className="text-xs text-tinta-suave">· {hallazgo.claveCampo}</span>
            ) : null}
          </div>
          <p className="mt-1.5 text-sm text-tinta">{hallazgo.mensaje}</p>
          {hallazgo.sobreescrito ? (
            <p className="mt-1 text-xs text-violeta">
              Sobreescrito por {hallazgo.sobreescritoPor}: {hallazgo.motivoSobreescritura}
            </p>
          ) : null}
        </li>
      ))}
    </ul>
  );
}

function PanelAsociacion({
  detalle,
  puedeElegir,
  eligiendo,
  alElegir,
}: {
  detalle: NonNullable<ReturnType<typeof useQuery<Awaited<ReturnType<typeof obtenerDetalle>>>>["data"]>;
  puedeElegir: boolean;
  eligiendo: boolean;
  alElegir: (candidatoId: string) => void;
}) {
  if (!detalle.candidatos.length) {
    return (
      <Vacio
        titulo="Sin candidatos"
        detalle="Ningun conector devolvio candidatos para este documento."
      />
    );
  }
  return (
    <ul className="space-y-2">
      {detalle.candidatos.map((candidato) => (
        <li
          key={candidato.id}
          className={`rounded-xl border bg-white px-4 py-3 shadow-plano transition ${
            candidato.seleccionado ? "border-exito ring-1 ring-exito/25" : "border-borde hover:border-borde-fuerte"
          } ${candidato.descartado ? "opacity-50" : ""}`}
        >
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-tinta">
                {candidato.descripcion ?? candidato.idObjeto}
              </p>
              <p className="text-xs text-tinta-suave">
                {candidato.conector} · {candidato.tipoObjeto} {candidato.idObjeto}
                {candidato.puntaje !== undefined ? ` · puntaje ${candidato.puntaje}` : ""}
              </p>
            </div>
            {candidato.seleccionado ? (
              <Pastilla tono="exito">Seleccionado</Pastilla>
            ) : puedeElegir && !candidato.descartado ? (
              <Boton tamano="sm" disabled={eligiendo} onClick={() => alElegir(candidato.id)}>
                Elegir
              </Boton>
            ) : null}
          </div>
        </li>
      ))}
    </ul>
  );
}

function PanelActividad({
  detalle,
}: {
  detalle: NonNullable<ReturnType<typeof useQuery<Awaited<ReturnType<typeof obtenerDetalle>>>>["data"]>;
}) {
  if (!detalle.revisiones.length) {
    return (
      <Vacio
        titulo="Sin actividad"
        detalle="Todavia no hubo revisiones humanas sobre este documento."
      />
    );
  }
  return (
    <ol className="space-y-3">
      {detalle.revisiones.map((revision) => (
        <li key={revision.id} className="rounded-xl border border-borde bg-white px-4 py-3 shadow-plano">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="font-medium text-tinta">{revision.decision}</span>
            <span className="text-tinta-suave">
              {revision.estadoAnterior} → {revision.estadoNuevo}
            </span>
            <span className="ml-auto text-xs text-tinta-suave">{formatearFecha(revision.alta)}</span>
          </div>
          <p className="mt-1 text-xs text-tinta-suave">{revision.actor}</p>
          {revision.motivo ? <p className="mt-1.5 text-sm text-tinta">{revision.motivo}</p> : null}
          {revision.cambios.length ? (
            <ul className="mt-2 space-y-1 border-t border-borde pt-2">
              {revision.cambios.map((cambio) => (
                <li key={cambio.claveCampo} className="text-xs text-tinta-suave">
                  <span className="font-medium text-tinta">{cambio.claveCampo}</span>{" "}
                  <span className="line-through">{cambio.valorAnterior ?? "vacio"}</span> →{" "}
                  <span className="text-violeta">{cambio.valorNuevo}</span>
                </li>
              ))}
            </ul>
          ) : null}
        </li>
      ))}
    </ol>
  );
}
