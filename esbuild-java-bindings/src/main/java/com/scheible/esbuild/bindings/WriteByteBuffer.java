package com.scheible.esbuild.bindings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Wraps a ByteBuffer. Allows to grow an is always little endian.
 */
class WriteByteBuffer {

	private ByteBuffer delegate;

	WriteByteBuffer(int capaity) {
		this.delegate = ByteBuffer.allocate(capaity).order(ByteOrder.LITTLE_ENDIAN);
	}

	ByteBuffer ensureRemaining(int delta) {
		if (this.delegate.remaining() < delta) {
			ByteBuffer enlargedBuffer = ByteBuffer.allocate(Math.max(delegate.capacity() * 2, delegate.capacity() + delta));
			enlargedBuffer.order(ByteOrder.LITTLE_ENDIAN);
			int position = this.delegate.position();
			this.delegate.rewind();
			enlargedBuffer.put(this.delegate);
			enlargedBuffer.position(position);
			this.delegate = enlargedBuffer;
		}

		return this.delegate;
	}

	WriteByteBuffer put(byte value) {
		this.delegate.put(value);
		return this;
	}

	WriteByteBuffer put(byte[] src) {
		this.delegate.put(src);
		return this;
	}

	WriteByteBuffer putInt(int value) {
		this.delegate.putInt(value);
		return this;
	}

	WriteByteBuffer putInt(int index, int value) {
		this.delegate.putInt(index, value);
		return this;
	}

	byte[] array() {
		return this.delegate.array();
	}

	int position() {
		return this.delegate.position();
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}
}
