import { cliente } from "./cliente";
import type {
  ContactoWhatsapp,
  CorrelacionWhatsapp,
  EstadoLineaWhatsapp,
  LineaWhatsapp,
  MensajeWhatsapp,
  MensajeWhatsappSaliente,
  NuevaCorrelacionWhatsapp,
  NuevaLineaWhatsapp,
  Pagina,
  ResultadoMensajeWhatsapp,
} from "../tipos/api";

export async function listarLineas(): Promise<LineaWhatsapp[]> {
  const { data } = await cliente.get<LineaWhatsapp[]>("/canales/whatsapp/lineas");
  return data;
}

export async function crearLinea(datos: NuevaLineaWhatsapp): Promise<LineaWhatsapp> {
  const { data } = await cliente.post<LineaWhatsapp>("/canales/whatsapp/lineas", datos);
  return data;
}

export async function cambiarEstadoLinea(
  lineaId: string,
  estado: EstadoLineaWhatsapp,
): Promise<LineaWhatsapp> {
  const { data } = await cliente.post<LineaWhatsapp>(
    `/canales/whatsapp/lineas/${lineaId}/estado`,
    null,
    { params: { estado } },
  );
  return data;
}

export async function probarLinea(lineaId: string): Promise<void> {
  await cliente.post(`/canales/whatsapp/lineas/${lineaId}/prueba`);
}

export async function autorizarContacto(
  lineaId: string,
  patron: string,
  descripcion?: string,
): Promise<ContactoWhatsapp> {
  const { data } = await cliente.post<ContactoWhatsapp>(
    `/canales/whatsapp/lineas/${lineaId}/contactos`,
    { patron, descripcion },
  );
  return data;
}

export async function revocarContacto(contactoId: string): Promise<void> {
  await cliente.delete(`/canales/whatsapp/contactos/${contactoId}`);
}

export async function crearCorrelacion(
  datos: NuevaCorrelacionWhatsapp,
): Promise<CorrelacionWhatsapp> {
  const { data } = await cliente.post<CorrelacionWhatsapp>(
    "/canales/whatsapp/correlaciones",
    datos,
  );
  return data;
}

export async function anularCorrelacion(correlacionId: string): Promise<void> {
  await cliente.delete(`/canales/whatsapp/correlaciones/${correlacionId}`);
}

export async function listarCorrelaciones(
  pagina: number,
  tamano = 20,
): Promise<Pagina<CorrelacionWhatsapp>> {
  const { data } = await cliente.get<Pagina<CorrelacionWhatsapp>>(
    "/canales/whatsapp/correlaciones",
    { params: { pagina, tamano } },
  );
  return data;
}

export async function listarMensajes(
  resultado: ResultadoMensajeWhatsapp | undefined,
  pagina: number,
  tamano = 20,
): Promise<Pagina<MensajeWhatsapp>> {
  const { data } = await cliente.get<Pagina<MensajeWhatsapp>>("/canales/whatsapp/mensajes", {
    params: { resultado, pagina, tamano },
  });
  return data;
}

export async function obtenerMensaje(mensajeId: string): Promise<MensajeWhatsapp> {
  const { data } = await cliente.get<MensajeWhatsapp>(`/canales/whatsapp/mensajes/${mensajeId}`);
  return data;
}

export async function listarSalientes(
  pagina: number,
  tamano = 20,
): Promise<Pagina<MensajeWhatsappSaliente>> {
  const { data } = await cliente.get<Pagina<MensajeWhatsappSaliente>>(
    "/canales/whatsapp/salientes",
    { params: { pagina, tamano } },
  );
  return data;
}
