package de.xehpuk.echo_http_server;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import java.net.InetSocketAddress;

import java.time.LocalDateTime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

@Command(name = "echo-http-server", version = "EchoHttpServer 1.2.0")
public class EchoHttpServer implements Runnable {
	@Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
	private boolean versionInfoRequested;

	@Option(names = {"-?", "--help"}, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = {"-h", "--host"}, description = "the host to listen on (default \"${DEFAULT-VALUE}\")")
	private String host = "localhost";

	@Option(names = {"-p", "--port"}, description = "the port to listen on (default ${DEFAULT-VALUE})")
	private int port = 8080;

	@Option(names = {"-w", "--wait"}, description = "wait for the request to finish before sending the response\n(some clients may choke otherwise)")
	private boolean wait;
	
	@Option(names = {"-P", "--prefix"}, description = "the prefix to use for the echoed headers (default \"${DEFAULT-VALUE}\")")
	private String headerPrefix = "X-Echo-";
	
	@Option(names = {"-s", "--suffix"}, description = "the suffix to use for the echoed headers")
	private String headerSuffix = "";

	@Option(names = {"-b", "--backlog"}, description = "the maximum number of queued incoming connections to allow (default ${DEFAULT-VALUE})")
	private int backlog = 1;

	@Option(names = {"-v", "--verbose"}, description = "log incoming requests completely")
	private boolean verbose;

	@Option(names = {"-H", "--headers"}, description = "log incoming requests' headers")
	private boolean verboseHeaders;

	@Option(names = {"-B", "--body"}, description = "log incoming requests' body")
	private boolean verboseBody;

	private static class Header {
		static final String ALLOW = "Allow";
		static final String CONTENT_LENGTH = "Content-Length";
		static final String CONTENT_TYPE = "Content-Type";
	}

	private static class Status {
		static final int OK = 200;
		static final int METHOD_NOT_ALLOWED = 405;
	}

	private static class ResponseLength {
		static final int NONE = -1;
		static final int CHUNKED = 0;
	}

	private static class Method {
		static final String DELETE = "DELETE";
		static final String GET = "GET";
		static final String HEAD = "HEAD";
		static final String OPTIONS = "OPTIONS";
		static final String PATCH = "PATCH";
		static final String POST = "POST";
		static final String PUT = "PUT";
	}

	private static final String ALLOWED_METHODS = String.join(",",
		Method.DELETE,
		Method.GET,
		Method.HEAD,
		Method.OPTIONS,
		Method.PATCH,
		Method.POST,
		Method.PUT
	);
	
	private static final String PICOCLI_USAGE_WIDTH = "picocli.usage.width";
	
	static { 
		if (System.getProperty(PICOCLI_USAGE_WIDTH) == null) {
			System.setProperty(PICOCLI_USAGE_WIDTH, "120");
		}
	}

	public static void main(final String... args) throws IOException {
		final int exitCode = new CommandLine(new EchoHttpServer()).execute(args);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}
	
	private boolean printHeaders() {
		return verbose || verboseHeaders;
	}
	
	private boolean printBody() {
		return verbose || verboseBody;
	}

	private Stream<Entry<String, List<String>>> streamHeaders(final Map<String, List<String>> map) {
		final var stream = map.entrySet().stream();
		return printHeaders()
			? stream.peek(header -> header.getValue().forEach(value -> System.out.printf("%s: %s%n", header.getKey(), value)))
			: stream;
	}

	private void beforeRequest(final HttpExchange he) {
		System.out.printf("[%1$tF %1$tT] %2$s %3$s %4$s%n",
			LocalDateTime.now(),
			he.getRequestMethod(),
			he.getRequestURI(),
			he.getProtocol());
	}

	private void afterRequest(final HttpExchange he) {
		System.out.println();
	}

	private void afterResponse(final HttpExchange he) {
		System.out.println();
		System.out.println();
	}

	@Override
	public void run() {
		final HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(host, port), backlog);
		} catch (final IOException ioe) {
			throw new UncheckedIOException(ioe);
		}

		server.createContext("/", he -> {
			try (he) {
				final Headers reqHeaders = he.getRequestHeaders();
				final Headers resHeaders = he.getResponseHeaders();
				beforeRequest(he);
				streamHeaders(reqHeaders)
					.map(entry -> Map.entry(headerPrefix + entry.getKey() + headerSuffix, entry.getValue()))
					.forEach(entry -> resHeaders.put(entry.getKey(), entry.getValue()));
				afterRequest(he);
				if (reqHeaders.containsKey(Header.CONTENT_TYPE)) {
					resHeaders.set(Header.CONTENT_TYPE, reqHeaders.getFirst(Header.CONTENT_TYPE));
				}

				switch (he.getRequestMethod()) {
					case Method.PATCH, Method.POST, Method.PUT -> {
						try (final InputStream is = he.getRequestBody()) {
							final byte[] input = wait
								? is.readAllBytes()
								: null;
							he.sendResponseHeaders(Status.OK, reqHeaders.containsKey(Header.CONTENT_LENGTH)
								? Long.parseLong(reqHeaders.getFirst(Header.CONTENT_LENGTH))
								: ResponseLength.CHUNKED);
							try (final OutputStream os = printBody()
									? new OutputStreams(Arrays.asList(he.getResponseBody(), System.out))
									: he.getResponseBody()) {
								if (wait) {
									os.write(input);
								} else {
									is.transferTo(os);
								}
							}
						}

						afterResponse(he);
					}
					case Method.DELETE, Method.HEAD, Method.GET ->
						he.sendResponseHeaders(Status.OK, ResponseLength.NONE);
					case Method.OPTIONS -> {
						resHeaders.set(Header.ALLOW, ALLOWED_METHODS);
						he.sendResponseHeaders(Status.OK, ResponseLength.NONE);
					}
					default -> {
						resHeaders.set(Header.ALLOW, ALLOWED_METHODS);
						he.sendResponseHeaders(Status.METHOD_NOT_ALLOWED, ResponseLength.NONE);
					}
				}
			}
		});

		server.start();

		System.out.println("EchoHttpServer running at " + server.getAddress());
	}
}
