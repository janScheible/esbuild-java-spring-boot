package com.scheible.esbuild.bindings.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 *
 * @author sj
 */
public class ImportMapper {

	public static final String FRONTEND_PREFIX_PLACEHOLDER = "${FRONTEND_PREFIX}";
	public static final String LIBRARY_PREFIX_PLACEHOLDER = "${LIBRARY_PREFIX}";
	public static final String APP_REVISION_PLACEHOLDER = "${APP_REVISION}";

	/**
	 * Maps realtive paths for a import map. 
	 */
	public static Map<String, String> map(Collection<Path> scriptFiles, String prefixPlaceHolderName) {
		return scriptFiles.stream().map(scriptFile -> map(scriptFile, prefixPlaceHolderName))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	static Entry<String, String> map(Path scriptFile, String prefixPlaceHolderName) {
		if(scriptFile.isAbsolute()) {
			throw new IllegalArgumentException("The path '" + scriptFile + "' must be relative!");
		}

		List<String> parts = toParts(scriptFile);
		return Map.entry(toBareImport(parts), toJavaScriptUrl(parts, prefixPlaceHolderName));
	}

	/**
	 * Converts the file parts to a bare import without a file extension.
	 */
	static String toBareImport(List<String> parts) {
		return parts.stream().collect(Collectors.joining("/", "~/", ""));
	}

	/**
	 * Converts the file parts to a JavaScript URL. Either with prefix and revision placholders or the real value.
	 */
	static String toJavaScriptUrl(List<String> parts, String prefixPlaceHolderName) {
		try {
			URI uri = new URI(null, null, parts.stream().collect(Collectors.joining("/", "/", ".js")), "rev=", null);
			return prefixPlaceHolderName + uri.getRawPath()	+ "?" + uri.getRawQuery() + APP_REVISION_PLACEHOLDER;
		} catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Splits a path into parts (directories and file) and removed the file extension (if any).
	 */
	static List<String> toParts(Path path) {
		List<String> pathParts = new ArrayList<>();

		for (int i = 0; i < path.getNameCount(); i++) {
			String subPath = path.subpath(i, i + 1).toString();
			if (i != path.getNameCount() - 1) {
				pathParts.add(subPath);
			} else {
				boolean isTypeDeclarationFile = subPath.toLowerCase().endsWith(".d.ts");
				if (isTypeDeclarationFile) {
					pathParts.add(subPath.substring(0, subPath.length() - 5));
				} else {
					pathParts.add(subPath.contains(".") ? subPath.substring(0, subPath.lastIndexOf('.')) : subPath);
				}
			}
		}

		return pathParts;
	}
}
