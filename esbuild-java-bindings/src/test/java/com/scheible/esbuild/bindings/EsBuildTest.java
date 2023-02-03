package com.scheible.esbuild.bindings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sj
 */
class EsBuildTest {
	
	@Test
	void testTransformSuccess() throws IOException, InterruptedException, ExecutionException {
		EsBuild esBuild = EsBuild.start();
		TranspilationResult result = esBuild.transform("test.ts", "function doIt(text: string) {}").get();
		esBuild.stop();
		
		assertThat(result.code()).isPresent();
		assertThat(result.error()).isEmpty();
	}
	
	@Test
	void testMultipleTransformSuccess() throws IOException, InterruptedException, ExecutionException {
		EsBuild esBuild = EsBuild.start();
		Future<TranspilationResult> firstResultFuture = esBuild.transform("test.ts", "function first(text: string) {}");
		Future<TranspilationResult> secondResultFuture = esBuild.transform("test.ts", "function second(text: string) {}");
		
		TranspilationResult secondResult = secondResultFuture.get();
		TranspilationResult firstResult = firstResultFuture.get();
		
		esBuild.stop();
		
		assertThat(firstResult.code().get()).contains("first");
		assertThat(secondResult.code().get()).contains("second");
	}
	
	@Test
	void testTransformError() throws IOException, InterruptedException, ExecutionException {
		EsBuild esBuild = EsBuild.start();
		TranspilationResult result = esBuild.transform("test.ts", "function doIt(text@: string) {}").get();
		esBuild.stop();
		
		assertThat(result.code()).isEmpty();
		assertThat(result.error()).isPresent();
	}
	
	@Test
	void testMultipleStartStop() throws IOException, InterruptedException, ExecutionException {
		EsBuild firstEsBuild = EsBuild.start();
		
		EsBuild secondEsBuild = EsBuild.start();
		secondEsBuild.stop();
		
		assertThat(firstEsBuild.isRunning()).isTrue();
		firstEsBuild.stop();
		assertThat(firstEsBuild.isRunning()).isFalse();
	}
	
	@Test
	void testSuccessfulRun() throws IOException, InterruptedException {
		assertThat(EsBuild.run(Path.of("."), "--help")).contains("esbuild");
	}
	
	@Test
	void testErrorRun() throws IOException, InterruptedException {
		assertThatThrownBy(() -> EsBuild.run(Path.of("."), "--bundle", "--metafile=meta.json", "test.ts"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("output");
	}
}
