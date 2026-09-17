import { cliente } from "./cliente";

export interface UsuarioEquipo {
  id: string;
  email: string;
  nombre: string;
  estado: "ACTIVO" | "BLOQUEADO" | "BAJA" | string;
  origenIdentidad?: string;
  administrador?: boolean;
  ultimoAcceso?: string;
  alta?: string;
  roles: string[];
}

export interface RolEquipo {
  id: string;
  codigo: string;
  nombre: string;
  predefinido: boolean;
}

export interface PaginaUsuarios {
  content: UsuarioEquipo[];
  totalElements: number;
}

export interface UsuarioFormulario {
  email: string;
  nombre: string;
  clave?: string;
  idioma?: string;
  roles: string[];
}

export async function listarUsuarios(): Promise<UsuarioEquipo[]> {
  const { data } = await cliente.get<PaginaUsuarios>("/administracion/usuarios", {
    params: { tamano: 100, orden: "email,asc" },
  });
  return data.content ?? [];
}

export async function listarRoles(): Promise<RolEquipo[]> {
  const { data } = await cliente.get<RolEquipo[]>("/administracion/roles");
  return data;
}

export async function crearUsuario(datos: UsuarioFormulario): Promise<UsuarioEquipo> {
  const { data } = await cliente.post<UsuarioEquipo>("/administracion/usuarios", datos);
  return data;
}

export async function actualizarUsuario(
  usuarioId: string,
  datos: UsuarioFormulario,
): Promise<UsuarioEquipo> {
  const { data } = await cliente.put<UsuarioEquipo>(
    `/administracion/usuarios/${usuarioId}`,
    datos,
  );
  return data;
}

export async function asignarRoles(
  usuarioId: string,
  roles: string[],
): Promise<UsuarioEquipo> {
  const { data } = await cliente.put<UsuarioEquipo>(
    `/administracion/usuarios/${usuarioId}/roles`,
    { roles },
  );
  return data;
}

export async function bloquearUsuario(usuarioId: string): Promise<UsuarioEquipo> {
  const { data } = await cliente.post<UsuarioEquipo>(
    `/administracion/usuarios/${usuarioId}/bloquear`,
  );
  return data;
}

export async function desbloquearUsuario(usuarioId: string): Promise<UsuarioEquipo> {
  const { data } = await cliente.post<UsuarioEquipo>(
    `/administracion/usuarios/${usuarioId}/desbloquear`,
  );
  return data;
}

export async function restablecerClave(
  usuarioId: string,
  claveNueva: string,
): Promise<void> {
  await cliente.post(`/administracion/usuarios/${usuarioId}/clave`, { claveNueva });
}

export async function eliminarUsuario(usuarioId: string): Promise<void> {
  await cliente.delete(`/administracion/usuarios/${usuarioId}`);
}
