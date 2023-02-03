package com.scheible.esbuild.maven;

import com.scheible.esbuild.bindings.EsBuild;
import com.scheible.esbuild.bindings.TranspilationResult;
import com.scheible.esbuild.bindings.util.ImportMapGenerator;
import com.scheible.esbuild.bindings.util.ImportMapper;
import com.scheible.esbuild.bindings.util.TsConfig;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author sj
 */
@Mojo(name = "prepare-frontend", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class PrepareFrontendMojo extends AbstractMojo {

	private static final PathMatcher TS_FILE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/*.{ts,tsx}");

	@Parameter(defaultValue = "${project.basedir}") // where the POM.xml is
	private File baseDirFile;

	@Parameter(defaultValue = "${project.build.outputDirectory}") // ./taget/classes
	private File outputDirFile;

	@Parameter(property = "esbuild-maven.skip", defaultValue = "${esbuild-maven.skip}")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			getLog().info("Skipping to prepare the frontend.");
			return;
		}

		try {
			Path frontendDir = this.baseDirFile.toPath().resolve("src").resolve("main").resolve("frontend");
			Path outputDir = this.outputDirFile.toPath();
			Path srcDir = frontendDir.resolve("src");
			Path libDir = frontendDir.resolve("lib");
			Path targetDir = outputDir.resolve("static").resolve("frontend");

			getLog().info("Frontend dir: '" + frontendDir + "'");
			getLog().info("  Src dir: '" + srcDir + "'");
			getLog().info("  Lib dir: '" + libDir + "'");
			getLog().info("Output dir: '" + outputDir + "'");
			getLog().info("  Target dir: '" + targetDir + "'");

			Path tsConfigFile = frontendDir.resolve("tsconfig.json");
			String tsConfigJson = TsConfig.readAsSingleLine(tsConfigFile);
			getLog().info("Read '" + frontendDir.relativize(tsConfigFile) + "' from frontend dir.");

			Collection<Path> srcFiles = getSrcFiles(srcDir);

			Collection<Path> tsSrcFiles = srcFiles.stream().filter(TS_FILE_MATCHER::matches).toList();
			Collection<Path> nonTsSrcFiles = new ArrayList<>(srcFiles);
			nonTsSrcFiles.removeAll(tsSrcFiles);

			Collection<Path> tsLibFiles = Files.exists(libDir) 
					? getSrcFiles(libDir).stream().filter(TS_FILE_MATCHER::matches).toList() 
					: Collections.emptyList();

			Path importMapFile = outputDir.resolve("import-map.json");

			getLog().info("Transforming files (from src dir to target dir):");
			transformTsFiles(srcDir, tsSrcFiles, targetDir, tsConfigJson, getLog());
			getLog().info("Copying files (from src dir to target dir):");
			copyNonTsFiles(srcDir, nonTsSrcFiles, targetDir, getLog());
			writeImportMap(srcDir, tsSrcFiles, libDir, tsLibFiles, outputDir, importMapFile, getLog());
		} catch (IOException ex) {
			throw new MojoExecutionException("Error while preparing the frontend.", ex);
		}
	}

	static Collection<Path> getSrcFiles(Path srcDir) throws IOException {
		try (Stream<Path> stream = Files.walk(srcDir)) {
			return stream.filter(Files::isRegularFile).toList();
		}
	}

	static void transformTsFiles(Path srcDir, Collection<Path> tsSrcFiles, Path targetDir, String tsConfigJson, Log log) throws IOException, MojoExecutionException {
		EsBuild esBuild = EsBuild.start();

		for (Path tsSrcFile : tsSrcFiles) {
			String originalFileName = tsSrcFile.getFileName().toString();
			String fileName = originalFileName.replace(".tsx", ".js").replace(".ts", ".js");

			byte[] typeScriptBytes = Files.readAllBytes(tsSrcFile);
			TranspilationResult result;
			try {
				result = esBuild.transform(originalFileName, typeScriptBytes, "--platform=browser", "--sourcemap=inline",
						"--tsconfig-raw=" + tsConfigJson).get();
			} catch (InterruptedException | ExecutionException ex) {
				throw new IllegalStateException(ex);
			}

			if (result.error().isPresent()) {
				esBuild.stop();
				String errorMessage = "Error transforming '" + srcDir.relativize(tsSrcFile) + "' :" + result.error().get().message();
				log.error(errorMessage);
				throw new MojoExecutionException(errorMessage);
			} else {
				Path tsTargetFile = targetDir.resolve(srcDir.relativize(tsSrcFile)).getParent().resolve(fileName);
				Files.createDirectories(tsTargetFile.getParent());
				Files.write(tsTargetFile, result.code().get().getBytes());
				log.info("- transformed '" + srcDir.relativize(tsSrcFile) + "' --> '" + targetDir.relativize(tsTargetFile) + "'");
			}
		}

		esBuild.stop();
	}

	static void copyNonTsFiles(Path srcDir, Collection<Path> nonTsSrcFiles, Path targetDir, Log log) throws IOException {
		for (Path nonTsSrcFile : nonTsSrcFiles) {
			Path nonTsTargetFile = targetDir.resolve(srcDir.relativize(nonTsSrcFile));
			Files.createDirectories(nonTsTargetFile.getParent());
			Files.copy(nonTsSrcFile, nonTsTargetFile, StandardCopyOption.REPLACE_EXISTING);
			log.info("- copied '" + srcDir.relativize(nonTsSrcFile) + "' --> '" + targetDir.relativize(nonTsTargetFile) + "'");
		}
	}

	static void writeImportMap(Path srcDir, Collection<Path> tsSrcFiles, Path libDir, Collection<Path> tsLibFiles,
			Path targetDir, Path importMapFile, Log log) throws IOException {
		Map<String, String> imports = new HashMap<>(ImportMapper.map(tsSrcFiles.stream().map(file -> srcDir.relativize(file)).toList(),
				ImportMapper.FRONTEND_PREFIX_PLACEHOLDER));
		if (Files.exists(libDir)) {
			imports.putAll(ImportMapper.map(tsLibFiles.stream().map(file -> libDir.relativize(file)).toList(),
					ImportMapper.LIBRARY_PREFIX_PLACEHOLDER));
		}

		String importMapJson = JsonbBuilder.create(new JsonbConfig().withFormatting(true)).toJson(ImportMapGenerator.toImportMap(imports));
		Files.writeString(importMapFile, importMapJson);
		log.info("Wrote '" + targetDir.relativize(importMapFile) + "' to output dir.");
	}
}
