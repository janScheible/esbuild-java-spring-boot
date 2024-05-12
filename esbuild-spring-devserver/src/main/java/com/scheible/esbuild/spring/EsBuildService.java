package com.scheible.esbuild.spring;

import com.scheible.esbuild.bindings.EsBuild;
import com.scheible.esbuild.bindings.TranspilationError;
import com.scheible.esbuild.bindings.TranspilationResult;
import com.scheible.esbuild.bindings.util.TsConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Optional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 *
 * @author sj
 */
public class EsBuildService implements ServletContextListener {

	private static final String JAVA_SCRIPT_ERROR_DIALOG = """
        export default undefined;

		addEventListener('DOMContentLoaded', event => {
			if (!('errorOccured' in document.body.dataset)) {
				document.body.dataset.errorOccured = 'true';

				document.body.insertAdjacentHTML(
					'afterbegin',
					`<style>dialog::backdrop { background-color: rgba(160, 160, 160, 0.9); }</style>
					<dialog id="error-dialog" style="background-color: white; display: inline-block; padding: 0px 4px 0px 4px; font-family: Arial,Helvetica Neue,Helvetica,sans-serif;">
					<h1 style="color: darkred;">Error</h1>
					<p>${new URL(import.meta.url).pathname}</p>
					<div style="display: flex; gap: 4px; border: 1px solid darkgray;">
						<div id="linenumber" style="background-color: lightgray;">
							<pre style="font-weight: bold; padding: 0px 8px 0px 8px;">${line}</pre>
							<div>&nbsp;</div>
						</div>
						<div id="source">
							<pre style="padding-right: 4px;">${codeLine}</pre>
							<pre>${'-'.repeat(${column})}^</pre>
						</div>
					</div>
					<p>${message}</p>
				</dialog>`);

                document.getElementById('error-dialog').showModal();
			}
		});""";

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final ResourceLoader resourceLoader;
	private final Optional<String> esBuildVersion;

	private EsBuild esBuild;
	private String tsConfigJson;

	public EsBuildService(ResourceLoader resourceLoader, Optional<String> esBuildVersion) {
		this.resourceLoader = resourceLoader;
		this.esBuildVersion = esBuildVersion;
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			Resource tsConfigResource = resourceLoader.getResource("file:./src/main/frontend/tsconfig.json");
			this.tsConfigJson = TsConfig.readAsSingleLine(tsConfigResource.getInputStream());

			this.esBuild = this.esBuildVersion.isEmpty() ? EsBuild.start() : EsBuild.start(this.esBuildVersion.get());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public String transform(String fileName, byte[] inputBytes) throws IOException {
		try {
			TranspilationResult result = this.esBuild.transform(fileName, inputBytes, "--platform=browser", "--sourcemap=inline", 
					"--tsconfig-raw=" + this.tsConfigJson).get();
			if(result.error().isPresent()) {
				this.logger.error(result.error().get().message() + " at " + result.error().get().line() + ":"
						+ result.error().get().column() + " in '" + fileName + "'");
			}
			return result.codeOrElse(EsBuildService::renderErrorJavaScript);
		} catch (InterruptedException | ExecutionException ex) {
			throw new IOException(ex);
		}
	}
	
	private static String renderErrorJavaScript(TranspilationError error) {
		return JAVA_SCRIPT_ERROR_DIALOG.replace("${line}", Integer.toString(error.line()))
				.replace("${column}", Integer.toString(error.column())).replace("${message}", error.message())
				.replace("${codeLine}", error.codeLine());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		this.esBuild.stop();
	}
}
