package com.scheible.esbuildspringboot;

import com.scheible.esbuild.spring.AppRevision;
import com.scheible.esbuild.bindings.util.ImportMapGenerator;
import com.scheible.esbuild.bindings.util.ImportMapper;
import jakarta.servlet.ServletContext;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author sj
 */
@Controller
public class IndexController {

	private final AppRevision appRevision;
	private final ImportMapGenerator importMapGenerator;
	private final ServletContext servletContext;

	public IndexController(AppRevision appRevision, ImportMapGenerator importMapGenerator, ServletContext servletContext) {
		this.appRevision = appRevision;
		this.importMapGenerator = importMapGenerator;
		this.servletContext = servletContext;
	}

	@GetMapping("/")
	public ModelAndView getPage(Model model) {
		model.addAttribute("appRevision", this.appRevision.value());
		model.addAttribute("importMap", this.importMapGenerator.generate(Map.of( //
				ImportMapper.FRONTEND_PREFIX_PLACEHOLDER, this.servletContext.getContextPath() + "/frontend", //
				ImportMapper.LIBRARY_PREFIX_PLACEHOLDER, this.servletContext.getContextPath() + "/webjars", //
				ImportMapper.APP_REVISION_PLACEHOLDER, this.appRevision.value())));

		return new ModelAndView("index", model.asMap());
	}
}
