# Releasing

Releases are cut by pushing a `v*` tag.
`.github/workflows/release.yml` is the only thing that reacts to that tag, and it produces every published artifact.
No version number is stored anywhere in the repository; the tag is the single source of truth.

::: info
The release workflow has no trigger other than the tag push, so it cannot be dry-run.
Before tagging, exercise the build with the pre-tag check described in [Checking a release build before tagging](#checking-a-release-build-before-tagging).
:::

## Cutting a release

Tag the commit and push the tag:

```sh
git tag -a v0.4.0 -m "v0.4.0"
git push origin v0.4.0
```

The workflow creates the GitHub release immediately, then each platform job uploads its artifact as it finishes.
The release is published rather than drafted, because the macOS bottle build needs the source tarball URL to be publicly fetchable.

## Release artifacts

A tag produces six assets:

| Artifact                                       | Platform    | Job              |
| ---------------------------------------------- | ----------- | ---------------- |
| `hawkeye-<version>.tar.gz`                     | Source      | `source-tarball` |
| `hawkeye-<version>.arm64_sonoma.bottle.tar.gz` | macOS arm64 | `bottle-arm64`   |
| `hawkeye_<version>_amd64.deb`                  | Linux amd64 | `deb-amd64`      |
| `hawkeye_<version>_arm64.deb`                  | Linux arm64 | `deb-arm64`      |
| `hawkeye-<version>-windows-x64.zip`            | Windows x64 | `windows-x64`    |
| `hawkeye-<version>-android-unsigned.apk`       | Android     | `android-apk`    |

The `.deb` files use Debian policy naming with underscores, which is why the install command in [Installation](../installation.md) globs `hawkeye_*.deb` and not `hawkeye-*.deb`.

Every platform job depends only on `source-tarball`, so one platform failing does not block the release or the other artifacts.
A failed job leaves the release published with that asset missing; re-upload it by hand with `gh release upload <tag> <file>` once the cause is fixed.

A seventh job, `update-tap`, is the exception.
It depends on `bottle-arm64` as well as `source-tarball`, and it pushes the updated formula to the [PX4/homebrew-px4](https://github.com/PX4/homebrew-px4) tap.
If the bottle build fails, the tap keeps pointing at the previous version while the GitHub release advertises the new one, so `brew install hawkeye` silently serves the old build until the job is re-run.
That is the only failure in this workflow with a user-visible consequence beyond a missing asset.

## How the version is derived

The `source-tarball` job strips the leading `v` from the tag and exports the result, and every other job reads it from there.
The two build systems receive it differently:

| Build system | How it receives the version      |
| ------------ | -------------------------------- |
| CMake        | `-DHAWKEYE_VERSION=<version>`    |
| Gradle       | `-PhawkeyeVersionName=<version>` |

`android/app/build.gradle.kts` computes the Android `versionCode` from that string as `major * 1000000 + minor * 1000 + patch`, so CI passes one value and Gradle derives the other:

| Tag      | versionName | versionCode |
| -------- | ----------- | ----------- |
| `v0.3.0` | `0.3.0`     | 3000        |
| `v0.4.0` | `0.4.0`     | 4000        |
| `v1.2.3` | `1.2.3`     | 1002003     |
| no tag   | `0.0.0-dev` | 1           |

A local build with no `-PhawkeyeVersionName` falls back to `0.0.0-dev`, so debug builds need no extra flags.
The version is parsed strictly: a tag that is not `MAJOR.MINOR.PATCH` with an optional suffix, or that has a component outside `0..999`, fails the Android build rather than producing a misleading version code.

::: warning
A prerelease tag reuses the version code of the release it precedes.
`v0.4.0-rc1` produces versionName `0.4.0-rc1` but versionCode `4000`, the same code the eventual `v0.4.0` gets, and Android refuses to install an APK whose version code is not greater than the installed one.
Avoid prerelease tags until the derivation accounts for them.
:::

## The Android APK

The `android-apk` job builds a single universal APK containing `arm64-v8a` and `x86_64`.
There is no `armeabi-v7a` build, so 32-bit ARM devices are not supported.
The minimum supported platform is Android 10 (API 29).

The APK is unsigned.
There is no signing configuration in the project and no keystore secret in the repository, so the artifact is named `-unsigned` to make that obvious.
It has to be signed with your own key before it will install; see [Signing a release APK](https://github.com/PX4/Hawkeye/blob/main/android/README.md#signing-a-release-apk) in the Android README.

Because the APK cannot be launched on a CI runner without an emulator, `android/scripts/verify-release-apk.sh` asserts on its contents instead:

- Both `lib/arm64-v8a/libhawkeye.so` and `lib/x86_64/libhawkeye.so` are present.
- All four asset trees are packaged: `assets/models/`, `assets/shaders/`, `assets/fonts/`, and `assets/themes/`.
  These are symlinks into the repository root, so the check catches a runner that failed to materialize them.
- The `versionName` AGP recorded matches the tag, and the `versionCode` is one Android will accept.

That script takes the APK output directory and the expected version, so you can run the same check locally against your own build.

## Checking a release build before tagging

`.github/workflows/android.yml` has a `release-build` job that runs the same `assembleRelease` task and the same verification script the release uses.
It runs on pushes to `main` and on manual dispatch, and is skipped on pull requests to keep review turnaround fast.

Trigger it from a branch before tagging:

```sh
gh workflow run android.yml --ref my-branch
```

The push-to-`main` run of that job also warms the native build cache the release job reads, because a tag run restores caches from the default branch.
A `--ref my-branch` dispatch does not warm it, since caches written on a branch are not visible to a later tag run.

The workflow's path filter covers the repository root `src/`, `lib/`, `fonts/`, `models/`, `shaders/`, and `themes/` directories in addition to `android/`.
The Android native library compiles source files out of the root `src/` tree and its assets are symlinks to the root asset directories, so a desktop-side change can break the APK and has to trigger Android CI.
