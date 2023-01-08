package com.scheible.esbuild.spring.importmap;

import com.scheible.esbuild.bindings.util.ImportMapper;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sj
 */
public class PlaceHolderReplacerTest {

	@Test
	public void testAppRevisionOnlyEncoding() {
		Assertions.assertThat(PlaceHolderReplacer.replace("${key}," + ImportMapper.APP_REVISION_PLACEHOLDER, //
				Map.of("${key}", "devel opment", //
						ImportMapper.APP_REVISION_PLACEHOLDER, "devel opment")))
				.isEqualTo("devel opment,devel%20opment");
	}
}
