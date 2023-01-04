package com.scheible.esbuild.bindings;

import com.scheible.esbuild.bindings.EsBuildProtocol.Packet;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sj
 */
class EsBuildProtocolTest {

	@Test
	void testProtocolRoundTrip() {
		ByteBuffer encoded = EsBuildProtocol.encodePacket(42, true, Map.of( //
				"boolean", false, //
				"integer", 23, //
				"byteBuffer", ByteBuffer.wrap(new byte[]{1, 2, 3, 4}), //
				"array", new String[]{"abc", null}, //
				"map", Map.of("nested", "value")));

		encoded = encoded.slice(0, encoded.position()).order(ByteOrder.LITTLE_ENDIAN);
		encoded.getInt(); // skip length
		Packet packet = EsBuildProtocol.decodePacket(encoded);

		assertThat(packet.id()).isEqualTo(42);
		assertThat(packet.request()).isEqualTo(true);
		assertThat(packet.value().get("boolean")).isEqualTo(false);
		assertThat(packet.value().get("integer")).isEqualTo(23);
		assertThat(packet.value().get("byteBuffer")).isEqualTo(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));
		assertThat(packet.value().get("array")).isEqualTo(new String[]{"abc", null});
		assertThat(packet.value().get("map")).isEqualTo(Map.of("nested", "value"));
	}

	@Test
	void testDecodeTransformCommand() {
		ByteBuffer commandBuffer = convertUnsignedBytes(new short[]{241, 0, 0, 0, 2, 0, 0, 0, 6, 4, 0, 0, 0, 7, 0,
			0, 0, 99, 111, 109, 109, 97, 110, 100, 3, 9, 0, 0, 0, 116, 114, 97, 110, 115, 102, 111, 114, 109, 5, 0, 0,
			0, 102, 108, 97, 103, 115, 5, 7, 0, 0, 0, 3, 12, 0, 0, 0, 45, 45, 99, 111, 108, 111, 114, 61, 116, 114, 117,
			101, 3, 18, 0, 0, 0, 45, 45, 108, 111, 103, 45, 108, 101, 118, 101, 108, 61, 115, 105, 108, 101, 110, 116,
			3, 13, 0, 0, 0, 45, 45, 108, 111, 103, 45, 108, 105, 109, 105, 116, 61, 48, 3, 15, 0, 0, 0, 45, 45, 116, 97,
			114, 103, 101, 116, 61, 101, 115, 110, 101, 120, 116, 3, 12, 0, 0, 0, 45, 45, 102, 111, 114, 109, 97, 116,
			61, 101, 115, 109, 3, 18, 0, 0, 0, 45, 45, 112, 108, 97, 116, 102, 111, 114, 109, 61, 98, 114, 111, 119,
			115, 101, 114, 3, 11, 0, 0, 0, 45, 45, 108, 111, 97, 100, 101, 114, 61, 116, 115, 7, 0, 0, 0, 105, 110, 112,
			117, 116, 70, 83, 1, 0, 5, 0, 0, 0, 105, 110, 112, 117, 116, 4, 32, 0, 0, 0, 102, 117, 110, 99, 116, 105,
			111, 110, 32, 115, 101, 99, 111, 110, 100, 40, 116, 101, 120, 116, 58, 32, 115, 116, 114, 105, 110, 103,
			41, 32, 123, 125});

		commandBuffer.getInt(); // skip length
		Packet packet = EsBuildProtocol.decodePacket(commandBuffer);

		assertThat(packet.id()).isEqualTo(1);
		assertThat(packet.request()).isEqualTo(true);
		assertThat(packet.value().get("command")).isEqualTo("transform");
	}

	@Test
	void testDecodeTransformResponse() {
		ByteBuffer responseBuffer = convertUnsignedBytes(new short[]{3, 0, 0, 0, 6, 6, 0, 0, 0, 4, 0, 0, 0, 99, 111,
			100, 101, 3, 26, 0, 0, 0, 102, 117, 110, 99, 116, 105, 111, 110, 32, 115, 101, 99, 111, 110, 100, 40, 116,
			101, 120, 116, 41, 32, 123, 10, 125, 10, 6, 0, 0, 0, 99, 111, 100, 101, 70, 83, 1, 0, 6, 0, 0, 0, 101, 114,
			114, 111, 114, 115, 5, 0, 0, 0, 0, 3, 0, 0, 0, 109, 97, 112, 3, 0, 0, 0, 0, 5, 0, 0, 0, 109, 97, 112, 70,
			83, 1, 0, 8, 0, 0, 0, 119, 97, 114, 110, 105, 110, 103, 115, 5, 0, 0, 0, 0});

		Packet packet = EsBuildProtocol.decodePacket(responseBuffer);

		assertThat(packet.id()).isEqualTo(1);
		assertThat(packet.request()).isEqualTo(false);
		assertThat(packet.value().containsKey("code")).isTrue();
	}

	private static ByteBuffer convertUnsignedBytes(short[] unsignedBytes) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(unsignedBytes.length).order(ByteOrder.LITTLE_ENDIAN);

		for (int i = 0; i < unsignedBytes.length; i++) {
			byteBuffer.put((byte) unsignedBytes[i]);
		}

		return byteBuffer.rewind();
	}
}
