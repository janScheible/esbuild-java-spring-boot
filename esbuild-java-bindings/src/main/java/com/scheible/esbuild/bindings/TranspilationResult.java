package com.scheible.esbuild.bindings;

import java.util.Optional;
import java.util.function.Function;

/**
 *
 * @author sj
 */
public record TranspilationResult(Optional<String> code, Optional<TranspilationError> error) {

	public String codeOrElse(Function<TranspilationError, String> errorFormatter) {
		return code.orElseGet(() -> errorFormatter.apply(error.get()));
	}
}
