import { useState } from "react";
import type { FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
  crearDelegacion,
  crearOrganizacionPartner,
  desactivarOrganizacionPartner,
  listarDelegaciones,
  listarOrganizacionesPartner,
  revocarDelegacion,
} from "../api/procesos";
import type { OrganizacionPartner } from "../api/procesos";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "../utilidades/fechas";
import { useIdioma } from "../contextos/ProveedorIdioma";
import type { Tono } from "../componentes/Interfaz";

const TONO_ESTADO: Record<string, Tono> = {
  ACTIVA: "exito",
  SUSPENDIDA: "alerta",
  INACTIVA: "neutro",
};

const SCOPES_DISPONIBLES = [
  "procesos.leer",
  "procesos.escribir",
  "instancias.leer",
  "tareas.completar",
];

type Seccion = "organizaciones" | "delegaciones";

export function Partners() {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [seccion, setSeccion] = useState<Seccion>("organizaciones");
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formularioOrg, setFormularioOrg] = useState(false);
  const [formularioDeleg, setFormularioDeleg] = useState(false);
  const [partnerDelegacion, setPartnerDelegacion] = useState("");

  const organizaciones = useQuery({
    queryKey: ["partners-organizaciones"],
    queryFn: listarOrganizacionesPartner,
  });
  const delegaciones = useQuery({
    queryKey: ["partners-delegaciones"],
    queryFn: listarDelegaciones,
  });

  const refrescar = () => {
    clienteConsultas.invalidateQueries({ queryKey: ["partners-organizaciones"] });
    clienteConsultas.invalidateQueries({ queryKey: ["partners-delegaciones"] });
  };

  const alExito = (texto: string) => {
    setError(null);
    setAviso(texto);
    setFormularioOrg(false);
    setFormularioDeleg(false);
    refrescar();
  };

  const alError = (fallo: unknown) => {
    setAviso(null);
    setError(mensajeDeError(fallo));
  };

  const crearOrg = useMutation({
    mutationFn: crearOrganizacionPartner,
    onSuccess: () => alExito(t("partners.organizacionCreada")),
    onError: alError,
  });

  const desactivarOrg = useMutation({
    mutationFn: desactivarOrganizacionPartner,
    onSuccess: () => alExito(t("partners.organizacionDesactivada")),
    onError: alError,
  });

  const crearDeleg = useMutation({
    mutationFn: crearDelegacion,
    onSuccess: () => alExito(t("partners.delegacionCreada")),
    onError: alError,
  });

  const revocarDeleg = useMutation({
    mutationFn: revocarDelegacion,
    onSuccess: () => alExito(t("partners.delegacionRevocada")),
    onError: alError,
  });

  const alCrearOrg = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const datos = new FormData(evento.currentTarget);
    crearOrg.mutate({
      codigo: String(datos.get("codigo") ?? "").trim(),
      nombre: String(datos.get("nombre") ?? "").trim(),
      emailContacto: String(datos.get("emailContacto") ?? "").trim() || undefined,
    });
  };

  const alCrearDeleg = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const datos = new FormData(evento.currentTarget);
    const scopes = datos.getAll("scopes").map((scope) => String(scope));
    crearDeleg.mutate({
      partnerId: partnerDelegacion,
      tenantClienteId: String(datos.get("tenantClienteId") ?? "").trim(),
      scopes: scopes.length > 0 ? scopes : undefined,
      expiracion: String(datos.get("expiracion") ?? "") || undefined,
      aprobador: String(datos.get("aprobador") ?? "").trim() || undefined,
    });
  };

  const listaOrg = organizaciones.data ?? [];
  const listaDeleg = delegaciones.data ?? [];
  const activas = listaOrg.filter((org) => (org.estado ?? "ACTIVA") === "ACTIVA");

  return (
    <>
      <Encabezado
        titulo={t("partners.titulo")}
        descripcion={t("partners.descripcion")}
      />
      <Contenido>
        <div className="mb-espacio-5 flex flex-wrap items-center justify-between gap-espacio-3">
          <GrupoSegmentado
            etiqueta={t("partners.grupoSeccion")}
            opciones={[
              { valor: "organizaciones" as Seccion, texto: t("partners.organizaciones") },
              { valor: "delegaciones" as Seccion, texto: t("partners.delegaciones") },
            ]}
            valor={seccion}
            alCambiar={setSeccion}
          />
          {seccion === "organizaciones" ? (
            <Boton
              variante="primario"
              onClick={() => setFormularioOrg((abierto) => !abierto)}
            >
              {t("partners.nuevaOrganizacion")}
            </Boton>
          ) : (
            <Boton
              variante="primario"
              onClick={() => setFormularioDeleg((abierto) => !abierto)}
              disabled={activas.length === 0}
            >
              {t("partners.nuevaDelegacion")}
            </Boton>
          )}
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
            <ErrorPanel titulo={t("partners.errorAccion")} mensaje={error} />
          </div>
        ) : null}

        {seccion === "organizaciones" ? (
          <>
            {formularioOrg ? (
              <Tarjeta className="mb-espacio-5">
                <form onSubmit={alCrearOrg} className="space-y-espacio-4">
                  <div className="grid gap-espacio-4 md:grid-cols-3">
                    <Campo
                      etiqueta={t("partners.codigo")}
                      name="codigo"
                      required
                      maxLength={32}
                      placeholder="ACME-PARTNER"
                    />
                    <Campo
                      etiqueta={t("partners.nombre")}
                      name="nombre"
                      required
                      maxLength={128}
                    />
                    <Campo
                      etiqueta={t("partners.emailContacto")}
                      name="emailContacto"
                      type="email"
                    />
                  </div>
                  <div className="flex justify-end gap-espacio-3">
                    <Boton type="button" onClick={() => setFormularioOrg(false)}>
                      {t("comun.cancelar")}
                    </Boton>
                    <Boton variante="primario" cargando={crearOrg.isPending}>
                      {t("comun.guardar")}
                    </Boton>
                  </div>
                </form>
              </Tarjeta>
            ) : null}
            {organizaciones.isPending ? (
              <Cargando filas={3} alto="h-64" />
            ) : organizaciones.isError ? (
              <ErrorPanel
                mensaje={mensajeDeError(organizaciones.error)}
                error={organizaciones.error}
                reintentar={() => organizaciones.refetch()}
              />
            ) : listaOrg.length === 0 ? (
              <Vacio
                titulo={t("partners.sinOrganizaciones")}
                detalle={t("partners.sinOrganizacionesDetalle")}
              />
            ) : (
              <ul
                aria-label={t("partners.organizaciones")}
                className="space-y-espacio-4"
              >
                {listaOrg.map((org) => (
                  <TarjetaOrganizacion
                    key={org.id}
                    organizacion={org}
                    alDesactivar={() => desactivarOrg.mutate(org.id)}
                    desactivando={
                      desactivarOrg.isPending && desactivarOrg.variables === org.id
                    }
                    t={t}
                  />
                ))}
              </ul>
            )}
          </>
        ) : (
          <>
            {formularioDeleg ? (
              <Tarjeta className="mb-espacio-5">
                <form onSubmit={alCrearDeleg} className="space-y-espacio-4">
                  <div className="grid gap-espacio-4 md:grid-cols-2">
                    <Selector
                      etiqueta={t("partners.partner")}
                      name="partnerId"
                      required
                      value={partnerDelegacion}
                      onChange={(evento) =>
                        setPartnerDelegacion(evento.target.value)
                      }
                    >
                      <option value="" disabled>
                        {t("partners.elegirPartner")}
                      </option>
                      {activas.map((org) => (
                        <option key={org.id} value={org.id}>
                          {org.nombre} ({org.codigo})
                        </option>
                      ))}
                    </Selector>
                    <Campo
                      etiqueta={t("partners.tenantCliente")}
                      name="tenantClienteId"
                      required
                      ayuda={t("partners.tenantClienteAyuda")}
                    />
                    <Campo
                      etiqueta={t("partners.expiracion")}
                      name="expiracion"
                      type="datetime-local"
                    />
                    <Campo
                      etiqueta={t("partners.aprobador")}
                      name="aprobador"
                    />
                  </div>
                  <fieldset className="space-y-espacio-2">
                    <legend className="text-pequeno font-semibold text-tinta">
                      {t("partners.scopes")}
                    </legend>
                    <div className="flex flex-wrap gap-espacio-4">
                      {SCOPES_DISPONIBLES.map((scope) => (
                        <label
                          key={scope}
                          className="flex items-center gap-espacio-2 text-pequeno text-tinta"
                        >
                          <input type="checkbox" name="scopes" value={scope} />
                          {scope}
                        </label>
                      ))}
                    </div>
                  </fieldset>
                  <div className="flex justify-end gap-espacio-3">
                    <Boton type="button" onClick={() => setFormularioDeleg(false)}>
                      {t("comun.cancelar")}
                    </Boton>
                    <Boton variante="primario" cargando={crearDeleg.isPending}>
                      {t("comun.guardar")}
                    </Boton>
                  </div>
                </form>
              </Tarjeta>
            ) : null}
            {delegaciones.isPending ? (
              <Cargando filas={3} alto="h-64" />
            ) : delegaciones.isError ? (
              <ErrorPanel
                mensaje={mensajeDeError(delegaciones.error)}
                error={delegaciones.error}
                reintentar={() => delegaciones.refetch()}
              />
            ) : listaDeleg.length === 0 ? (
              <Vacio
                titulo={t("partners.sinDelegaciones")}
                detalle={t("partners.sinDelegacionesDetalle")}
              />
            ) : (
              <ul
                aria-label={t("partners.delegaciones")}
                className="space-y-espacio-4"
              >
                {listaDeleg.map((delegacion) => (
                  <li key={delegacion.id} className="min-w-0">
                    <Tarjeta>
                      <div className="flex flex-wrap items-start justify-between gap-espacio-3">
                        <div className="min-w-0 space-y-espacio-1">
                          <p className="text-pequeno font-semibold text-tinta">
                            {listaOrg.find((org) => org.id === delegacion.partnerId)
                              ?.nombre ?? delegacion.partnerId}
                          </p>
                          <p className="text-pequeno text-tinta-suave">
                            {t("partners.tenantCliente")}: {delegacion.tenantClienteId}
                          </p>
                          {delegacion.scopes && delegacion.scopes.length > 0 ? (
                            <div className="flex flex-wrap gap-espacio-2 pt-espacio-1">
                              {delegacion.scopes.map((scope) => (
                                <Pastilla key={scope} tono="informacion">
                                  {scope}
                                </Pastilla>
                              ))}
                            </div>
                          ) : null}
                          {delegacion.expiracion ? (
                            <p className="text-pequeno text-tinta-suave">
                              {t("partners.expira")}: {formatearFecha(delegacion.expiracion)}
                            </p>
                          ) : null}
                        </div>
                        <Boton
                          variante="peligro"
                          tamano="sm"
                          cargando={
                            revocarDeleg.isPending &&
                            revocarDeleg.variables === delegacion.id
                          }
                          onClick={() => revocarDeleg.mutate(delegacion.id)}
                        >
                          {t("partners.revocar")}
                        </Boton>
                      </div>
                    </Tarjeta>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </Contenido>
    </>
  );
}

function TarjetaOrganizacion({
  organizacion,
  alDesactivar,
  desactivando,
  t,
}: {
  organizacion: OrganizacionPartner;
  alDesactivar: () => void;
  desactivando: boolean;
  t: (clave: string) => string;
}) {
  const estado = organizacion.estado ?? "ACTIVA";
  const activa = estado === "ACTIVA";
  return (
    <li className="min-w-0">
      <Tarjeta>
        <div className="flex flex-wrap items-start justify-between gap-espacio-3">
          <div className="min-w-0 space-y-espacio-1">
            <div className="flex items-center gap-espacio-3">
              <p className="text-pequeno font-semibold text-tinta">
                {organizacion.nombre}
              </p>
              <Pastilla tono={TONO_ESTADO[estado] ?? "neutro"}>
                {t(`partners.estado${estado}`)}
              </Pastilla>
            </div>
            <p className="text-pequeno text-tinta-suave">
              {t("partners.codigo")}: {organizacion.codigo}
            </p>
            {organizacion.emailContacto ? (
              <p className="text-pequeno text-tinta-suave">
                {organizacion.emailContacto}
              </p>
            ) : null}
            {organizacion.delegaciones && organizacion.delegaciones.length > 0 ? (
              <p className="text-pequeno text-tinta-suave">
                {t("partners.conteoDelegaciones")}: {organizacion.delegaciones.length}
              </p>
            ) : null}
          </div>
          {activa ? (
            <Boton
              variante="peligro"
              tamano="sm"
              cargando={desactivando}
              onClick={alDesactivar}
            >
              {t("partners.desactivar")}
            </Boton>
          ) : null}
        </div>
      </Tarjeta>
    </li>
  );
}
