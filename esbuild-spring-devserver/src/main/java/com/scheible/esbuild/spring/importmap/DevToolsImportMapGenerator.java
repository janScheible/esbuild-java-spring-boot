package com.scheible.esbuild.spring.importmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scheible.esbuild.bindings.util.ImportMapGenerator;
import com.scheible.esbuild.bindings.util.ImportMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

/**
 *
 * @author sj
 */
@Component
public class DevToolsImportMapGenerator implements ImportMapGenerator {

	private final ObjectMapper objectMapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	private final ResourcePatternResolver resourceResolver;

	public DevToolsImportMapGenerator(ResourceLoader resourceLoader) {
		this.resourceResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}

	@Override
	public String generate(Map<String, String> params) {
		try {
			Map<String, String> imports = new HashMap<>(getImports("file:./src/main/frontend/src",
					ImportMapper.FRONTEND_PREFIX_PLACEHOLDER));
			imports.putAll(getImports("file:./src/main/frontend/lib", ImportMapper.LIBRARY_PREFIX_PLACEHOLDER));

			Map<String, Map<String, String>> importMap = ImportMapGenerator.toImportMap(imports);

			String importMapJson = this.objectMapper.writeValueAsString(importMap);
			return PlaceHolderReplacer.replace(importMapJson, params);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private Map<String, String> getImports(String rootLocation, String prefixPlaceHolderName) throws IOException {
		Resource rootResource = this.resourceResolver.getResource(rootLocation);
		if (rootResource.exists()) {
			Path frontendDir = rootResource.getFile().toPath().toAbsolutePath();
			List<Resource> scriptFiles = Stream.concat(Stream.of(
					this.resourceResolver.getResources(rootLocation + "/**/*.ts")), Stream.of(
					this.resourceResolver.getResources(rootLocation + "/**/*.tsx"))).toList();

			List<Path> relativeScriptFiles = new ArrayList<>();
			for (Resource scriptFile : scriptFiles) {
				relativeScriptFiles.add(frontendDir.relativize(scriptFile.getFile().toPath()));
			}

			return ImportMapper.map(relativeScriptFiles, prefixPlaceHolderName);
		} else {
			return Collections.emptyMap();
		}
	}
}
