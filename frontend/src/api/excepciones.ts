import { cliente } from "./cliente";
import type { EstadoExcepcion, Excepcion, Pagina } from "../tipos/api";

export async function listarExcepciones(
  estado: EstadoExcepcion | undefined,
  pagina: number,
  tamano: number,
): Promise<Pagina<Excepcion>> {
  const { data } = await cliente.get<Pagina<Excepcion>>("/excepciones", {
    params: { estado, pagina, tamano },
  });
  return data;
}

export async function resolverExcepcion(excepcionId: string, resolucion: string): Promise<Excepcion> {
  const { data } = await cliente.post<Excepcion>(`/excepciones/${excepcionId}/resolver`, { resolucion });
  return data;
}
