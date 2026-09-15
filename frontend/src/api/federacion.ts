import { cliente } from "./cliente";

export interface ProveedorOauth {
  codigo: string;
  nombre: string;
}

export async function listarProveedoresOauth(codigoTenant: string): Promise<ProveedorOauth[]> {
  const { data } = await cliente.get<ProveedorOauth[]>("/federacion/oauth/proveedores", {
    params: { tenant: codigoTenant },
  });
  return data;
}

export function urlInicioOauth(codigoTenant: string, codigoProveedor: string): string {
  const retorno = encodeURIComponent(window.location.origin);
  return `/api/v1/federacion/oauth/${encodeURIComponent(codigoTenant)}/${encodeURIComponent(codigoProveedor)}/iniciar?retorno=${retorno}`;
}
