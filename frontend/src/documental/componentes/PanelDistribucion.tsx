import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  borrarDestinatario,
  guardarDestinatario,
  obtenerDestinatarios,
  obtenerEnvios,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import {
  Boton,
  BotonIcono,
  Campo,
  Panel,
  Tarjeta,
  CabeceraTarjeta,
} from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { IconoCerrar, IconoMas } from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { FAMILIAS, SITUACIONES_ENVIO, comoFecha } from "../dominio";

const VACIO = {
  nombre: "",
  correo: "",
  familias: [] as string[],
  situaciones: ["APROBADO"],
};

export function PanelDistribucion() {
  const { t, idioma } = useIdioma();
  const clienteConsultas = useQueryClient();

  const destinatarios = useQuery({
    queryKey: ["documental-destinatarios"],
    queryFn: obtenerDestinatarios,
  });
  const envios = useQuery({
    queryKey: ["documental-envios", "ENVIADO"],
    queryFn: () => obtenerEnvios({ estado: "ENVIADO", limite: 50 }),
  });
  const pendientes = useQuery({
    queryKey: ["documental-envios", "PENDIENTE"],
    queryFn: () => obtenerEnvios({ estado: "PENDIENTE", limite: 50 }),
    refetchInterval: 10000,
  });

  const [abierto, setAbierto] = useState(false);
  const [borrador, setBorrador] = useState(VACIO);
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const cerrar = () => {
    setAbierto(false);
    setBorrador(VACIO);
  };

  const alternarLista = (lista: string[], valor: string) =>
    lista.includes(valor)
      ? lista.filter((item) => item !== valor)
      : [...lista, valor];

  const guardar = async () => {
    try {
      await guardarDestinatario(borrador);
      setAviso({ tono: "ok", texto: t("documental.distribucion.guardado") });
      cerrar();
      clienteConsultas.invalidateQueries({
        queryKey: ["documental-destinatarios"],
      });
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const eliminar = async (id: string) => {
    try {
      await borrarDestinatario(id);
      setAviso({ tono: "ok", texto: t("documental.distribucion.borrado") });
      clienteConsultas.invalidateQueries({
        queryKey: ["documental-destinatarios"],
      });
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  if (destinatarios.isLoading) return <Cargando />;

  const listado = destinatarios.data || [];
  const enviados = envios.data || [];
  const enCola = pendientes.data || [];

  return (
    <div
      className="grid h-full grid-cols-1 items-start gap-espacio-4 overflow-auto lg:grid-cols-2"
      data-testid="documental-distribucion"
    >
      <Tarjeta padding="p-0">
        <div className="border-b border-borde px-espacio-4 py-espacio-3">
          <CabeceraTarjeta
            titulo={t("documental.distribucion.destinatarios")}
            acciones={
              <Boton tamano="sm" onClick={() => setAbierto(true)}>
                <IconoMas />
                {t("documental.distribucion.agregar")}
              </Boton>
            }
          />
        </div>

        {aviso ? (
          <p
            role="status"
            className={`mx-espacio-4 text-pequeno font-semibold ${aviso.tono === "ok" ? "text-exito-texto" : "text-rojo-alto"}`}
          >
            {aviso.texto}
          </p>
        ) : null}

        {listado.length ? (
          listado.map((destinatario) => (
            <div
              key={destinatario.id}
              className="flex items-center gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3 last:border-b-0"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-pequeno font-semibold text-tinta">
                  {destinatario.nombre}
                </p>
                <p className="truncate text-micro text-tinta-suave">
                  {destinatario.correo}
                </p>
                <div className="mt-espacio-1 flex flex-wrap gap-espacio-1">
                  {(destinatario.familias || []).map((familia) => (
                    <span
                      key={familia}
                      className="rounded-insignia bg-accion-tonal px-espacio-2 py-espacio-1 text-micro font-bold uppercase text-accion-tonal-texto"
                    >
                      {t(`documental.familia.${familia}`)}
                    </span>
                  ))}
                  {(destinatario.situaciones || []).map((situacion) => (
                    <span
                      key={situacion}
                      className="rounded-insignia bg-lienzo px-espacio-2 py-espacio-1 text-micro font-bold uppercase text-tinta-media ring-1 ring-inset ring-borde"
                    >
                      {t(`documental.estado.${situacion}`)}
                    </span>
                  ))}
                </div>
              </div>
              <BotonIcono
                tamano="sm"
                aria-label={t("documental.distribucion.quitar")}
                onClick={() => void eliminar(destinatario.id)}
              >
                <IconoCerrar />
              </BotonIcono>
            </div>
          ))
        ) : (
          <div className="p-espacio-4">
            <Vacio
              titulo={t("documental.distribucion.vacio")}
              detalle={t("documental.distribucion.vacioDetalle")}
            />
          </div>
        )}
      </Tarjeta>

      <div className="flex flex-col gap-espacio-4">
        <Tarjeta padding="p-0">
          <div className="border-b border-borde px-espacio-4 py-espacio-3">
            <CabeceraTarjeta titulo={t("documental.distribucion.enCola")} />
          </div>
          {pendientes.isError ? (
            <div className="p-espacio-4">
              <ErrorPanel
                error={pendientes.error}
                mensaje={mensajeDeError(pendientes.error)}
              />
            </div>
          ) : enCola.length ? (
            enCola.map((envio) => (
              <div
                key={envio.id}
                className="flex items-center gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3 last:border-b-0"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-pequeno font-semibold text-tinta">
                    {envio.nombre_archivo || envio.documento_id}
                  </p>
                  <p className="truncate text-micro text-tinta-suave">
                    {envio.correo}
                  </p>
                </div>
                <span className="text-micro text-tinta-suave">
                  {comoFecha(envio.creado_en, idioma)}
                </span>
              </div>
            ))
          ) : (
            <p className="p-espacio-4 text-pequeno text-tinta-suave">
              {t("documental.distribucion.sinPendientes")}
            </p>
          )}
        </Tarjeta>

        <Tarjeta padding="p-0">
          <div className="border-b border-borde px-espacio-4 py-espacio-3">
            <CabeceraTarjeta titulo={t("documental.distribucion.enviados")} />
          </div>
          {enviados.length ? (
            enviados.map((envio) => (
              <div
                key={envio.id}
                className="flex items-center gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3 last:border-b-0"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-pequeno font-semibold text-tinta">
                    {envio.nombre_archivo || envio.documento_id}
                  </p>
                  <p className="truncate text-micro text-tinta-suave">
                    {envio.correo}
                  </p>
                </div>
                <span className="text-micro text-exito-texto">
                  {comoFecha(envio.enviado_en || envio.creado_en, idioma)}
                </span>
              </div>
            ))
          ) : (
            <p className="p-espacio-4 text-pequeno text-tinta-suave">
              {t("documental.distribucion.sinEnviados")}
            </p>
          )}
        </Tarjeta>
      </div>

      {abierto ? (
        <Panel
          titulo={t("documental.distribucion.agregar")}
          alCerrar={cerrar}
          pie={
            <div className="flex justify-end gap-espacio-2">
              <Boton variante="secundario" onClick={cerrar}>
                {t("documental.visor.cancelar")}
              </Boton>
              <Boton
                variante="primario"
                disabled={!borrador.nombre || !borrador.correo.includes("@")}
                onClick={() => void guardar()}
              >
                {t("documental.visor.guardar")}
              </Boton>
            </div>
          }
        >
          <div className="flex flex-col gap-espacio-4">
            <Campo
              etiqueta={t("documental.distribucion.nombre")}
              value={borrador.nombre}
              onChange={(evento) =>
                setBorrador((previo) => ({
                  ...previo,
                  nombre: evento.target.value,
                }))
              }
            />
            <Campo
              etiqueta={t("documental.distribucion.correo")}
              type="email"
              value={borrador.correo}
              onChange={(evento) =>
                setBorrador((previo) => ({
                  ...previo,
                  correo: evento.target.value,
                }))
              }
            />
            <fieldset>
              <legend className="mb-espacio-2 text-micro font-semibold uppercase tracking-wider text-tinta-suave">
                {t("documental.distribucion.familias")}
              </legend>
              <div className="flex flex-wrap gap-espacio-2">
                {FAMILIAS.map((familia) => (
                  <button
                    key={familia}
                    type="button"
                    aria-pressed={borrador.familias.includes(familia)}
                    onClick={() =>
                      setBorrador((previo) => ({
                        ...previo,
                        familias: alternarLista(previo.familias, familia),
                      }))
                    }
                    className={`rounded-insignia px-espacio-3 py-espacio-1 text-micro font-bold uppercase ring-1 ring-inset transition-colors focus-visible:outline-foco ${
                      borrador.familias.includes(familia)
                        ? "bg-accion-tonal text-accion-tonal-texto ring-accion-primaria"
                        : "bg-superficie text-tinta-media ring-borde hover:ring-borde-fuerte"
                    }`}
                  >
                    {t(`documental.familia.${familia}`)}
                  </button>
                ))}
              </div>
            </fieldset>
            <fieldset>
              <legend className="mb-espacio-2 text-micro font-semibold uppercase tracking-wider text-tinta-suave">
                {t("documental.distribucion.situaciones")}
              </legend>
              <div className="flex flex-wrap gap-espacio-2">
                {SITUACIONES_ENVIO.map((situacion) => (
                  <button
                    key={situacion}
                    type="button"
                    aria-pressed={borrador.situaciones.includes(situacion)}
                    onClick={() =>
                      setBorrador((previo) => ({
                        ...previo,
                        situaciones: alternarLista(
                          previo.situaciones,
                          situacion,
                        ),
                      }))
                    }
                    className={`rounded-insignia px-espacio-3 py-espacio-1 text-micro font-bold uppercase ring-1 ring-inset transition-colors focus-visible:outline-foco ${
                      borrador.situaciones.includes(situacion)
                        ? "bg-accion-tonal text-accion-tonal-texto ring-accion-primaria"
                        : "bg-superficie text-tinta-media ring-borde hover:ring-borde-fuerte"
                    }`}
                  >
                    {t(`documental.estado.${situacion}`)}
                  </button>
                ))}
              </div>
            </fieldset>
          </div>
        </Panel>
      ) : null}
    </div>
  );
}
