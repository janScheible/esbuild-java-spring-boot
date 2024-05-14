package com.scheible.esbuild.bindings;

import com.scheible.esbuild.bindings.EsBuildProtocol.Packet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sj
 */
public class EsBuild {

	static final String ESBUILD_VERSION = "0.19.7";
	
	private static final List<String> STANDARD_RUN_FLAGS = List.of("--color=false");

	private static final String[] STRING_ARRAY_TYPE = new String[]{};
	private static final List<String> STANDARD_TRANSFORM_FLAGS = List.of("--log-level=silent", "--log-limit=0");
	
	private static final Object INSTANCE_LOCK = new Object();
	private static EsBuild instance = null;
	private static int instanceShareCounter = 0;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final Process process;

	private final AtomicInteger requestId = new AtomicInteger(0);

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final Map<Integer, CountDownLatch> synchronizationMap = new ConcurrentHashMap<>();
	private final Map<Integer, TranspilationResult> resultMap = new ConcurrentHashMap<>();

	private final AtomicBoolean reading = new AtomicBoolean(true);
	private final Thread readThread;

	private EsBuild(String esBuildVersion, Process process) {
		this.process = process;

		this.readThread = new Thread(() -> {
			this.logger.info("esbuild {} is running...", esBuildVersion);
			boolean firstPacket = true;

			byte[] packetLengthBytes = new byte[4];
			ByteBuffer packetLengthBuffer = ByteBuffer.wrap(packetLengthBytes);
			packetLengthBuffer.order(ByteOrder.LITTLE_ENDIAN);

			while (this.reading.get()) {
				try {
					if (process.getInputStream().readNBytes(packetLengthBytes, 0, 4) != 4) {
						if (this.reading.get()) {
							this.logger.error("Error reading esbuild output.");
						}
						break;
					}
					int packetLength = packetLengthBuffer.getInt(0);

					ByteBuffer buffer = ByteBuffer.allocate(packetLength);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					if (process.getInputStream().readNBytes(buffer.array(), 0, packetLength) != packetLength) {
						if (this.reading.get()) {
							this.logger.error("Error reading esbuild output.");
						}
						break;
					}

					// first package is the returned version number --> just ignore
					if (!firstPacket) {
						Packet packet = EsBuildProtocol.decodePacket(buffer);

						if (packet.request() && "ping".equals(packet.value().get("command"))) {
							ByteBuffer sendBuffer = EsBuildProtocol.encodePacket(packet.id(), false, Map.of());
							process.getOutputStream().write(sendBuffer.array(), 0, sendBuffer.position());
							process.getOutputStream().flush();
						} else if (!packet.request()) {
							String code = (String) packet.value().get("code");
							Object[] errors = (Object[]) packet.value().get("errors");

							if (errors.length == 0) {
								this.resultMap.put(packet.id(), new TranspilationResult(Optional.of(code), Optional.empty()));
							} else {
								@SuppressWarnings("unchecked")
								Map<String, Object> firstError
										= (Map<String, Object>) errors[0];

								String text = (String) firstError.get("text");

								@SuppressWarnings("unchecked")
								Map<String, Object> location
										= (Map<String, Object>) firstError.get("location");
								int line = (int) location.get("line");
								int column = (int) location.get("column");
								String lineText = (String) location.get("lineText");

								this.resultMap.put(packet.id(), new TranspilationResult(Optional.empty(),
										Optional.of(new TranspilationError(line, column, text, lineText))));
							}

							this.synchronizationMap.get(packet.id()).countDown();
						} else {
							throw new IllegalStateException("Received unknown packet!");
						}
					}
				} catch (IOException ex) {
					this.logger.error("Error reading esbuild output.", ex);
				}

				firstPacket = false;
			}
		});
		this.readThread.setName("EsBuild Read Thread");
	}

	public static String run(Path workDir, String... args) throws IOException, InterruptedException {
		return run(null, workDir, args);
	}

