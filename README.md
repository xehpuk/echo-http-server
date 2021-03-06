# EchoHttpServer

This HTTP server does the following:

1. Echoes the request headers, potentially prefixed and suffixed.
2. Responds to the request, depending on the method:
   - `PATCH`, `POST`, `PUT`: Pipes the request body to the response body.
   - `DELETE`, `HEAD`, `GET`: Empty 200 OK
   - `OPTIONS`: Behaves like a normal server by returning the allowed headers.
   - else: 405 Method Not Allowed

```
Usage: echo-http-server [-?BHvVw] [-b=<backlog>] [-h=<host>] [-p=<port>] [-P=<headerPrefix>] [-s=<headerSuffix>]
  -?, --help                display this help message
  -b, --backlog=<backlog>   the maximum number of queued incoming connections to allow (default 1)
  -B, --body                log incoming requests' body
  -h, --host=<host>         the host to listen on (default "localhost")
  -H, --headers             log incoming requests' headers
  -p, --port=<port>         the port to listen on (default 8080)
  -P, --prefix=<headerPrefix>
                            the prefix to use for the echoed headers (default "X-Echo-")
  -s, --suffix=<headerSuffix>
                            the suffix to use for the echoed headers
  -v, --verbose             log incoming requests completely
  -V, --version             display version info
  -w, --wait                wait for the request to finish before sending the response
                            (some clients may choke otherwise)
```