import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  Campo,
  GrupoSegmentado,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import { IconoCheck } from "../componentes/Iconos";
import {
  despublicarPlantilla,
  instalarPlantilla,
  listarCatalogoMarketplace,
  listarInstalaciones,
  listarMisPublicaciones,
  listarProcesos,
  publicarPlantilla,
} from "../api/procesos";
import type { PublicacionMarketplace } from "../api/procesos";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { useIdioma } from "../contextos/ProveedorIdioma";

type Seccion = "catalogo" | "instalaciones" | "publicaciones";

const POLITICAS = ["MANUAL", "MAYOR", "MENOR", "TODO"];

export function Marketplace() {
  const { t } = useIdioma();
  const navegar = useNavigate();
  const clienteConsultas = useQueryClient();
  const [seccion, setSeccion] = useState<Seccion>("catalogo");
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formularioPublicar, setFormularioPublicar] = useState(false);
  const [instalando, setInstalando] = useState<string | null>(null);
  const [versionSeleccionada, setVersionSeleccionada] = useState("");

  const catalogo = useQuery({
    queryKey: ["marketplace-catalogo"],
    queryFn: listarCatalogoMarketplace,
  });
  const instalaciones = useQuery({
    queryKey: ["marketplace-instalaciones"],
    queryFn: listarInstalaciones,
  });
  const publicaciones = useQuery({
    queryKey: ["marketplace-publicaciones"],
    queryFn: listarMisPublicaciones,
  });
  const procesos = useQuery({
    queryKey: ["procesos"],
    queryFn: listarProcesos,
  });

  const versionesPublicadas = useMemo(
    () =>
      (procesos.data ?? []).flatMap((proceso) =>
        (proceso.versiones ?? [])
          .filter((version) => version.estado === "PUBLICADA")
          .map((version) => ({
            versionId: version.id,
            etiqueta: `${proceso.nombre} · v${version.numero}`,
          })),
      ),
    [procesos.data],
  );

  const refrescar = () => {
    clienteConsultas.invalidateQueries({ queryKey: ["marketplace-catalogo"] });
    clienteConsultas.invalidateQueries({ queryKey: ["marketplace-instalaciones"] });
    clienteConsultas.invalidateQueries({ queryKey: ["marketplace-publicaciones"] });
  };

  const alExito = (texto: string) => {
    setError(null);
    setAviso(texto);
    setFormularioPublicar(false);
    setInstalando(null);
    refrescar();
  };

  const alError = (fallo: unknown) => {
    setAviso(null);
    setError(mensajeDeError(fallo));
  };

  const instalar = useMutation({
    mutationFn: instalarPlantilla,
    onSuccess: () => alExito(t("marketplace.instaladaAviso")),
    onError: alError,
  });

  const publicar = useMutation({
    mutationFn: publicarPlantilla,
    onSuccess: () => alExito(t("marketplace.publicadaAviso")),
    onError: alError,
  });

  const despublicar = useMutation({
    mutationFn: despublicarPlantilla,
    onSuccess: () => alExito(t("marketplace.retiradaAviso")),
    onError: alError,
  });

  const alInstalar = (evento: FormEvent<HTMLFormElement>, publicacionId: string) => {
    evento.preventDefault();
    const datos = new FormData(evento.currentTarget);
    instalar.mutate({
      publicacionId,
      pin: datos.get("pin") === "on",
      politicaActualizacion: String(datos.get("politicaActualizacion") ?? "MANUAL"),
    });
  };

  const alPublicar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const datos = new FormData(evento.currentTarget);
    const comercial: Record<string, unknown> = {};
    const precio = String(datos.get("precio") ?? "").trim();
    const contacto = String(datos.get("contacto") ?? "").trim();
    const publicadorNombre = String(datos.get("publicadorNombre") ?? "").trim();
    if (precio) comercial.precio = precio;
    if (contacto) comercial.contacto = contacto;
    if (publicadorNombre) comercial.publicadorNombre = publicadorNombre;
    publicar.mutate({
      versionId: versionSeleccionada,
      categoria: String(datos.get("categoria") ?? "").trim() || undefined,
      comercial: Object.keys(comercial).length > 0 ? comercial : undefined,
    });
  };

  const listaCatalogo = catalogo.data ?? [];
  const listaInstalaciones = instalaciones.data ?? [];
  const listaPublicaciones = publicaciones.data ?? [];
  const instaladas = new Set(listaInstalaciones.map((inst) => inst.definicionId));

  return (
    <>
      <Encabezado
        titulo={t("marketplace.titulo")}
        descripcion={t("marketplace.descripcion")}
      />
      <Contenido>
        <div className="mb-espacio-5 flex flex-wrap items-center justify-between gap-espacio-3">
          <GrupoSegmentado
            etiqueta={t("marketplace.grupoSeccion")}
            opciones={[
              { valor: "catalogo" as Seccion, texto: t("marketplace.catalogo") },
              { valor: "instalaciones" as Seccion, texto: t("marketplace.instalaciones") },
              { valor: "publicaciones" as Seccion, texto: t("marketplace.misPublicaciones") },
            ]}
            valor={seccion}
            alCambiar={setSeccion}
          />
          {seccion === "publicaciones" ? (
            <Boton
              variante="primario"
              onClick={() => setFormularioPublicar((abierto) => !abierto)}
              disabled={versionesPublicadas.length === 0}
            >
              {t("marketplace.publicarNueva")}
            </Boton>
          ) : null}
        </div>

        {aviso ? (
          <div
            role="status"
            aria-atomic="true"
            className="mb-espacio-4 flex items-start gap-espacio-2 rounded-panel border border-exito-borde bg-exito-tenue p-espacio-4 text-pequeno text-exito-texto"
          >
            <span aria-hidden="true" className="mt-px shrink-0">
              <IconoCheck tamano={16} />
            </span>
            <p className="min-w-0 [overflow-wrap:anywhere]">{aviso}</p>
          </div>
        ) : null}
        {error ? (
          <div className="mb-espacio-4">
            <ErrorPanel titulo={t("marketplace.errorAccion")} mensaje={error} />
          </div>
        ) : null}

        {seccion === "catalogo" ? (
          catalogo.isPending ? (
            <Cargando filas={3} alto="h-64" />
          ) : catalogo.isError ? (
            <ErrorPanel
              mensaje={mensajeDeError(catalogo.error)}
              error={catalogo.error}
              reintentar={() => catalogo.refetch()}
            />
          ) : listaCatalogo.length === 0 ? (
            <Vacio
              titulo={t("marketplace.catalogoVacio")}
              detalle={t("marketplace.catalogoVacioDetalle")}
            />
          ) : (
            <ul
              aria-label={t("marketplace.catalogo")}
              className="grid gap-espacio-4 md:grid-cols-2"
            >
              {listaCatalogo.map((publicacion) => (
                <TarjetaPublicacion
                  key={publicacion.id}
                  publicacion={publicacion}
                  instalada={instaladas.has(publicacion.definicionId)}
                  abierta={instalando === publicacion.id}
                  alAlternar={() =>
                    setInstalando((actual) =>
                      actual === publicacion.id ? null : publicacion.id,
                    )
                  }
                  alInstalar={alInstalar}
                  cargando={instalar.isPending && instalar.variables?.publicacionId === publicacion.id}
                  t={t}
                />
              ))}
            </ul>
          )
        ) : null}

        {seccion === "instalaciones" ? (
          instalaciones.isPending ? (
            <Cargando filas={3} alto="h-64" />
          ) : instalaciones.isError ? (
            <ErrorPanel
              mensaje={mensajeDeError(instalaciones.error)}
              error={instalaciones.error}
              reintentar={() => instalaciones.refetch()}
            />
          ) : listaInstalaciones.length === 0 ? (
            <Vacio
              titulo={t("marketplace.sinInstalaciones")}
              detalle={t("marketplace.sinInstalacionesDetalle")}
            />
          ) : (
            <ul
              aria-label={t("marketplace.instalaciones")}
              className="space-y-espacio-4"
            >
              {listaInstalaciones.map((instalacion) => (
                <li key={instalacion.id} className="min-w-0">
                  <Tarjeta>
                    <div className="flex flex-wrap items-start justify-between gap-espacio-3">
                      <div className="min-w-0 space-y-espacio-1">
                        <div className="flex flex-wrap items-center gap-espacio-2">
                          <p className="text-pequeno font-semibold text-tinta">
                            {t("marketplace.instalacion")}
                          </p>
                          {instalacion.pin ? (
                            <Pastilla tono="informacion">{t("marketplace.pin")}</Pastilla>
                          ) : null}
                          <Pastilla tono="neutro">
                            {instalacion.politicaActualizacion ?? "MANUAL"}
                          </Pastilla>
                        </div>
                        <p className="text-pequeno text-tinta-suave">
                          {t("marketplace.publicador")}: {instalacion.publicadorTenantId}
                        </p>
                        <p className="text-pequeno text-tinta-suave">
                          {t("marketplace.definicionOrigen")}: {instalacion.definicionId}
                        </p>
                        {instalacion.alta ? (
                          <p className="text-pequeno text-tinta-suave">
                            {t("marketplace.instaladaEl")}: {formatearFecha(instalacion.alta)}
                          </p>
                        ) : null}
                      </div>
                      {instalacion.definicionLocalId ? (
                        <Boton
                          variante="primario"
                          tamano="sm"
                          onClick={() =>
                            navegar(`/workflow/${instalacion.definicionLocalId}`)
                          }
                        >
                          {t("marketplace.abrirEnStudio")}
                        </Boton>
                      ) : null}
                    </div>
                  </Tarjeta>
                </li>
              ))}
            </ul>
          )
        ) : null}

        {seccion === "publicaciones" ? (
          <>
            {formularioPublicar ? (
              <Tarjeta className="mb-espacio-5">
                <form onSubmit={alPublicar} className="space-y-espacio-4">
                  <div className="grid gap-espacio-4 md:grid-cols-2">
                    <Selector
                      etiqueta={t("marketplace.version")}
                      name="versionId"
                      required
                      value={versionSeleccionada}
                      onChange={(evento) =>
                        setVersionSeleccionada(evento.target.value)
                      }
                    >
                      <option value="" disabled>
                        {t("marketplace.elegirVersion")}
                      </option>
                      {versionesPublicadas.map((version) => (
                        <option key={version.versionId} value={version.versionId}>
                          {version.etiqueta}
                        </option>
                      ))}
                    </Selector>
                    <Campo
                      etiqueta={t("marketplace.categoria")}
                      name="categoria"
                      maxLength={128}
                      placeholder="comex"
                    />
                    <Campo
                      etiqueta={t("marketplace.precio")}
                      name="precio"
                      placeholder="USD 0"
                    />
                    <Campo
                      etiqueta={t("marketplace.contacto")}
                      name="contacto"
                      type="email"
                    />
                    <Campo
                      etiqueta={t("marketplace.publicadorNombre")}
                      name="publicadorNombre"
                      maxLength={128}
                    />
                  </div>
                  <div className="flex justify-end gap-espacio-3">
                    <Boton type="button" onClick={() => setFormularioPublicar(false)}>
                      {t("comun.cancelar")}
                    </Boton>
                    <Boton variante="primario" cargando={publicar.isPending}>
                      {t("marketplace.publicar")}
                    </Boton>
                  </div>
                </form>
              </Tarjeta>
            ) : null}
            {publicaciones.isPending ? (
              <Cargando filas={3} alto="h-64" />
            ) : publicaciones.isError ? (
              <ErrorPanel
                mensaje={mensajeDeError(publicaciones.error)}
                error={publicaciones.error}
                reintentar={() => publicaciones.refetch()}
              />
            ) : listaPublicaciones.length === 0 ? (
              <Vacio
                titulo={t("marketplace.sinPublicaciones")}
                detalle={t("marketplace.sinPublicacionesDetalle")}
              />
            ) : (
              <ul
                aria-label={t("marketplace.misPublicaciones")}
                className="space-y-espacio-4"
              >
                {listaPublicaciones.map((publicacion) => (
                  <li key={publicacion.id} className="min-w-0">
                    <Tarjeta>
                      <div className="flex flex-wrap items-start justify-between gap-espacio-3">
                        <div className="min-w-0 space-y-espacio-1">
                          <div className="flex items-center gap-espacio-3">
                            <p className="text-pequeno font-semibold text-tinta">
                              {publicacion.comercial?.nombreProceso ?? publicacion.definicionId}
                            </p>
                            {publicacion.categoria ? (
                              <Pastilla tono="informacion">{publicacion.categoria}</Pastilla>
                            ) : null}
                          </div>
                          <p className="text-pequeno text-tinta-suave">
                            {t("marketplace.versionNumero")}:{" "}
                            {publicacion.comercial?.versionNumero ?? "—"} ·{" "}
                            {t("marketplace.nodos")}:{" "}
                            {publicacion.comercial?.nodosTotal ?? "—"}
                          </p>
                          {publicacion.comercial?.precio ? (
                            <p className="text-pequeno text-tinta-suave">
                              {t("marketplace.precio")}: {publicacion.comercial.precio}
                            </p>
                          ) : null}
                          {publicacion.alta ? (
                            <p className="text-pequeno text-tinta-suave">
                              {t("marketplace.publicadaEl")}: {formatearFecha(publicacion.alta)}
                            </p>
                          ) : null}
                        </div>
                        <Boton
                          variante="peligro"
                          tamano="sm"
                          cargando={
                            despublicar.isPending &&
                            despublicar.variables === publicacion.id
                          }
                          onClick={() => despublicar.mutate(publicacion.id)}
                        >
                          {t("marketplace.retirar")}
                        </Boton>
                      </div>
                    </Tarjeta>
                  </li>
                ))}
              </ul>
            )}
          </>
        ) : null}
      </Contenido>
    </>
  );
}

