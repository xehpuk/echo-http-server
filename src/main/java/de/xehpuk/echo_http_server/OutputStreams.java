package de.xehpuk.echo_http_server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Broadcasts output to an arbitrary number of streams.
 * @author xehpuk
 * @since 1.1.0
 */
public class OutputStreams extends OutputStream {
	/**
	 * Does something with a stream that may throw an {@link IOException}.
	 */
	@FunctionalInterface
	private static interface IORunnable {
		void run(OutputStream stream) throws IOException;
	}
	
	/**
	 * The underlying streams which are broadcast to.
	 */
	private final List<OutputStream> streams;
	/**
	 * Close all underlying streams when this stream is closed?
	 */
	private final boolean autoClose;
	/**
	 * Abort an operation immediately if it failed for one stream?
	 */
	private final boolean failFast;

	/**
	 * @param streams {@link #streams}
	 * @param autoClose {@link #autoClose}
	 * @param failFast {@link #failFast}
	 */
	public OutputStreams(final List<OutputStream> streams, final boolean autoClose, final boolean failFast) {
		if (failFast) {
			this.streams = streams.stream()
				.map(Objects::requireNonNull)
				.collect(Collectors.toList());
		} else {
			this.streams = streams.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}
		this.autoClose = autoClose;
		this.failFast = failFast;
	}

	public OutputStreams(final List<OutputStream> streams, final boolean autoClose) {
		this(streams, autoClose, false);
	}

	public OutputStreams(final List<OutputStream> streams) {
		this(streams, false);
	}

	public boolean isAutoClose() {
		return autoClose;
	}

	public boolean isFailFast() {
		return failFast;
	}
	
	/**
	 * Execute the runnable on all underlying streams with some exception handling boilerplate.
	 * @param runnable the potentially throwing operation
	 * @throws IOException if the operation failed for at least one stream
	 */
	private void run(final IORunnable runnable) throws IOException {
		if (failFast) {
			for (final OutputStream stream : streams) {
				runnable.run(stream);
			}
		} else {
			IOException exception = null;
			for (final OutputStream stream : streams) {
				try {
					runnable.run(stream);
				} catch (final IOException ioe) {
					if (exception == null) {
						exception = ioe;
					} else {
						exception.addSuppressed(ioe);
					}
				}
			}
			if (exception != null) {
				throw exception;
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (autoClose) {
			run(OutputStream::close);
		}
	}

	@Override
	public void flush() throws IOException {
		run(OutputStream::flush);
	}

	@Override
	public void write(final int b) throws IOException {
		run(stream -> stream.write(b));
	}

	@Override
	public void write(final byte[] b) throws IOException {
		run(stream -> stream.write(b));
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		run(stream -> stream.write(b, off, len));
	}
}
