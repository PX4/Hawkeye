# Forks, branding, and official builds

Hawkeye is open source under [BSD-3-Clause](LICENSE). You may fork it, modify it, and redistribute it, for commercial purposes or otherwise. Nothing on this page takes any of that away.

What this page covers is the one thing an open source license does not grant: the right to use the project's name and identity. That distinction is standard across open source, and it exists so that people who download something called "Hawkeye" get the build this project actually produced.

## Official builds

A build is official only if it comes from one of these:

- **GitHub Releases** on [PX4/Hawkeye](https://github.com/PX4/Hawkeye/releases) — the source tarball, macOS bottle, `.deb` packages, Windows ZIP, and Android APK.
- **Google Play**, published under the Dronecode Foundation account. Currently the internal test track only.
- **Homebrew**, via the [PX4/homebrew-px4](https://github.com/PX4/homebrew-px4) tap.

Official Android builds are signed with keys held by the Dronecode Foundation, which is what makes this verifiable rather than a claim. A build from anywhere else carries a different signature and will not install over an official one without uninstalling first. Release builds produced by a fork are unsigned by design: the signing step only runs when the project's keystore secrets are present, so a fork's CI cannot produce something that looks signed by us.

Builds obtained anywhere else are unofficial. They may be perfectly good, but this project did not produce them, cannot vouch for them, and does not support them. Please report bugs in an unofficial build to whoever published it.

## If you publish a fork

You are welcome to. Please give it your own identity first:

- **Use a different app name.** Not "Hawkeye", and not something confusingly similar to it.
- **Use a different icon.** Do not reuse Hawkeye's icon or a close imitation.
- **Change the Android `applicationId`.** It is currently `com.px4.hawkeye.android`. Move it out of `com.px4.*` entirely, both because it is our application identity and because `PX4` is not our name to sublicense (see below). Two apps sharing an `applicationId` also cannot coexist on one device.
- **Do not use PX4 or Dronecode names, logos, or brand colors** as the identity of your build. Hawkeye ships the Dronecode brand palette because Hawkeye is a Dronecode project; a fork is not.
- **State that you are not affiliated with us**, in your README and your store listing. Suggested wording:

  > NAME is an independent application based on Hawkeye. It is not affiliated with, authorized by, sponsored by, or otherwise approved by the Hawkeye project, PX4, or the Dronecode Foundation.

Keep the copyright and license notices intact. The BSD-3-Clause license requires that, and it is what lets you say the next part.

## What is always fine

Describing accurately where your work came from is normal, expected, and encouraged:

- "based on Hawkeye", "a fork of Hawkeye", "compatible with Hawkeye"
- Linking back to this repository
- Keeping our copyright notices in the source, as the license requires
- Private builds, internal builds, and research builds you do not publish under a confusable identity

The line is between describing a relationship and claiming an identity. The first is fine; the second is what we are asking you to avoid.

## Why we ask

- **[BSD-3-Clause](LICENSE), clause 3** already says the copyright holder's and contributors' names may not be used "to endorse or promote products derived from this software" without permission. Renaming your fork is how you comply with the license you are using.
- **PX4 and Dronecode are trademarks of the Dronecode Foundation**, governed by the [Dronecode trademark policy](https://dronecode.org/trademarks/). A copyright license carries no implied trademark license, so the BSD grant on this code does not extend to those names.
- **[F-Droid's inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/)** requires forked apps to change their application ID, name, and icon before they can be listed. Following this page keeps your fork eligible.
- **[Google Play's impersonation policy](https://support.google.com/googleplay/android-developer/answer/9888374)** prohibits apps that mislead users about their relationship to another app or developer, including through confusingly similar names and icons. Rebranding keeps your listing safe.

## Safety and warranty

Hawkeye is a visualization and analysis tool. It is not a flight-safety device, not a ground control station, and not certified for any operational use. Do not make flight decisions based on what it displays.

The software is provided "AS IS", without warranty of any kind, as stated in [LICENSE](LICENSE). This project disclaims liability for any build, and it has no knowledge of and no responsibility whatsoever for builds it did not produce.

## Reporting an impersonating app

If you find an app passing itself off as official Hawkeye:

- Report it to Google Play through their [impersonation report form](https://support.google.com/googleplay/android-developer/answer/16341334).
- Open an issue on [PX4/Hawkeye](https://github.com/PX4/Hawkeye/issues) so maintainers know about it.

To report a security vulnerability, see [SECURITY.md](SECURITY.md) instead.

## Questions

If you are unsure whether your planned use of the name is okay, open an issue and ask. Questions asked in good faith are welcome, and it is easier to sort out before you ship than after.
