#!/usr/bin/env bash
# Respaldo y restauracion de NextDocs: bases PostgreSQL y objetos de MinIO.
set -euo pipefail

CONTENEDOR_PG="${NEXTDOCS_PG_CONTENEDOR:-nextdocs-postgres}"
USUARIO_PG="${NEXTDOCS_PG_USUARIO:-nextdocs}"
BASES="${NEXTDOCS_PG_BASES:-nextdocs nextdocs_workflow}"
VOLUMEN_MINIO="${NEXTDOCS_MINIO_VOLUMEN:-nextdocs-ai_minio-datos}"
IMAGEN_PG="${NEXTDOCS_IMAGEN_PG:-postgres:16}"

uso() {
	cat <<'FIN'

  Respaldo y restauracion de NextDocs.

    scripts/respaldo.sh crear <carpeta>
    scripts/respaldo.sh verificar <carpeta>
    scripts/respaldo.sh restaurar <carpeta>

  crear      Vuelca cada base con pg_dump --format=custom y empaqueta el
             volumen de MinIO. No interrumpe el servicio.

  verificar  Levanta un PostgreSQL descartable, restaura el volcado ahi y
             compara la cantidad de filas de cada tabla contra la base viva.
             Tambien restaura los objetos en un volumen aparte y compara.
             No toca nada de produccion.

  restaurar  Restaura sobre las bases y el volumen reales. DESTRUCTIVO:
             pide confirmacion escrita.

  Variables:
    NEXTDOCS_PG_CONTENEDOR   por defecto nextdocs-postgres
    NEXTDOCS_PG_USUARIO      por defecto nextdocs
    NEXTDOCS_PG_BASES        por defecto "nextdocs nextdocs_workflow"
    NEXTDOCS_MINIO_VOLUMEN   por defecto nextdocs-ai_minio-datos

FIN
}

crear() {
	local destino="$1"
	mkdir -p "$destino"
	local marca
	marca="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

	for base in $BASES; do
		echo "  volcando $base"
		docker exec "$CONTENEDOR_PG" pg_dump -U "$USUARIO_PG" -d "$base" --format=custom \
			> "$destino/$base.dump"
	done

	echo "  empaquetando objetos de $VOLUMEN_MINIO"
	docker run --rm -v "$VOLUMEN_MINIO":/datos:ro -v "$(cd "$destino" && pwd)":/respaldo \
		alpine tar czf /respaldo/minio.tar.gz -C /datos .

	{
		echo "fecha=$marca"
		echo "bases=$BASES"
		echo "volumen=$VOLUMEN_MINIO"
	} > "$destino/respaldo.info"

	echo
	ls -la "$destino"
	echo
	echo "  Un respaldo sin verificar no es un respaldo:"
	echo "    scripts/respaldo.sh verificar $destino"
	echo
}