function TarjetaPublicacion({
  publicacion,
  instalada,
  abierta,
  alAlternar,
  alInstalar,
  cargando,
  t,
}: {
  publicacion: PublicacionMarketplace;
  instalada: boolean;
  abierta: boolean;
  alAlternar: () => void;
  alInstalar: (evento: FormEvent<HTMLFormElement>, publicacionId: string) => void;
  cargando: boolean;
  t: (clave: string) => string;
}) {
  const comercial = publicacion.comercial ?? {};
  const [politica, setPolitica] = useState("MANUAL");
  return (
    <li className="min-w-0">
      <Tarjeta className="h-full">
        <div className="flex h-full flex-col gap-espacio-3">
          <div className="flex items-start justify-between gap-espacio-3">
            <div className="min-w-0">
              <p className="text-pequeno font-semibold text-tinta">
                {comercial.nombreProceso ?? publicacion.definicionId}
              </p>
              {comercial.publicadorNombre ? (
                <p className="text-pequeno text-tinta-suave">
                  {comercial.publicadorNombre}
                </p>
              ) : null}
            </div>
            {publicacion.categoria ? (
              <Pastilla tono="informacion">{publicacion.categoria}</Pastilla>
            ) : null}
          </div>
          {comercial.descripcionProceso ? (
            <p className="text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
              {comercial.descripcionProceso}
            </p>
          ) : null}
          <div className="flex flex-wrap gap-espacio-2">
            {(comercial.tiposNodo ?? []).map((tipo) => (
              <Pastilla key={tipo} tono="neutro">
                {t(`tipoNodo.${tipo}`)}
              </Pastilla>
            ))}
          </div>
          <div className="mt-auto flex items-center justify-between gap-espacio-3">
            <p className="text-pequeno text-tinta-suave">
              {t("marketplace.nodos")}: {comercial.nodosTotal ?? "—"}
              {comercial.precio ? ` · ${comercial.precio}` : ""}
            </p>
            {instalada ? (
              <Pastilla tono="exito">{t("marketplace.instalada")}</Pastilla>
            ) : (
              <Boton variante="primario" tamano="sm" onClick={alAlternar}>
                {t("marketplace.instalar")}
              </Boton>
            )}
          </div>
          {abierta && !instalada ? (
            <form
              onSubmit={(evento) => alInstalar(evento, publicacion.id)}
              className="space-y-espacio-3 border-t border-borde pt-espacio-3"
            >
              <input type="hidden" name="politicaActualizacion" value={politica} />
              <Selector
                etiqueta={t("marketplace.politica")}
                value={politica}
                onChange={(evento) => setPolitica(evento.target.value)}
                ayuda={t("marketplace.politicaAyuda")}
              >
                {POLITICAS.map((politica) => (
                  <option key={politica} value={politica}>
                    {t(`marketplace.politica${politica}`)}
                  </option>
                ))}
              </Selector>
              <label className="flex items-center gap-espacio-2 text-pequeno text-tinta">
                <input type="checkbox" name="pin" />
                {t("marketplace.pinEtiqueta")}
              </label>
              <div className="flex justify-end gap-espacio-3">
                <Boton type="button" tamano="sm" onClick={alAlternar}>
                  {t("comun.cancelar")}
                </Boton>
                <Boton variante="primario" tamano="sm" cargando={cargando}>
                  {t("marketplace.confirmarInstalar")}
                </Boton>
              </div>
            </form>
          ) : null}
        </div>
      </Tarjeta>
    </li>
  );
}
