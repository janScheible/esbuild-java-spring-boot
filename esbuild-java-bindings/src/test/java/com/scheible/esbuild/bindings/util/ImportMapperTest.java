package com.scheible.esbuild.bindings.util;

import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sj
 */
class ImportMapperTest {

	@Test
	void testAbsolutFilePath() {
		assertThatThrownBy(() -> ImportMapper.map(Path.of("/c:/test"), "${PREFIX}"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testToBareImport() {
		assertThat(ImportMapper.toBareImport(List.of("test", "script"))).isEqualTo("~/test/script");
	}

	@Test
	void testToJavaScriptUrl() {
		assertThat(ImportMapper.toJavaScriptUrl(List.of("test", "script"), "${PREFIX}"))
				.isEqualTo("${PREFIX}/test/script.js?rev=${APP_REVISION}");

		assertThat(ImportMapper.toJavaScriptUrl(List.of("test", "script with whitespace"), "${PREFIX}"))
				.isEqualTo("${PREFIX}/test/script%20with%20whitespace.js?rev=${APP_REVISION}");
	}

	@Test
	void testToParts() {
		assertThat(ImportMapper.toParts(Path.of("test", "script.js"))).isEqualTo(List.of("test", "script"));
		assertThat(ImportMapper.toParts(Path.of("test", "script.ts"))).isEqualTo(List.of("test", "script"));
	}
}
