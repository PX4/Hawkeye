/*
 * live_marker.h -- Pure parser for the live-session inbox marker.
 *
 * The Kotlin launcher writes "<millis> <port>" to inbox/.live. This parses that
 * buffer with zero file I/O or Raylib dependency, so it can be unit-tested with
 * string inputs (see tests/test_live_marker.c).
 */
#ifndef LIVE_MARKER_H
#define LIVE_MARKER_H

#include <stdint.h>
#include <stdlib.h>

/*
 * Parses a live marker buffer "<millis> [port]". Returns the millis token (0 on a
 * NULL/empty/non-numeric buffer, matching the .ready token contract so the
 * newest-token-wins comparison is unaffected). Writes a validated listen port
 * through out_port: the trailing port when present and within 1024..65535, else
 * default_port. A bare "<millis>" (legacy format) keeps the default.
 */
static inline long long parse_live_marker(const char *buf, uint16_t default_port,
                                          uint16_t *out_port) {
    *out_port = default_port;
    if (!buf) return 0;

    char *end = NULL;
    long long token = strtoll(buf, &end, 10);
    if (end == buf) return 0;

    char *port_end = NULL;
    long port = strtol(end, &port_end, 10);
    if (port_end != end && port >= 1024 && port <= 65535) {
        *out_port = (uint16_t)port;
    }
    return token;
}

#endif
