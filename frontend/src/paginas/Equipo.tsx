import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { Boton, Campo, Panel, Pastilla, Tarjeta } from "../componentes/Interfaz";
import { IconoCheck, IconoInfo, IconoMas, IconoUsuario } from "../componentes/Iconos";
import { mensajeDeError } from "../api/cliente";
import {
  actualizarUsuario,
  asignarRoles,
  bloquearUsuario,
  crearUsuario,
  desbloquearUsuario,
  eliminarUsuario,
  listarRoles,
  listarUsuarios,
  restablecerClave,
  type UsuarioEquipo,
} from "../api/usuarios";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { useSesion } from "../contextos/ProveedorSesion";
import type { Tono } from "../componentes/Interfaz";

const TONO_ROL: Record<string, Tono> = {
  ADMINISTRADOR: "informacion",
  OPERADOR: "exito",
  REVISOR: "alerta",
  AUDITOR: "neutro",
};

export function Equipo() {
  const { t } = useIdioma();
  const { sesion } = useSesion();
  const clienteConsultas = useQueryClient();
  const [editando, setEditando] = useState<UsuarioEquipo | "nuevo" | null>(null);
  const [claveDe, setClaveDe] = useState<UsuarioEquipo | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const usuarios = useQuery({ queryKey: ["equipo-usuarios"], queryFn: listarUsuarios });
  const roles = useQuery({ queryKey: ["equipo-roles"], queryFn: listarRoles });

  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["equipo-usuarios"] });

  const alternarBloqueo = useMutation({
    mutationFn: (usuario: UsuarioEquipo) =>
      usuario.estado === "BLOQUEADO"
        ? desbloquearUsuario(usuario.id)
        : bloquearUsuario(usuario.id),
    onSuccess: () => {
      setError(null);
      refrescar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const eliminar = useMutation({
    mutationFn: eliminarUsuario,
    onSuccess: () => {
      setError(null);
      setAviso(t("equipo.eliminadoAviso"));
      refrescar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const lista = usuarios.data ?? [];

  return (
    <>
      <Encabezado
        titulo={t("equipo.titulo")}
        descripcion={t("equipo.descripcion")}
        acciones={
          <Boton
            type="button"
            variante="primario"
            onClick={() => setEditando("nuevo")}
          >
            <IconoMas tamano={16} />
            {t("equipo.agregar")}
          </Boton>
        }
      />
      <Contenido>
        <div
          role="note"
          className="mb-espacio-5 flex items-start gap-espacio-3 rounded-panel border border-violeta-borde bg-violeta-tenue p-espacio-4 text-pequeno text-tinta"
        >
          <span aria-hidden="true" className="mt-px shrink-0 text-violeta">
            <IconoInfo tamano={18} />
          </span>
          <p className="min-w-0 [overflow-wrap:anywhere]">
            {t("equipo.comoEntran")}{" "}
            <span className="font-semibold text-accion-tonal-texto">
              {sesion?.codigoTenant}
            </span>
          </p>
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
            <ErrorPanel titulo={t("equipo.errorAccion")} mensaje={error} />
          </div>
        ) : null}

        {usuarios.isPending ? (
          <Cargando filas={3} alto="h-40" />
        ) : usuarios.isError ? (
          <ErrorPanel
            mensaje={mensajeDeError(usuarios.error)}
            error={usuarios.error}
            reintentar={() => usuarios.refetch()}
          />
        ) : lista.length === 0 ? (
          <Vacio
            titulo={t("equipo.sinMiembros")}
            detalle={t("equipo.sinMiembrosDetalle")}
          />
        ) : (
          <ul aria-label={t("equipo.lista")} className="grid gap-espacio-4 lg:grid-cols-2">
            {lista.map((usuario) => (
              <li key={usuario.id} className="min-w-0">
                <TarjetaUsuario
                  usuario={usuario}
                  esPropio={usuario.id === sesion?.usuarioId}
                  enProceso={alternarBloqueo.isPending || eliminar.isPending}
                  alEditar={() => setEditando(usuario)}
                  alClave={() => setClaveDe(usuario)}
                  alAlternarBloqueo={() => alternarBloqueo.mutate(usuario)}
                  alEliminar={() => {
                    if (window.confirm(t("equipo.eliminarConfirmar", { email: usuario.email }))) {
                      eliminar.mutate(usuario.id);
                    }
                  }}
                />
              </li>
            ))}
          </ul>
        )}
      </Contenido>

      {editando ? (
        <PanelUsuario
          usuario={editando === "nuevo" ? null : editando}
          roles={roles.data ?? []}
          alCerrar={() => setEditando(null)}
          alGuardado={(creado) => {
            setEditando(null);
            setAviso(
              creado ? t("equipo.creadoAviso") : t("equipo.actualizadoAviso"),
            );
            setError(null);
            refrescar();
          }}
        />
      ) : null}

      {claveDe ? (
        <PanelClave
          usuario={claveDe}
          alCerrar={() => setClaveDe(null)}
          alGuardado={() => {
            setClaveDe(null);
            setAviso(t("equipo.claveAviso"));
            setError(null);
          }}
          alError={(mensaje) => {
            setClaveDe(null);
            setError(mensaje);
          }}
        />
      ) : null}
    </>
  );
}

function TarjetaUsuario({
  usuario,
  esPropio,
  enProceso,
  alEditar,
  alClave,
  alAlternarBloqueo,
  alEliminar,
}: {
  usuario: UsuarioEquipo;
  esPropio: boolean;
  enProceso: boolean;
  alEditar: () => void;
  alClave: () => void;
  alAlternarBloqueo: () => void;
  alEliminar: () => void;
}) {
  const { t } = useIdioma();
  const iniciales = usuario.nombre
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join("");

  return (
    <Tarjeta className="h-full">
      <div className="flex min-w-0 items-start gap-espacio-3">
        <span
          aria-hidden="true"
          className="flex size-espacio-10 shrink-0 items-center justify-center rounded-insignia bg-violeta-tenue text-pequeno font-bold text-accion-tonal-texto"
        >
          {iniciales || <IconoUsuario tamano={18} />}
        </span>
        <div className="min-w-0 flex-1">
          <h2 className="font-titulo text-titulo-panel [overflow-wrap:anywhere]">
            {usuario.nombre}
            {esPropio ? (
              <span className="ml-espacio-2 align-middle text-micro font-normal text-tinta-suave">
                {t("equipo.vos")}
              </span>
            ) : null}
          </h2>
          <p className="mt-espacio-1 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
            {usuario.email}
          </p>
        </div>
        <Pastilla tono={usuario.estado === "ACTIVO" ? "exito" : "alerta"}>
          {usuario.estado === "ACTIVO"
            ? t("equipo.estadoActivo")
            : usuario.estado === "BLOQUEADO"
              ? t("equipo.estadoBloqueado")
              : usuario.estado}
        </Pastilla>
      </div>
      <div className="mt-espacio-4 flex flex-wrap items-center gap-espacio-2">
        {usuario.roles.length === 0 ? (
          <span className="text-pequeno text-tinta-suave">{t("equipo.sinRoles")}</span>
        ) : (
          usuario.roles.map((rol) => (
            <Pastilla key={rol} tono={TONO_ROL[rol] ?? "neutro"}>
              {t(`equipo.rol.${rol}`) === `equipo.rol.${rol}` ? rol : t(`equipo.rol.${rol}`)}
            </Pastilla>
          ))
        )}
        {usuario.origenIdentidad && usuario.origenIdentidad !== "LOCAL" ? (
          <Pastilla tono="neutro">{usuario.origenIdentidad}</Pastilla>
        ) : null}
      </div>
      <div className="mt-espacio-5 flex flex-wrap gap-espacio-2 border-t border-borde pt-espacio-4">
        <Boton type="button" variante="secundario" tamano="sm" onClick={alEditar}>
          {t("equipo.editar")}
        </Boton>
        <Boton type="button" variante="secundario" tamano="sm" onClick={alClave}>
          {t("equipo.restablecerClave")}
        </Boton>
        <Boton
          type="button"
          variante="secundario"
          tamano="sm"
          disabled={enProceso || esPropio}
          onClick={alAlternarBloqueo}
        >
          {usuario.estado === "BLOQUEADO"
            ? t("equipo.desbloquear")
            : t("equipo.bloquear")}
        </Boton>
        {!esPropio ? (
          <Boton
            type="button"
            variante="peligro"
            tamano="sm"
            disabled={enProceso}
            onClick={alEliminar}
          >
            {t("equipo.eliminar")}
          </Boton>
        ) : null}
      </div>
    </Tarjeta>
  );
}

function PanelUsuario({
  usuario,
  roles,
  alCerrar,
  alGuardado,
}: {
  usuario: UsuarioEquipo | null;
  roles: { id: string; codigo: string; nombre: string }[];
  alCerrar: () => void;
  alGuardado: (creado: boolean) => void;
}) {
  const { t } = useIdioma();
  const [nombre, setNombre] = useState(usuario?.nombre ?? "");
  const [email, setEmail] = useState(usuario?.email ?? "");
  const [clave, setClave] = useState("");
  const [seleccionados, setSeleccionados] = useState<string[]>(usuario?.roles ?? []);
  const [errorLocal, setErrorLocal] = useState<string | null>(null);

  const guardar = useMutation({
    mutationFn: async () => {
      if (usuario) {
        const actualizado = await actualizarUsuario(usuario.id, {
          nombre: nombre.trim(),
          email: email.trim(),
          roles: seleccionados,
        });
        await asignarRoles(usuario.id, seleccionados);
        return actualizado;
      }
      return crearUsuario({
        nombre: nombre.trim(),
        email: email.trim(),
        clave,
        roles: seleccionados,
      });
    },
    onSuccess: () => alGuardado(!usuario),
    onError: (fallo) => setErrorLocal(mensajeDeError(fallo)),
  });

  function alternarRol(codigo: string) {
    setSeleccionados((previo) =>
      previo.includes(codigo)
        ? previo.filter((actual) => actual !== codigo)
        : [...previo, codigo],
    );
  }

  return (
    <Panel
      titulo={usuario ? t("equipo.editarTitulo") : t("equipo.nuevoTitulo")}
      descripcion={
        usuario ? t("equipo.editarDescripcion") : t("equipo.nuevoDescripcion")
      }
      alCerrar={alCerrar}
      pie={
        <div className="flex flex-wrap justify-end gap-espacio-2">
          <Boton type="button" variante="secundario" onClick={alCerrar}>
            {t("comun.cancelar")}
          </Boton>
          <Boton
            type="button"
            variante="primario"
            cargando={guardar.isPending}
            disabled={
              guardar.isPending ||
              !nombre.trim() ||
              !email.trim() ||
              (!usuario && clave.length < 12) ||
              seleccionados.length === 0
            }
            onClick={() => guardar.mutate()}
          >
            {usuario ? t("equipo.guardar") : t("equipo.crear")}
          </Boton>
        </div>
      }
    >
      <div className="space-y-espacio-4">
        {errorLocal ? (
          <div className="mb-espacio-4">
            <ErrorPanel titulo={t("equipo.errorAccion")} mensaje={errorLocal} />
          </div>
        ) : null}
        <Campo
          etiqueta={t("equipo.campoNombre")}
          value={nombre}
          onChange={(evento) => setNombre(evento.target.value)}
          required
          autoComplete="name"
        />
        <Campo
          etiqueta={t("equipo.campoEmail")}
          type="email"
          value={email}
          onChange={(evento) => setEmail(evento.target.value)}
          required
          autoComplete="off"
          placeholder="nombre@empresa.com"
        />
        {!usuario ? (
          <Campo
            etiqueta={t("equipo.campoClave")}
            ayuda={t("equipo.campoClaveAyuda")}
            type="password"
            value={clave}
            onChange={(evento) => setClave(evento.target.value)}
            required
            autoComplete="new-password"
          />
        ) : null}
        <fieldset>
          <legend className="mb-espacio-2 text-pequeno font-semibold text-tinta">
            {t("equipo.campoRoles")}
          </legend>
          <ul className="space-y-espacio-2">
            {roles.map((rol) => (
              <li key={rol.id}>
                <label className="flex cursor-pointer items-start gap-espacio-3 rounded-control border border-borde p-espacio-3 transition-colors hover:border-accion-primaria">
                  <input
                    type="checkbox"
                    className="mt-espacio-1 size-espacio-4 accent-accion-primaria"
                    checked={seleccionados.includes(rol.codigo)}
                    onChange={() => alternarRol(rol.codigo)}
                  />
                  <span className="min-w-0">
                    <span className="block text-pequeno font-semibold">
                      {t(`equipo.rol.${rol.codigo}`) !== `equipo.rol.${rol.codigo}`
                        ? t(`equipo.rol.${rol.codigo}`)
                        : rol.nombre}
                    </span>
                    <span className="block text-micro text-tinta-suave">
                      {t(`equipo.rolDesc.${rol.codigo}`) !== `equipo.rolDesc.${rol.codigo}`
                        ? t(`equipo.rolDesc.${rol.codigo}`)
                        : ""}
                    </span>
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </fieldset>
      </div>
    </Panel>
  );
}

function PanelClave({
  usuario,
  alCerrar,
  alGuardado,
  alError,
}: {
  usuario: UsuarioEquipo;
  alCerrar: () => void;
  alGuardado: () => void;
  alError: (mensaje: string) => void;
}) {
  const { t } = useIdioma();
  const [clave, setClave] = useState("");
  const [errorLocal, setErrorLocal] = useState<string | null>(null);

  const guardar = useMutation({
    mutationFn: () => restablecerClave(usuario.id, clave),
    onSuccess: alGuardado,
    onError: (fallo) => {
      const mensaje = mensajeDeError(fallo);
      setErrorLocal(mensaje);
      alError(mensaje);
    },
  });

  return (
    <Panel
      titulo={t("equipo.claveTitulo")}
      descripcion={t("equipo.claveDescripcion", { email: usuario.email })}
      alCerrar={alCerrar}
      pie={
        <div className="flex flex-wrap justify-end gap-espacio-2">
          <Boton type="button" variante="secundario" onClick={alCerrar}>
            {t("comun.cancelar")}
          </Boton>
          <Boton
            type="button"
            variante="primario"
            cargando={guardar.isPending}
            disabled={guardar.isPending || clave.length < 12}
            onClick={() => guardar.mutate()}
          >
            {t("equipo.claveGuardar")}
          </Boton>
        </div>
      }
    >
      {errorLocal ? (
        <div className="mb-espacio-4">
          <ErrorPanel titulo={t("equipo.errorAccion")} mensaje={errorLocal} />
        </div>
      ) : null}
      <Campo
        etiqueta={t("equipo.campoClaveNueva")}
        ayuda={t("equipo.campoClaveAyuda")}
        type="password"
        value={clave}
        onChange={(evento) => setClave(evento.target.value)}
        required
        autoComplete="new-password"
      />
    </Panel>
  );
}
