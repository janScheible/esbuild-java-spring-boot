package com.scheible.esbuild.bindings.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author sj
 */
public class TsConfig {

	public static String readAsSingleLine(Path tsConfigFile) throws IOException {
		return readAsSingleLine(Files.newInputStream(tsConfigFile));
	}

	public static String readAsSingleLine(InputStream tsConfigFile) {
		try {
			return new String(tsConfigFile.readAllBytes(), StandardCharsets.UTF_8)
					.replaceAll("\\R", " ").replaceAll("\\t", " ").replaceAll("[ ]+", " ").trim();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
