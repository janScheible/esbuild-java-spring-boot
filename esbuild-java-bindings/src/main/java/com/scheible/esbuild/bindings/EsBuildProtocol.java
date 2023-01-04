package com.scheible.esbuild.bindings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sj
 */
class EsBuildProtocol {

	record Packet(int id, boolean request, Map<String, Object> value) {

	}

	private static final byte NULL_TYPE = 0;
	private static final byte BOOLEAN_TYPE = 1;
	private static final byte INTEGER_TYPE = 2;
	private static final byte STRING_TYPE = 3;
	private static final byte BYTE_BUFFER_TYPE = 4;
	private static final byte ARRAY_TYPE = 5;
	private static final byte MAP_TYPE = 6;

	/**
	 * Encode packet including leading length.
	 */
	static ByteBuffer encodePacket(int id, boolean request, Map<String, Object> value) {
		WriteByteBuffer buffer = new WriteByteBuffer(1024);
		buffer.putInt(0); // length not yet known
		buffer.putInt(id << 1 | (request ? 0 : 1));

		EsBuildProtocol.write(buffer, value);

		buffer.putInt(0, buffer.position() - 4); // now we know the length

		return ByteBuffer.wrap(buffer.array(), 0, buffer.position()).order(ByteOrder.LITTLE_ENDIAN)
				.position(buffer.position());
	}

	private static void write(WriteByteBuffer buffer, Object value) {
		if (value == null) {
			buffer.ensureRemaining(1).put(NULL_TYPE);
		} else if (value instanceof Boolean) {
			buffer.ensureRemaining(1 + 1).put(BOOLEAN_TYPE).put((byte) (value == Boolean.TRUE ? 1 : 0));
		} else if (value instanceof Integer integer) {
			buffer.ensureRemaining(1 + 4).put(INTEGER_TYPE).putInt(integer);
		} else if (value instanceof String string) {
			writeString(buffer, string, true);
		} else if (value instanceof ByteBuffer byteBuffer) {
			buffer.ensureRemaining(1 + 4 + byteBuffer.capacity())
					.put(BYTE_BUFFER_TYPE).putInt(byteBuffer.capacity()).put(byteBuffer);
		} else if (value.getClass().isArray()) {
			writeArray(buffer, (Object[]) value);
		} else if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) value;
			writeMap(buffer, map);
		} else {
			throw new IllegalArgumentException("Unkonwn type " + value.getClass().getSimpleName() + "!");
		}
	}

	private static void writeString(WriteByteBuffer buffer, String string, boolean writeType) {
		byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
		buffer.ensureRemaining((writeType ? 1 : 0) + 4 + stringBytes.length);
		if (writeType) {
			buffer.put(STRING_TYPE);
		}
		buffer.putInt(stringBytes.length).put(stringBytes);
	}

	private static void writeArray(WriteByteBuffer buffer, Object[] array) {
		buffer.ensureRemaining(1 + 4).put(ARRAY_TYPE).putInt(array.length);
		for (Object item : array) {
			write(buffer, item);
		}
	}

	private static void writeMap(WriteByteBuffer buffer, Map<String, Object> map) {
		buffer.ensureRemaining(1 + 4).put(MAP_TYPE).putInt(map.size());

		for (var entry : map.entrySet()) {
			writeString(buffer, entry.getKey(), false);
			write(buffer, entry.getValue());
		}
	}

	/**
	 * Decode packet without leading length.
	 */
	static Packet decodePacket(ByteBuffer buffer) {
		int id = buffer.getInt();
		boolean request = (id & 1) == 0;
		id >>>= 1;

		if (EsBuildProtocol.read(buffer) instanceof Map map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> valueMap = map;
			return new Packet(id, request, valueMap);
		} else {
			throw new IllegalStateException("Only packets of type map are supported!");
		}
	}

	private static Object read(ByteBuffer buffer) {
		short type = Byte.valueOf(buffer.get()).shortValue();
		return switch (type) {
			case NULL_TYPE ->
				null;
			case BOOLEAN_TYPE ->
				buffer.get() != 0;
			case INTEGER_TYPE ->
				buffer.getInt();
			case STRING_TYPE ->
				readString(buffer);
			case BYTE_BUFFER_TYPE ->
				readByteBuffer(buffer);
			case ARRAY_TYPE ->
				readArray(buffer);
			case MAP_TYPE ->
				readMap(buffer);
			default ->
				throw new IllegalArgumentException("Unknown type " + type + "!");
		};
	}

	static String readString(ByteBuffer buffer) {
		int textLength = buffer.getInt();
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < textLength; i++) {
			stringBuilder.append((char) buffer.get());
		}
		return stringBuilder.toString();
	}

	static Object[] readArray(ByteBuffer buffer) {
		int arrayLength = buffer.getInt();
		Object[] array = new Object[arrayLength];
		for (int i = 0; i < arrayLength; i++) {
			array[i] = read(buffer);
		}
		return array;
	}

	static Map<String, Object> readMap(ByteBuffer buffer) {
		int entryCount = buffer.getInt();
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < entryCount; i++) {
			String key = readString(buffer);
			Object value = read(buffer);
			map.put(key, value);
		}
		return map;
	}

	static ByteBuffer readByteBuffer(ByteBuffer buffer) {
		int bufferLength = buffer.getInt();
		byte[] bufferBytes = new byte[bufferLength];
		buffer.get(bufferBytes);
		return ByteBuffer.wrap(bufferBytes);
	}
}
