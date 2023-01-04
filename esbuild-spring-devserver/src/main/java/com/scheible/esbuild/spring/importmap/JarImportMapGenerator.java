package com.scheible.esbuild.spring.importmap;

import com.scheible.esbuild.bindings.util.ImportMapGenerator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 *
 * @author sj
 */
public class JarImportMapGenerator implements ImportMapGenerator {
	private String cachedImportMap = null;

	@Override
	public String generate(Map<String, String> params) {
		if (this.cachedImportMap == null) {
			try {
				Resource importMapFile = new ClassPathResource("/import-map.json");
				String importMapJson = new String(importMapFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				this.cachedImportMap = PlaceHolderReplacer.replace(importMapJson, params);;
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		return this.cachedImportMap;
	}
}
