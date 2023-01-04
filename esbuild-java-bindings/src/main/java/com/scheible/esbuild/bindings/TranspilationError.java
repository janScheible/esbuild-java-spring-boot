package com.scheible.esbuild.bindings;

/**
 *
 * @author sj
 */
public record TranspilationError(int line, int column, String message, String codeLine) {

}