	public static String run(String esBuildVersion, Path workDir, String... args) throws IOException, InterruptedException {
		if(args.length == 0) {
			throw new IllegalArgumentException("At least one argument must be passed!");
		}

		String finalEsBuildVersion = Optional.ofNullable(esBuildVersion).orElse(ESBUILD_VERSION);
		Path executable = Executable.copyToTarget(finalEsBuildVersion, workDir);

		List<String> command = new ArrayList<>();
		command.add(executable.toString());
		command.addAll(STANDARD_RUN_FLAGS);
		command.addAll(Arrays.asList(args));

		ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).directory(workDir.toFile());

		Process process = builder.start();
		
		StringBuilder output = new StringBuilder();
		String error = null;
		try (BufferedReader stdoutReader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = stdoutReader.readLine()) != null) {
				output.append(line).append("\n");
				
				if (error == null && line.contains("[ERROR]")) {
					error = line;
				}
			}
		}

		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IllegalStateException("EsBuild error '" + (error == null ? "unknown" : error)
					+ "' with exit code " + exitCode + "!");
		}

		return output.toString();
	}

	public static EsBuild start() throws IOException {
		return start(null, Path.of("."));
	}

	public static EsBuild start(Path workDir) throws IOException {
		return start(null, workDir);
	}

	public static EsBuild start(String esBuildVersion, Path workDir) throws IOException {
		synchronized (INSTANCE_LOCK) {
			if(instance != null) {
				instanceShareCounter++;
				return instance;
			} else {
				String finalEsBuildVersion = Optional.ofNullable(esBuildVersion).orElse(ESBUILD_VERSION);
				Path executable = Executable.copyToTarget(finalEsBuildVersion, workDir);

				ProcessBuilder builder = new ProcessBuilder(executable.toString(), "--service=" + finalEsBuildVersion, "--ping").directory(workDir.toFile());
				Process process = builder.redirectErrorStream(true).start();
				EsBuild esBuild = new EsBuild(finalEsBuildVersion, process);
				esBuild.readThread.start();
				
				instance = esBuild;				
				return esBuild;
			}
		}
	}

	public Future<TranspilationResult> transform(String fileName, String input, String... flags) throws IOException {
		return transform(fileName, input.getBytes(StandardCharsets.UTF_8), flags);
	}

	public Future<TranspilationResult> transform(String fileName, byte[] inputBytes, String... flags) throws IOException {
		List<String> allFlags = new ArrayList<>(STANDARD_TRANSFORM_FLAGS);
		allFlags.addAll(Arrays.asList(flags));
		allFlags.add("--loader=" + getLoaderFromExtension(fileName));
		allFlags.add("--sourcefile=./" + fileName);

		int nextRequestId = this.requestId.incrementAndGet();

		CountDownLatch synchronizationLatch = new CountDownLatch(1);
		this.synchronizationMap.put(nextRequestId, synchronizationLatch);

		ByteBuffer transformRequest = EsBuildProtocol.encodePacket(nextRequestId, true, //
				Map.of("command", "transform", //
						"flags", allFlags.toArray(STRING_ARRAY_TYPE), //
						"input", ByteBuffer.wrap(inputBytes), //
						"inputFS", false));

		// only a single thread at a time should do that to avoid corruption othe output stream
		synchronized (this) {
			this.process.getOutputStream().write(transformRequest.array(), 0, transformRequest.position());
			this.process.getOutputStream().flush();
		}

		FutureTask<TranspilationResult> resultWaitTask = new FutureTask<>(() -> {
			synchronizationLatch.await();
			EsBuild.this.synchronizationMap.remove(nextRequestId);

			TranspilationResult result = EsBuild.this.resultMap.get(nextRequestId);
			EsBuild.this.resultMap.remove(nextRequestId);

			return result;
		});
		this.executor.submit(resultWaitTask);
		return resultWaitTask;
	}

	private static String getLoaderFromExtension(String fileName) {
		return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
	}
	
	public boolean isRunning() {
		return this.reading.get();
	}

	public void stop() {
		synchronized (INSTANCE_LOCK) {
			if(instanceShareCounter > 0) {
				instanceShareCounter--;
			} else {
				instance = null;
				
				this.reading.set(false);
				this.readThread.interrupt();
				this.process.destroy();
			}
		}
	}
}
