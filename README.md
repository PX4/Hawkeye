# Hawkeye

[![Release](https://img.shields.io/github/v/release/PX4/Hawkeye)](https://github.com/PX4/Hawkeye/releases/latest)

Real-time 3D flight visualizer for PX4 — watch live SITL simulations, replay ULog flights, and analyze multi-drone swarms with correlation tracking, takeoff alignment, and deconfliction. Supports up to 16 vehicles simultaneously with ghost overlays, formation views, and frame-by-frame inspection.

Built with [Raylib](https://www.raylib.com/) and [MAVLink](https://mavlink.io/). Zero dependencies to install — just build and fly.

![Hawkeye](https://artifacts.px4.io/hawkeye/cover.png)

**Full documentation: [px4.github.io/Hawkeye](https://px4.github.io/Hawkeye/)**

## Install

### macOS (Homebrew)

```bash
brew tap PX4/px4
brew install PX4/px4/hawkeye
```

### Linux (Debian/Ubuntu)

Download the `.deb` from the [latest release](https://github.com/PX4/Hawkeye/releases/latest):

```bash
sudo dpkg -i hawkeye_*.deb
```

### Windows

Download `hawkeye-<version>-windows-x64.zip` from the [latest release](https://github.com/PX4/Hawkeye/releases/latest), extract it anywhere, and run `hawkeye.exe`.

On first launch, Windows SmartScreen may warn that the binary is from an unknown publisher — click "More info" → "Run anyway". The ZIP is unsigned; we're working on winget distribution to remove this warning.

### Android

Download `hawkeye-<version>-android.apk` from the [latest release](https://github.com/PX4/Hawkeye/releases/latest) and install it. It requires Android 10 or newer and bundles both `arm64-v8a` and `x86_64`.

Releases also go to the Google Play internal test track; ask a maintainer to add you to the tester list for automatic updates.

### Source builds

See [Building from source](https://px4.github.io/Hawkeye/developer/build) in the developer docs.

### Official builds

Official Hawkeye builds come from the [GitHub releases](https://github.com/PX4/Hawkeye/releases) on this repository, the `PX4/px4` Homebrew tap, and Google Play under the Dronecode Foundation account. Android releases are signed with keys held by the Dronecode Foundation.

Builds from anywhere else are unofficial and unsupported by this project. Hawkeye is a visualization tool, not a flight-safety device, and is provided without warranty of any kind.

Anyone may fork and republish Hawkeye under the terms of the license; see [FORKS.md](FORKS.md) for what to rename first.

## Quickstart

Launch with PX4 SITL (single vehicle):

```bash
# Terminal 1: Start PX4 SITL with SIH
make px4_sitl sihsim_quadx

# Terminal 2: Launch viewer
hawkeye
```

Replay a ULog file:

```bash
hawkeye --replay path/to/flight.ulg
```

See the [full documentation](https://px4.github.io/Hawkeye/) for CLI options, multi-vehicle swarms, ULog replay, HUD modes, camera views, correlation analysis, and keyboard shortcuts.

## Acknowledgments

This project was inspired by [jMAVSim](https://github.com/PX4/jMAVSim), the Java-based MAVLink simulator and viewer. Vehicle 3D models are derived from jMAVSim's assets.

## License

[BSD-3-Clause](LICENSE). Third-party components and their licenses are listed in [NOTICE.md](NOTICE.md).

The license covers the code. It does not cover the Hawkeye, PX4, or Dronecode names and logos, so forks published as apps need their own name and icon; see [FORKS.md](FORKS.md).
