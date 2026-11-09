package com.veterinaria.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ContentLanguageFilterTest {

    private final ContentLanguageFilter filtro = new ContentLanguageFilter();

    @Test
    void agregaContentLanguageEsCoALaRespuesta() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/api/catalogo"), respuesta, new MockFilterChain());

        assertThat(respuesta.getHeader("Content-Language")).isEqualTo("es-CO");
    }

    @Test
    void continuaConLaCadenaDeFiltros() throws Exception {
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(new MockHttpServletRequest("GET", "/api/citas"), new MockHttpServletResponse(), cadena);

        assertThat(cadena.getRequest()).isNotNull();
    }
}
