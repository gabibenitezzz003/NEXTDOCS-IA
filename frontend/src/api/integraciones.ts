import { clienteProcesos } from "./procesos";

export type TipoIntegracion = "CORREO" | "TELEGRAM" | "WHATSAPP";

export interface Integracion {
  tipo: TipoIntegracion;
  etiqueta: string;
  configurada: boolean;
  habilitada: boolean;
  verificadaEn?: string;
  configuracion: Record<string, string>;
  campos: string[];
  requeridos: string[];
  secretos: string[];
}

export interface ResultadoPrueba {
  exitosa: boolean;
  detalle: string;
}

export async function listarIntegraciones(): Promise<Integracion[]> {
  const { data } = await clienteProcesos.get<Integracion[]>("/integraciones");
  return data;
}

export async function guardarIntegracion(
  tipo: TipoIntegracion,
  configuracion: Record<string, string>,
  habilitada: boolean,
): Promise<Integracion> {
  const { data } = await clienteProcesos.put<Integracion>(
    `/integraciones/${tipo}`,
    { configuracion, habilitada },
  );
  return data;
}

export async function eliminarIntegracion(tipo: TipoIntegracion): Promise<void> {
  await clienteProcesos.delete(`/integraciones/${tipo}`);
}

export async function probarIntegracion(
  tipo: TipoIntegracion,
  destino?: string,
): Promise<ResultadoPrueba> {
  const { data } = await clienteProcesos.post<ResultadoPrueba>(
    `/integraciones/${tipo}/probar`,
    destino ? { destino } : {},
  );
  return data;
}
