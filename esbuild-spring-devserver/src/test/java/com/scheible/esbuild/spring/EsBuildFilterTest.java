package com.scheible.esbuild.spring;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author sj
 */
class EsBuildFilterTest {

	@Test
	void testIsFrontendRequest() {
		assertThat(EsBuildFilter.isFrontendRequest(
				UriComponentsBuilder.fromHttpUrl("http://localhost/").build(), "")).isFalse();
		assertThat(EsBuildFilter.isFrontendRequest(
				UriComponentsBuilder.fromHttpUrl("http://localhost/script.js").build(), "")).isFalse();
		assertThat(EsBuildFilter.isFrontendRequest(
				UriComponentsBuilder.fromHttpUrl("http://localhost/context/frontend/script.js").build(), "")).isFalse();

		assertThat(EsBuildFilter.isFrontendRequest(
				UriComponentsBuilder.fromHttpUrl("http://localhost/frontend/script.js").build(), "")).isTrue();
		assertThat(EsBuildFilter.isFrontendRequest(
				UriComponentsBuilder.fromHttpUrl("http://localhost/context/frontend/script.js").build(), "/context")).isTrue();
	}

	@Test
	void testGetPathWithoutFrontendPrefix() {
		assertThat(EsBuildFilter.getPathWithoutFrontendPrefix(
				UriComponentsBuilder.fromHttpUrl("http://localhost/frontend/script.js").build(), "")).isEqualTo("/script.js");
		assertThat(EsBuildFilter.getPathWithoutFrontendPrefix(
				UriComponentsBuilder.fromHttpUrl("http://localhost/context/frontend/script.js").build(), "/context")).isEqualTo("/script.js");
	}

	@Test
	void testGetExtension() {
		assertThat(EsBuildFilter.getExtension(
				UriComponentsBuilder.fromHttpUrl("http://localhost/script.js").build())).isEqualTo("js");

		assertThat(EsBuildFilter.getExtension(
				UriComponentsBuilder.fromHttpUrl("http://localhost/script.CsS").build())).isEqualTo("css");		
		
		assertThat(EsBuildFilter.getExtension(
				UriComponentsBuilder.fromHttpUrl("http://localhost/script").build())).isEqualTo("");
	}
}
