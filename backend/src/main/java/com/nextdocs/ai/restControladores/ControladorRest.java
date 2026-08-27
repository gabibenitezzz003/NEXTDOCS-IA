package com.nextdocs.ai.restControladores;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ProhibidoException;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.ContextoSeguridad;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class ControladorRest<T> {

	public static final int TAMANO_PAGINA_MAXIMO = 200;

	public final Log log = LogFactory.getLog(getClass());

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	public PrincipalNextDocs principal() {
		return ContextoSeguridad.principal();
	}

	public String tenantId() {
		return principal().getTenantId();
	}

	public Tenant tenant() {
		String id = tenantId();
		return tenantRepository.findById(id).orElseThrow(() -> EntidadNoEncontradaException.de("Tenant", id));
	}

	public Usuario usuario() {
		PrincipalNextDocs principal = principal();
		if (principal.getTipoActor() != TipoActor.USUARIO) {
			return null;
		}
		return usuarioRepository.buscarPorIdYTenant(principal.getIdActor(), principal.getTenantId())
				.orElseThrow(() -> EntidadNoEncontradaException.de("Usuario", principal.getIdActor()));
	}

	public Usuario usuarioObligatorio() {
		Usuario usuario = usuario();
		if (usuario == null) {
			throw new ProhibidoException("Esta accion requiere un usuario humano autenticado");
		}
		return usuario;
	}

	public Pageable paginado(int pagina, int tamano, String orden) {
		int tamanoEfectivo = Math.min(Math.max(tamano, 1), TAMANO_PAGINA_MAXIMO);
		return PageRequest.of(Math.max(pagina, 0), tamanoEfectivo, ordenar(orden));
	}

	public Sort ordenar(String orden) {
		if (orden == null || orden.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "alta");
		}
		String[] partes = orden.split(",");
		Sort.Direction direccion = partes.length > 1 ? Sort.Direction.fromString(partes[1]) : Sort.Direction.ASC;
		return Sort.by(direccion, partes[0]);
	}

	public <E extends Enum<E>> List<String> listar(E[] valores) {
		List<String> nombres = new ArrayList<>();
		for (E valor : valores) {
			nombres.add(valor.name());
		}
		return nombres;
	}
}
