/*
 * test_live_marker.c -- Unit tests for parse_live_marker (live_marker.h).
 * Pure string parsing, no file I/O or Raylib.
 */
#include "live_marker.h"

#include <assert.h>
#include <stdio.h>

#define DEFAULT_PORT 19410

int main(void) {
    uint16_t port;
    long long token;

    // "<millis> <port>": token and port both parsed.
    token = parse_live_marker("1780000000123 14550", DEFAULT_PORT, &port);
    assert(token == 1780000000123LL);
    assert(port == 14550);

    // Bare "<millis>" (legacy): token parsed, port falls back to default.
    token = parse_live_marker("1780000000123", DEFAULT_PORT, &port);
    assert(token == 1780000000123LL);
    assert(port == DEFAULT_PORT);

    // Port below the valid range falls back to default (token still parsed).
    token = parse_live_marker("42 80", DEFAULT_PORT, &port);
    assert(token == 42);
    assert(port == DEFAULT_PORT);

    // Port above 65535 falls back to default.
    token = parse_live_marker("42 70000", DEFAULT_PORT, &port);
    assert(token == 42);
    assert(port == DEFAULT_PORT);

    // Port boundaries: 1024 and 65535 are accepted.
    parse_live_marker("1 1024", DEFAULT_PORT, &port);
    assert(port == 1024);
    parse_live_marker("1 65535", DEFAULT_PORT, &port);
    assert(port == 65535);

    // Empty / non-numeric buffer: token 0, default port.
    token = parse_live_marker("", DEFAULT_PORT, &port);
    assert(token == 0);
    assert(port == DEFAULT_PORT);
    token = parse_live_marker("garbage", DEFAULT_PORT, &port);
    assert(token == 0);
    assert(port == DEFAULT_PORT);

    // NULL buffer: token 0, default port (no crash).
    token = parse_live_marker(NULL, DEFAULT_PORT, &port);
    assert(token == 0);
    assert(port == DEFAULT_PORT);

    printf("test_live_marker: all assertions passed\n");
    return 0;
}
