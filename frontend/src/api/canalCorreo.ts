import { cliente } from "./cliente";
import type {
  BuzonCorreo,
  CorrelacionCorreo,
  EstadoBuzonCorreo,
  LecturaBuzon,
  MensajeCorreo,
  MensajeSaliente,
  NuevaCorrelacionCorreo,
  NuevoBuzonCorreo,
  Pagina,
  RemitenteAutorizado,
  ResultadoMensajeCorreo,
} from "../tipos/api";

export async function listarBuzones(): Promise<BuzonCorreo[]> {
  const { data } = await cliente.get<BuzonCorreo[]>("/canales/correo/buzones");
  return data;
}

export async function crearBuzon(datos: NuevoBuzonCorreo): Promise<BuzonCorreo> {
  const { data } = await cliente.post<BuzonCorreo>("/canales/correo/buzones", datos);
  return data;
}

export async function cambiarEstadoBuzon(
  buzonId: string,
  estado: EstadoBuzonCorreo,
): Promise<BuzonCorreo> {
  const { data } = await cliente.post<BuzonCorreo>(
    `/canales/correo/buzones/${buzonId}/estado`,
    null,
    { params: { estado } },
  );
  return data;
}

export async function probarBuzon(buzonId: string): Promise<void> {
  await cliente.post(`/canales/correo/buzones/${buzonId}/prueba`);
}

export async function leerBuzon(buzonId: string): Promise<LecturaBuzon> {
  const { data } = await cliente.post<LecturaBuzon>(`/canales/correo/buzones/${buzonId}/lectura`);
  return data;
}

export async function autorizarRemitente(
  buzonId: string,
  patron: string,
  descripcion?: string,
): Promise<RemitenteAutorizado> {
  const { data } = await cliente.post<RemitenteAutorizado>(
    `/canales/correo/buzones/${buzonId}/remitentes`,
    { patron, descripcion },
  );
  return data;
}

export async function revocarRemitente(remitenteId: string): Promise<void> {
  await cliente.delete(`/canales/correo/remitentes/${remitenteId}`);
}

export async function crearCorrelacion(
  datos: NuevaCorrelacionCorreo,
): Promise<CorrelacionCorreo> {
  const { data } = await cliente.post<CorrelacionCorreo>("/canales/correo/correlaciones", datos);
  return data;
}

export async function anularCorrelacion(correlacionId: string): Promise<void> {
  await cliente.delete(`/canales/correo/correlaciones/${correlacionId}`);
}

export async function listarCorrelaciones(
  pagina: number,
  tamano = 20,
): Promise<Pagina<CorrelacionCorreo>> {
  const { data } = await cliente.get<Pagina<CorrelacionCorreo>>("/canales/correo/correlaciones", {
    params: { pagina, tamano },
  });
  return data;
}

export async function listarMensajes(
  resultado: ResultadoMensajeCorreo | undefined,
  pagina: number,
  tamano = 20,
): Promise<Pagina<MensajeCorreo>> {
  const { data } = await cliente.get<Pagina<MensajeCorreo>>("/canales/correo/mensajes", {
    params: { resultado, pagina, tamano },
  });
  return data;
}

export async function obtenerMensaje(mensajeId: string): Promise<MensajeCorreo> {
  const { data } = await cliente.get<MensajeCorreo>(`/canales/correo/mensajes/${mensajeId}`);
  return data;
}

export async function listarSalientes(
  pagina: number,
  tamano = 20,
): Promise<Pagina<MensajeSaliente>> {
  const { data } = await cliente.get<Pagina<MensajeSaliente>>("/canales/correo/salientes", {
    params: { pagina, tamano },
  });
  return data;
}
