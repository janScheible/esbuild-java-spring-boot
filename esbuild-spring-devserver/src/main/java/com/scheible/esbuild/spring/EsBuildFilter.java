package com.scheible.esbuild.spring;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author sj
 */
public class EsBuildFilter implements Filter {

	private static final String JAVA_SCRIPT_MIME_TYPE = MediaTypeFactory.getMediaType("script.js").get().toString();

	private final ResourceLoader resourceLoader;
	private final ServletContext servletContext;
	private final EsBuildService esBuildService;

	public EsBuildFilter(ResourceLoader resourceLoader, ServletContext servletContext, EsBuildService esBuildService) {
		this.resourceLoader = resourceLoader;
		this.servletContext = servletContext;
		this.esBuildService = esBuildService;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest request && servletResponse instanceof HttpServletResponse response) {
			UriComponents uriComponents = UriComponentsBuilder.fromPath(request.getRequestURI()).build();

			if (isFrontendRequest(uriComponents, this.servletContext.getContextPath())) {
				if (isJavaScriptUrl(uriComponents)) {
					Resource typeScriptFile = findTypeScriptFile(uriComponents);

					if (typeScriptFile.exists()) {
						response.setContentType(JAVA_SCRIPT_MIME_TYPE);
						response.setCharacterEncoding(StandardCharsets.UTF_8.name());

						try (PrintWriter writer = response.getWriter()) {
							writer.println(this.esBuildService.transform(typeScriptFile.getFilename(),
									typeScriptFile.getInputStream().readAllBytes()));
						}
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				} else {
					String filePath = getPathWithoutFrontendPrefix(uriComponents, this.servletContext.getContextPath());
					RequestDispatcher dispatcher = servletRequest.getServletContext().getRequestDispatcher(filePath);
					// also do not cache all other frontend resources by warpping the response with NoStoreResponseWrapper
					dispatcher.forward(request, new NoStoreResponseWrapper(response));
				}

				return;
			}
		}

		chain.doFilter(servletRequest, servletResponse);
	}

	static boolean isFrontendRequest(UriComponents uriComponents, String contextPath) {
		if ("".equals(contextPath) && !uriComponents.getPathSegments().isEmpty()
				&& uriComponents.getPathSegments().get(0).toLowerCase().equals("frontend")) {
			return true;
		} else if (!"".equals(contextPath) && uriComponents.getPathSegments().size() > 1
				&& uriComponents.getPathSegments().get(1).toLowerCase().equals("frontend")) {
			return true;
		}

		return false;
	}
	
	static boolean isJavaScriptUrl(UriComponents uriComponents) {
		return uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1).toLowerCase().endsWith(".js");
	}

	static String getPathWithoutFrontendPrefix(UriComponents uriComponents, String contextPath) {
		List<String> pathSegments = new ArrayList<>(uriComponents.getPathSegments());
		if (!"".equals(contextPath)) {
			pathSegments.remove(0); // remove context path if any
		}
		pathSegments.remove(0); // now remove the first segment 'frontend'
		return pathSegments.stream().collect(Collectors.joining("/", "/", ""));
	}

	private Resource findTypeScriptFile(UriComponents uriComponents) {
		String filePath = getPathWithoutFrontendPrefix(uriComponents, this.servletContext.getContextPath());
		String baseFilePath = filePath.substring(0, filePath.length() - 3);

		Resource file = this.resourceLoader.getResource("file:./src/main/frontend/src" + baseFilePath + ".ts");
		if (!file.exists()) {
			file = this.resourceLoader.getResource("file:./src/main/frontend/src" + baseFilePath + ".tsx");
		}

		return file;
	}

	private static class NoStoreResponseWrapper extends HttpServletResponseWrapper {

		private NoStoreResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void setHeader(String name, String value) {
			if ("cache-control".equals(name.toLowerCase())) {
				super.setHeader(name, "no-store");
			} else {
				super.setHeader(name, value);
			}
		}
	}
}
