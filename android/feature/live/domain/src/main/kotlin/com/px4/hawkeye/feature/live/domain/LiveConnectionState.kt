package com.px4.hawkeye.feature.live.domain

/**
 * Live MAVLink connection state, derived from the renderer's receiver:
 * - [WAITING]: listening, no telemetry yet (or never connected).
 * - [CONNECTED]: receiving telemetry from a system.
 * - [LOST]: was connected, then telemetry stopped (heartbeat timeout).
 */
enum class LiveConnectionState { WAITING, CONNECTED, LOST }
