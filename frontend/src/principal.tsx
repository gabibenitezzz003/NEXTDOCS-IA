import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { Aplicacion } from "./Aplicacion";
import { ProveedorSesion } from "./contextos/ProveedorSesion";
import { ProveedorTema } from "./contextos/ProveedorTema";
import "./estilos.css";

const clienteConsultas = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 15_000,
    },
  },
});

createRoot(document.getElementById("raiz")!).render(
  <StrictMode>
    <ProveedorTema>
      <QueryClientProvider client={clienteConsultas}>
        <BrowserRouter>
          <ProveedorSesion>
            <Aplicacion />
          </ProveedorSesion>
        </BrowserRouter>
      </QueryClientProvider>
    </ProveedorTema>
  </StrictMode>,
);
