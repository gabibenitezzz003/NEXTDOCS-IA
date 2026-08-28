import { cliente } from "./cliente";
import type { DetalleDocumento, Documento, EstadoDocumento, Pagina, RevisionDocumento } from "../tipos/api";

export interface FiltroDocumentos {
  estados?: EstadoDocumento[];
  origen?: string;
  codigoPlantilla?: string;
  texto?: string;
  soloRaiz?: boolean;
  pagina?: number;
  tamano?: number;
  orden?: string;
}

export async function listarDocumentos(filtro: FiltroDocumentos): Promise<Pagina<Documento>> {
  const { data } = await cliente.get<Pagina<Documento>>("/documentos", {
    params: {
      ...filtro,
      estados: filtro.estados?.length ? filtro.estados.join(",") : undefined,
    },
  });
  return data;
}

export async function obtenerDetalle(documentoId: string): Promise<DetalleDocumento> {
  const { data } = await cliente.get<DetalleDocumento>(`/documentos/${documentoId}/detalle`);
  return data;
}

export async function obtenerResumen(): Promise<Record<string, number>> {
  const { data } = await cliente.get<Record<string, number>>("/documentos/resumen");
  return data;
}

export async function urlOriginal(documentoId: string): Promise<string> {
  const { data } = await cliente.get<{ url: string }>(`/documentos/${documentoId}/original`);
  return data.url;
}

export interface DatosRevision {
  decision: string;
  motivo?: string;
  correcciones?: Record<string, string>;
  hallazgosSobreescritos?: string[];
}

export async function revisar(documentoId: string, datos: DatosRevision): Promise<RevisionDocumento> {
  const { data } = await cliente.post<RevisionDocumento>(`/documentos/${documentoId}/revisiones`, datos);
  return data;
}

export async function reprocesar(documentoId: string): Promise<Documento> {
  const { data } = await cliente.post<Documento>(`/documentos/${documentoId}/reprocesar`);
  return data;
}

export async function cerrar(documentoId: string): Promise<Documento> {
  const { data } = await cliente.post<Documento>(`/documentos/${documentoId}/cerrar`);
  return data;
}

export async function seleccionarCandidato(documentoId: string, candidatoId: string, motivo: string) {
  const { data } = await cliente.post(`/documentos/${documentoId}/candidatos/${candidatoId}/seleccionar`, {
    motivo,
  });
  return data;
}

export async function ingresarDocumento(archivo: File, codigoPlantilla?: string): Promise<Documento> {
  const formulario = new FormData();
  formulario.append("archivo", archivo);
  formulario.append(
    "datos",
    new Blob([JSON.stringify({ origen: "WEB", codigoPlantilla })], { type: "application/json" }),
  );
  const { data } = await cliente.post<Documento>("/documentos", formulario, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}
