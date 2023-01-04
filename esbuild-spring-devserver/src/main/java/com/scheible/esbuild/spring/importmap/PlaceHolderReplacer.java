package com.scheible.esbuild.spring.importmap;

import com.scheible.esbuild.bindings.util.ImportMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.web.util.UriUtils;

/**
 *
 * @author sj
 */
class PlaceHolderReplacer {

	static String replace(String importMapJson, Map<String, String> params) {
		for (Map.Entry<String, String> param : params.entrySet()) {
			importMapJson = importMapJson.replace(param.getKey(),
					ImportMapper.APP_REVISION_PLACEHOLDER.equals(param.getValue())
					? UriUtils.encode(param.getValue(), StandardCharsets.UTF_8) : param.getValue());
		}
		
		return importMapJson;
	}
}
