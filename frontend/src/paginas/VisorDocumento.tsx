import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Cargando, ErrorPanel } from "../componentes/Estados";
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
    <div className="fixed inset-0 z-40 flex justify-end bg-grafito/45" role="dialog" aria-modal="true">
      <button type="button" aria-label="Cerrar" className="flex-1 cursor-default" onClick={alCerrar} />
      <section className="flex w-full max-w-3xl flex-col bg-lienzo shadow-2xl">
        <header className="flex items-start justify-between gap-4 border-b border-borde bg-white px-6 py-5">
          <div className="min-w-0">
            <p className="truncate font-titulo text-lg text-tinta">
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
            <button
              type="button"
              onClick={abrirOriginal}
              className="rounded-lg border border-borde px-3 py-1.5 text-sm text-tinta transition hover:border-violeta hover:text-violeta"
            >
              Ver original
            </button>
            <button
              type="button"
              onClick={alCerrar}
              className="rounded-lg px-2.5 py-1.5 text-sm text-tinta-suave transition hover:bg-borde/40"
            >
              Cerrar
            </button>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto px-6 py-5">
          {aviso ? (
            <div
              role="status"
              className={`mb-4 rounded-lg px-3.5 py-2.5 text-sm ${
                aviso.tono === "ok"
                  ? "border border-exito/25 bg-exito-tenue text-exito"
                  : "border border-rojo/25 bg-rojo-tenue text-rojo"
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

              <nav className="mb-4 flex gap-1 border-b border-borde">
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
                    className={`-mb-px border-b-2 px-3 py-2 text-sm transition ${
                      pestana === clave
                        ? "border-violeta font-medium text-violeta"
                        : "border-transparent text-tinta-suave hover:text-tinta"
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
              <p className="mb-2 text-xs text-violeta">
                {Object.keys(correcciones).length} campo(s) corregido(s) sin guardar
              </p>
            ) : null}
            <input
              value={motivo}
              onChange={(evento) => setMotivo(evento.target.value)}
              placeholder="Motivo de la decision (obligatorio para rechazar, observar o corregir)"
              className="w-full rounded-lg border border-borde px-3 py-2 text-sm outline-none transition focus:border-violeta focus:ring-2 focus:ring-violeta/15"
            />
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("APROBADO")}
                onClick={() => decidir.mutate("APROBAR")}
                className="rounded-lg bg-exito px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-40"
              >
                Aprobar
              </button>
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("OBSERVADO")}
                onClick={() => decidir.mutate("OBSERVAR")}
                className="rounded-lg bg-alerta px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-40"
              >
                Observar
              </button>
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("RECHAZADO")}
                onClick={() => decidir.mutate("RECHAZAR")}
                className="rounded-lg bg-rojo px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-40"
              >
                Rechazar
              </button>
              <button
                type="button"
                disabled={trabajando}
                onClick={() => reproceso.mutate()}
                className="rounded-lg border border-borde px-4 py-2 text-sm font-medium text-tinta transition hover:border-violeta hover:text-violeta disabled:opacity-40"
              >
                Reprocesar
              </button>
              <button
                type="button"
                disabled={trabajando || !documento.transicionesPosibles.includes("CERRADO")}
                onClick={() => cierre.mutate()}
                className="rounded-lg border border-borde px-4 py-2 text-sm font-medium text-tinta transition hover:border-grafito disabled:opacity-40"
              >
                Cerrar documento
              </button>
            </div>
            {documento.transicionesPosibles.length === 0 ? (
              <p className="mt-2 text-xs text-tinta-suave">
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
    <div className="rounded-lg border border-borde bg-white px-3 py-2.5">
      <p className="text-xs text-tinta-suave">{etiqueta}</p>
      <p className={`mt-0.5 truncate font-titulo text-sm ${color}`}>{valor}</p>
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
      <p className="rounded-lg border border-dashed border-borde bg-white px-4 py-8 text-center text-sm text-tinta-suave">
        Todavia no se ejecuto ninguna extraccion sobre este documento.
      </p>
    );
  }
  return (
    <div className="overflow-hidden rounded-xl border border-borde bg-white">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-borde bg-lienzo text-xs uppercase tracking-wide text-tinta-suave">
          <tr>
            <th className="px-4 py-2.5 font-medium">Campo</th>
            <th className="px-4 py-2.5 font-medium">Valor</th>
            <th className="px-4 py-2.5 font-medium">Presencia</th>
            <th className="px-4 py-2.5 font-medium">Confianza</th>
          </tr>
        </thead>
        <tbody>
          {detalle.extraccion.valores.map((valor) => {
            const corregido = correcciones[valor.claveCampo];
            return (
              <tr key={valor.id} className="border-b border-borde/70 last:border-0">
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
                      className={`w-full rounded-md border px-2 py-1 text-sm outline-none transition ${
                        corregido !== undefined
                          ? "border-violeta bg-violeta-tenue/40"
                          : "border-transparent hover:border-borde focus:border-violeta"
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
      <p className="rounded-lg border border-exito/25 bg-exito-tenue px-4 py-6 text-center text-sm text-exito">
        La validacion no encontro hallazgos.
      </p>
    );
  }
  return (
    <ul className="space-y-2">
      {hallazgos.map((hallazgo) => (
        <li key={hallazgo.id} className="rounded-lg border border-borde bg-white px-4 py-3">
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
      <p className="rounded-lg border border-dashed border-borde bg-white px-4 py-8 text-center text-sm text-tinta-suave">
        Ningun conector devolvio candidatos para este documento.
      </p>
    );
  }
  return (
    <ul className="space-y-2">
      {detalle.candidatos.map((candidato) => (
        <li
          key={candidato.id}
          className={`rounded-lg border bg-white px-4 py-3 ${
            candidato.seleccionado ? "border-exito ring-1 ring-exito/25" : "border-borde"
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
              <span className="shrink-0 rounded-full bg-exito-tenue px-2.5 py-0.5 text-xs font-medium text-exito ring-1 ring-exito/25">
                Seleccionado
              </span>
            ) : puedeElegir && !candidato.descartado ? (
              <button
                type="button"
                disabled={eligiendo}
                onClick={() => alElegir(candidato.id)}
                className="shrink-0 rounded-lg border border-borde px-3 py-1.5 text-xs font-medium transition hover:border-violeta hover:text-violeta disabled:opacity-40"
              >
                Elegir
              </button>
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
      <p className="rounded-lg border border-dashed border-borde bg-white px-4 py-8 text-center text-sm text-tinta-suave">
        Todavia no hubo revisiones humanas sobre este documento.
      </p>
    );
  }
  return (
    <ol className="space-y-3">
      {detalle.revisiones.map((revision) => (
        <li key={revision.id} className="rounded-lg border border-borde bg-white px-4 py-3">
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
