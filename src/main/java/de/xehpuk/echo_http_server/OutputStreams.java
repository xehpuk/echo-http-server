package de.xehpuk.echo_http_server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OutputStreams extends OutputStream {
	@FunctionalInterface
	private static interface IORunnable {
		void run(OutputStream stream) throws IOException;
	}
	
	private final List<OutputStream> streams;
	private final boolean autoClose;
	private final boolean failFast;

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
