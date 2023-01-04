package com.scheible.esbuild.bindings.util;

import java.util.Map;

/**
 *
 * @author sj
 */
public interface ImportMapGenerator {

	String generate(Map<String, String> params);
	
	/**
	 * Returns a map of map structure that can be serilaized to JSON (scopes are not supported).
	 */
	public static Map<String, Map<String, String>> toImportMap(Map<String, String> imports) {
		return Map.of("imports", imports);
	}	
}
