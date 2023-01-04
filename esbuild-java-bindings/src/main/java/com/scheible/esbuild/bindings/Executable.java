package com.scheible.esbuild.bindings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 *
 * @author sj
 */
class Executable {

	static final String ESBUILD_VERSION = "0.16.12";

	private enum OperatingSystem {
		WINDOWS("win32", ".exe"), LINUX("linux", "");

		private final String name;
		private final String extension;

		private OperatingSystem(String name, String extension) {
			this.name = name;
			this.extension = extension;
		}

		private static OperatingSystem identify() {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("windows")) {
				return OperatingSystem.WINDOWS;
			} else if (os.contains("linux")) {
				return OperatingSystem.LINUX;
			} else {
				throw new IllegalStateException("The operating system '" + os + "' is not supported!");
			}
		}
	};

	private enum ProcessorArch {
		X64("x64");

		private final String value;

		private ProcessorArch(String value) {
			this.value = value;
		}

		private static ProcessorArch identify() {
			String arch = System.getProperty("os.arch").toLowerCase();
			if (arch.contains("amd64")) {
				return ProcessorArch.X64;
			} else {
				throw new IllegalStateException("The processor architecture '" + arch + "' is not supported!");
			}
		}
	};

	private static String getExecutableName(OperatingSystem operatingSystem) {
		return "esbuild-" + operatingSystem.name + "-" + ProcessorArch.identify().value + "-"
				+ ESBUILD_VERSION + operatingSystem.extension;
	}

	static Path copyToTarget() throws IOException {
		OperatingSystem operatingSystem = OperatingSystem.identify();

		String executableName = getExecutableName(operatingSystem);
		Path file = Path.of("./target").resolve(executableName);
		Files.createDirectories(file.getParent());
		try (InputStream executableInput = Thread.currentThread().getContextClassLoader().getResourceAsStream(executableName)) {
			Files.copy(executableInput, file, StandardCopyOption.REPLACE_EXISTING);
		}

		if (operatingSystem == OperatingSystem.LINUX) {
			makeExecutable(file);
		}

		return file;
	}

	private static void makeExecutable(Path file) throws IOException {
		PosixFileAttributeView attributes = Files.getFileAttributeView(file, PosixFileAttributeView.class);
		Set<PosixFilePermission> permissions = attributes.readAttributes().permissions();
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		attributes.setPermissions(permissions);
	}
}
