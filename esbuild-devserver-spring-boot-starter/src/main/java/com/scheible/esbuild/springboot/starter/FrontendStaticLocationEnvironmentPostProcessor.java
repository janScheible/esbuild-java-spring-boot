package com.scheible.esbuild.springboot.starter;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

/**
 *
 * @author sj
 */
class FrontendStaticLocationEnvironmentPostProcessor implements EnvironmentPostProcessor{

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if(ClassUtils.isPresent("org.springframework.boot.devtools.settings.DevToolsSettings", 
				FrontendStaticLocationEnvironmentPostProcessor.class.getClassLoader())) {
			String staticLocations = environment.getProperty("spring.web.resources.static-locations", 
					String.join(",", new WebProperties.Resources().getStaticLocations()));
			
			Map<String, Object> staticLocation = Map.of("spring.web.resources.static-locations", 
					staticLocations + ",file:./src/main/frontend/src/");
			
			environment.getPropertySources().addFirst(
					new MapPropertySource("esbuild-frontend-static-location", staticLocation));
		}
	}	
}
