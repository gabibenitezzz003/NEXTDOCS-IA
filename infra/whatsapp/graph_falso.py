import json
import os
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

TOKEN = os.environ.get("GRAPH_FALSO_TOKEN", "token-de-acceso-de-prueba")
PUERTO = int(os.environ.get("GRAPH_FALSO_PUERTO", "8091"))

enviados = []


def construir_pdf(texto):
    objetos = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 200] /Resources "
        b"<< /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]
    flujo = ("BT /F1 14 Tf 20 120 Td (" + texto + ") Tj ET").encode("latin-1")
    objetos.append(b"<< /Length " + str(len(flujo)).encode() + b" >>\nstream\n" + flujo + b"\nendstream")

    cuerpo = b"%PDF-1.4\n"
    posiciones = []
    for indice, objeto in enumerate(objetos, start=1):
        posiciones.append(len(cuerpo))
        cuerpo += str(indice).encode() + b" 0 obj\n" + objeto + b"\nendobj\n"

    inicio_xref = len(cuerpo)
    cuerpo += b"xref\n0 " + str(len(objetos) + 1).encode() + b"\n0000000000 65535 f \n"
    for posicion in posiciones:
        cuerpo += str(posicion).zfill(10).encode() + b" 00000 n \n"
    cuerpo += (
        b"trailer\n<< /Size " + str(len(objetos) + 1).encode() + b" /Root 1 0 R >>\nstartxref\n"
        + str(inicio_xref).encode() + b"\n%%EOF\n"
    )
    return cuerpo


CATALOGO = {
    "remito": ("application/pdf", construir_pdf("REMITO 4477 - NEXT DOC AI")),
    "factura": ("application/pdf", construir_pdf("FACTURA A 0001-00099887")),
    "disfrazado": ("application/pdf", b"MZ\x90\x00 esto no es un pdf, es un ejecutable renombrado"),
    "enorme": ("application/pdf", construir_pdf("GRANDE")),
}

TAMANOS_DECLARADOS = {"enorme": 999999999}


class Manejador(BaseHTTPRequestHandler):

    def log_message(self, formato, *argumentos):
        print("graph-falso " + formato % argumentos, flush=True)

    def _responder(self, codigo, tipo, cuerpo):
        self.send_response(codigo)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)

    def _autorizado(self):
        if self.headers.get("Authorization") == "Bearer " + TOKEN:
            return True
        self._responder(401, "application/json", json.dumps(
            {"error": {"message": "token de acceso invalido", "type": "OAuthException"}}).encode())
        return False

    def do_GET(self):
        if self.path == "/salud":
            self._responder(200, "application/json", json.dumps({"estado": "arriba"}).encode())
            return
        if self.path == "/enviados":
            self._responder(200, "application/json", json.dumps(enviados).encode())
            return
        if not self._autorizado():
            return

        descarga = re.match(r"^/descarga/([^/?]+)", self.path)
        if descarga:
            identificador = descarga.group(1)
            if identificador not in CATALOGO:
                self._responder(404, "application/json", b"{}")
                return
            tipo, contenido = CATALOGO[identificador]
            self._responder(200, tipo, contenido)
            return

        identificador = self.path.rstrip("/").split("/")[-1].split("?")[0]
        if identificador in CATALOGO:
            tipo, contenido = CATALOGO[identificador]
            metadatos = {
                "url": "http://graph-falso:%d/descarga/%s" % (PUERTO, identificador),
                "mime_type": tipo,
                "file_size": TAMANOS_DECLARADOS.get(identificador, len(contenido)),
                "id": identificador,
                "messaging_product": "whatsapp",
            }
            self._responder(200, "application/json", json.dumps(metadatos).encode())
            return

        self._responder(200, "application/json", json.dumps(
            {"id": identificador, "display_phone_number": "+541150000000",
             "verified_name": "NEXT DOC AI (simulado)"}).encode())

    def do_POST(self):
        if not self._autorizado():
            return
        largo = int(self.headers.get("Content-Length", "0"))
        cuerpo = self.rfile.read(largo).decode("utf-8") if largo else "{}"
        enviados.append(json.loads(cuerpo))
        identificador = "wamid.SALIDA%04d" % len(enviados)
        self._responder(200, "application/json", json.dumps(
            {"messaging_product": "whatsapp", "messages": [{"id": identificador}]}).encode())


if __name__ == "__main__":
    print("graph-falso escuchando en %d con %d medias" % (PUERTO, len(CATALOGO)), flush=True)
    ThreadingHTTPServer(("0.0.0.0", PUERTO), Manejador).serve_forever()
