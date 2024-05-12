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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
	@ConditionalOnMissingBean
	AppRevision appRevision(Optional<GitProperties> gitProperties) {
		return new AppRevision(gitProperties.map(GitProperties::getShortCommitId).orElse("development"));
	}

	@ConditionalOnClass(name = "org.springframework.boot.devtools.settings.DevToolsSettings")
	static class DevToolsEnvironment {

		@Bean
		@ConditionalOnMissingBean
		ImportMapGenerator importMapGenerator(ResourceLoader resourceLoader) {
			return new DevToolsImportMapGenerator(resourceLoader);
		}

		@Bean
		@ConditionalOnMissingBean
		EsBuildService buildService(ResourceLoader resourceLoader,
				@Value("${esbuild-spring-devserver.esbuild-version}") Optional<String> esBuildVersion) {
			return new EsBuildService(resourceLoader, esBuildVersion);
		}

		@Bean
		@ConditionalOnMissingBean
		EsBuildFilter esBuildFilter(ResourceLoader resourceLoader, ServletContext servletContext, EsBuildService esBuildService) {
			return new EsBuildFilter(resourceLoader, servletContext, esBuildService);
		}
	}

	@ConditionalOnMissingClass("org.springframework.boot.devtools.settings.DevToolsSettings")
	static class JarEnvironment {

		@Bean
		@ConditionalOnMissingBean
		ImportMapGenerator importMapGenerator() {
			return new JarImportMapGenerator();
		}
	}
}
