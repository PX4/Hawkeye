# Data Sources

Hawkeye consumes telemetry from two sources: live MAVLink streams (from PX4 SITL) and ULog file replay.

## MAVLink messages (live mode)

| Message                | Purpose                                                |
| ---------------------- | ------------------------------------------------------ |
| `HEARTBEAT`            | Vehicle type, arming state, autopilot mode             |
| `HIL_STATE_QUATERNION` | Position, attitude, velocity, airspeed (PX4 SIH)       |
| `ATTITUDE_QUATERNION`  | Attitude (vehicles without HIL)                        |
| `GLOBAL_POSITION_INT`  | Position and velocity (vehicles without HIL)           |
| `STATUSTEXT`           | Warning and log messages (severity 0–7)                |
| `HOME_POSITION`        | Authoritative home location                            |

The vehicle pose comes from exactly one source.
A PX4 SIH sim streams `HIL_STATE_QUATERNION`, the simulator's ground truth, and Hawkeye uses only that.
Real vehicles and non-SIH SITL don't send it, so Hawkeye requests `ATTITUDE_QUATERNION` and `GLOBAL_POSITION_INT` at 50 Hz instead.
If a vehicle sends both, `HIL_STATE_QUATERNION` wins: the standard pose messages are ignored and, if Hawkeye had requested them, stopped again.

## ULog topics (replay mode)

| Topic                     | Required | Purpose                                       |
| ------------------------- | -------- | --------------------------------------------- |
| `vehicle_attitude`        | yes      | Orientation quaternion                        |
| `vehicle_local_position`  | yes      | NED position and velocity                     |
| `vehicle_global_position` | no       | GPS position (enables real-world coordinates) |
| `vehicle_status`          | no       | Vehicle type, arming, flight mode transitions |
| `home_position`           | no       | Authoritative home (Tier 1 source)            |
| `airspeed_validated`      | no       | Airspeed sensor data                          |
| `logging`                 | no       | STATUSTEXT warnings (feeds the HUD ticker)    |

If a required topic is missing, Hawkeye refuses to load the log and prints an error.
If optional topics are missing, the affected features silently degrade (no airspeed, no STATUSTEXT, etc.).

## Related

- [Position Data Tiers](./position-tiers.md) — How Hawkeye classifies logs based on which position sources are present
- [Live SITL](./sitl.md) — Connecting PX4 SITL to Hawkeye via MAVLink
- [ULog Replay](./replay.md) — Loading `.ulg` files for playback
