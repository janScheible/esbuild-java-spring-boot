package com.scheible.esbuild.springboot.starter;

import com.scheible.esbuild.spring.AppRevision;
import com.scheible.esbuild.spring.EsBuildFilter;
import com.scheible.esbuild.spring.EsBuildService;
import com.scheible.esbuild.springboot.starter.EsbuildDevserverAutoConfiguration.DevToolsEnvironment;
import com.scheible.esbuild.springboot.starter.EsbuildDevserverAutoConfiguration.JarEnvironment;
import com.scheible.esbuild.bindings.util.ImportMapGenerator;
import com.scheible.esbuild.spring.importmap.DevToolsImportMapGenerator;
import com.scheible.esbuild.spring.importmap.JarImportMapGenerator;
import jakarta.servlet.ServletContext;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

/**
 *
 * @author sj
 */
@AutoConfiguration
@Import({DevToolsEnvironment.class, JarEnvironment.class})
public class EsbuildDevserverAutoConfiguration {

	@Bean
	AppRevision appRevision(Optional<GitProperties> gitProperties) {
		return new AppRevision(gitProperties.map(GitProperties::getShortCommitId).orElse("development"));
	}

	@ConditionalOnClass(name = "org.springframework.boot.devtools.settings.DevToolsSettings")
	static class DevToolsEnvironment {

		@Bean
		ImportMapGenerator importMapGenerator(ResourceLoader resourceLoader) {
			return new DevToolsImportMapGenerator(resourceLoader);
		}

		@Bean
		EsBuildService buildService(ResourceLoader resourceLoader) {
			return new EsBuildService(resourceLoader);
		}

		@Bean
		EsBuildFilter esBuildFilter(ResourceLoader resourceLoader, ServletContext servletContext, EsBuildService esBuildService) {
			return new EsBuildFilter(resourceLoader, servletContext, esBuildService);
		}
	}

	@ConditionalOnMissingClass("org.springframework.boot.devtools.settings.DevToolsSettings")
	static class JarEnvironment {

		@Bean
		ImportMapGenerator importMapGenerator() {
			return new JarImportMapGenerator();
		}
	}
}