verificar() {
	local origen
	origen="$(cd "$1" && pwd)"
	local contenedor="nextdocs-verificacion-respaldo"
	local volumen="nextdocs-verificacion-minio"
	local fallas=0

	docker rm -f "$contenedor" >/dev/null 2>&1 || true
	docker volume rm -f "$volumen" >/dev/null 2>&1 || true

	docker run -d --name "$contenedor" -e POSTGRES_USER="$USUARIO_PG" \
		-e POSTGRES_PASSWORD=verificacion -e POSTGRES_DB=postgres "$IMAGEN_PG" >/dev/null
	for _ in $(seq 1 30); do
		if docker exec "$contenedor" pg_isready -U "$USUARIO_PG" >/dev/null 2>&1; then break; fi
		sleep 2
	done

	for base in $BASES; do
		echo "  verificando $base"
		docker exec "$contenedor" psql -U "$USUARIO_PG" -d postgres -qc "CREATE DATABASE \"$base\"" >/dev/null
		docker cp "$origen/$base.dump" "$contenedor:/tmp/$base.dump" >/dev/null
		docker exec "$contenedor" pg_restore -U "$USUARIO_PG" -d "$base" \
			--no-owner --no-privileges "/tmp/$base.dump" >/dev/null 2>&1 || true

		local tablas comparadas difieren
		tablas="$(docker exec "$CONTENEDOR_PG" psql -U "$USUARIO_PG" -d "$base" -tAc \
			"SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")"
		comparadas=0
		difieren=0
		while read -r tabla; do
			[ -z "$tabla" ] && continue
			local vivo copia
			vivo="$(docker exec "$CONTENEDOR_PG" psql -U "$USUARIO_PG" -d "$base" -tAc \
				"SELECT count(*) FROM \"$tabla\"" 2>/dev/null || echo error)"
			copia="$(docker exec "$contenedor" psql -U "$USUARIO_PG" -d "$base" -tAc \
				"SELECT count(*) FROM \"$tabla\"" 2>/dev/null || echo ausente)"
			comparadas=$((comparadas + 1))
			if [ "$vivo" != "$copia" ]; then
				difieren=$((difieren + 1))
				echo "    DIFIERE $tabla: vivo=$vivo copia=$copia"
			fi
		done <<< "$tablas"
		echo "    $comparadas tablas, $difieren con diferencias"
		[ "$difieren" -ne 0 ] && fallas=$((fallas + 1))
	done

	echo "  verificando objetos"
	docker volume create "$volumen" >/dev/null
	docker run --rm -v "$volumen":/datos -v "$origen":/respaldo:ro \
		alpine tar xzf /respaldo/minio.tar.gz -C /datos

	local suma_viva suma_copia objetos
	suma_viva="$(docker run --rm -v "$VOLUMEN_MINIO":/d:ro alpine sh -c \
		'cd /d && find . -type f ! -path "./.minio.sys/*" | sort | xargs md5sum 2>/dev/null | md5sum' | cut -d' ' -f1)"
	suma_copia="$(docker run --rm -v "$volumen":/d:ro alpine sh -c \
		'cd /d && find . -type f ! -path "./.minio.sys/*" | sort | xargs md5sum 2>/dev/null | md5sum' | cut -d' ' -f1)"
	objetos="$(docker run --rm -v "$volumen":/d:ro alpine sh -c \
		'find /d -type f ! -path "*/.minio.sys/*" | wc -l')"

	if [ "$suma_viva" = "$suma_copia" ]; then
		echo "    $objetos objetos, identicos"
	else
		echo "    LOS OBJETOS DIFIEREN: vivo=$suma_viva copia=$suma_copia"
		fallas=$((fallas + 1))
	fi

	docker rm -f "$contenedor" >/dev/null 2>&1 || true
	docker volume rm -f "$volumen" >/dev/null 2>&1 || true

	echo
	if [ "$fallas" -eq 0 ]; then
		echo "  Respaldo verificado."
		echo
		return 0
	fi
	echo "  El respaldo NO paso la verificacion."
	echo
	return 1
}

restaurar() {
	local origen
	origen="$(cd "$1" && pwd)"
	echo
	echo "  Esto sobrescribe las bases [$BASES] y el volumen $VOLUMEN_MINIO."
	echo "  Escribi RESTAURAR para continuar."
	read -r respuesta
	[ "$respuesta" = "RESTAURAR" ] || { echo "  Cancelado."; exit 1; }

	for base in $BASES; do
		echo "  restaurando $base"
		docker exec "$CONTENEDOR_PG" psql -U "$USUARIO_PG" -d postgres -qc \
			"DROP DATABASE IF EXISTS \"$base\" WITH (FORCE)"
		docker exec "$CONTENEDOR_PG" psql -U "$USUARIO_PG" -d postgres -qc "CREATE DATABASE \"$base\""
		docker cp "$origen/$base.dump" "$CONTENEDOR_PG:/tmp/$base.dump" >/dev/null
		docker exec "$CONTENEDOR_PG" pg_restore -U "$USUARIO_PG" -d "$base" \
			--no-owner --no-privileges "/tmp/$base.dump"
	done

	echo "  restaurando objetos"
	docker run --rm -v "$VOLUMEN_MINIO":/datos -v "$origen":/respaldo:ro \
		alpine sh -c 'rm -rf /datos/* /datos/.minio.sys && tar xzf /respaldo/minio.tar.gz -C /datos'

	echo
	echo "  Restaurado. Reinicia los servicios: docker compose --profile app --profile workflow restart"
	echo
}

case "${1:-}" in
	crear)     [ $# -eq 2 ] || { uso; exit 1; }; crear "$2" ;;
	verificar) [ $# -eq 2 ] || { uso; exit 1; }; verificar "$2" ;;
	restaurar) [ $# -eq 2 ] || { uso; exit 1; }; restaurar "$2" ;;
	*)         uso; exit 1 ;;
esac
