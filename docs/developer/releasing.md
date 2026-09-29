# Releasing

Releases are cut by pushing a tag, and desktop and Android release independently:

| Tag                  | Example          | What ships                                                                         |
| -------------------- | ---------------- | ---------------------------------------------------------------------------------- |
| `desktop-v<version>` | `desktop-v1.1.0` | Source tarball, macOS bottle, Linux `.deb`s, Windows zip, and the Homebrew tap     |
| `android-v<version>` | `android-v1.0.1` | The Android APK on a GitHub release, and the AAB to the Google Play internal track |

Neither kind ships anything from the other, and each has its own version sequence.
A bare `v<version>` tag, which released everything at once up to `v1.0.0`, is rejected: the workflow fails in its first job with a pointer to the two prefixes, before anything is published.

`.github/workflows/release.yml` is the only workflow that reacts to these tags. It works out which kind of release the tag is, creates the release as a draft, calls the workflows in the same directory that build and upload every artifact, and publishes the release once they all succeed.
No version number is stored anywhere in the repository; the tag is the single source of truth.
To see what a tag would do without pushing it, run the script the workflow's first job runs:

```sh
.github/scripts/release/get-release-versions.sh android-v1.0.1-rc1
```

::: info
The release workflow has no trigger other than the tag push, so it cannot be dry-run.
Before tagging, exercise the build with the pre-tag check described in [Checking a release build before tagging](#checking-a-release-build-before-tagging).
:::

## Cutting a release

Tag the commit and push the tag:

```sh
git tag -a desktop-v1.1.0 -m "desktop-v1.1.0"
git push origin desktop-v1.1.0
```

An Android release is the same with the `android-v` prefix.

Every release goes through the same lifecycle, owned by `release.yml`: it creates a draft release, the called workflows build and upload artifacts to that draft, and `release.yml` publishes it only once every upload has succeeded.
Nobody sees a release until it is complete, and a failed run leaves an invisible draft rather than a public release with assets missing.

| Workflow               | Job            | Runs for                                  | Does                                                                                                     |
| ---------------------- | -------------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `release.yml`          | `version`      | every tag                                 | Runs `.github/scripts/release/get-release-versions.sh` to derive the scope, version, and prerelease flag |
| `release.yml`          | `create-draft` | every valid tag                           | Creates the draft release with its title, generated notes, and prerelease flag                           |
| `desktop-release.yml`  | all            | `desktop-v*`                              | Builds and uploads the source tarball, `.deb`s, and Windows zip                                          |
| `homebrew-release.yml` | `bottle-arm64` | `desktop-v*`, after `desktop-release.yml` | Builds and uploads the macOS bottle                                                                      |
| `android-release.yml`  | `android`      | `android-v*`                              | Builds, verifies, and uploads the APK, and uploads the AAB to Google Play                                |
| `release.yml`          | `publish`      | every valid tag, after all uploads        | Publishes the draft and sets whether it is Latest                                                        |
| `homebrew-tap.yml`     | `update-tap`   | `desktop-v*`, after `publish`             | Points the Homebrew formula at the published release                                                     |

The called workflows only run when `release.yml` calls them (`workflow_call`), so none can be triggered on its own by a tag, and none creates, edits, or publishes the release itself.
Secrets are passed only where needed: `android-release.yml` inherits the repository secrets for signing, Crashlytics, and Play, `homebrew-tap.yml` gets only `HOMEBREW_TAP_TOKEN`, and the others use nothing beyond the workflow's `GITHUB_TOKEN`.

The workflows themselves only wire jobs together.
Any step longer than two lines lives in `.github/scripts/` instead, where it can be read, linted, and run locally.
They are grouped by the pipeline that owns them, so each directory maps to a workflow:

| Directory                   | Used by                                    | Scripts                                                                                                                                               |
| --------------------------- | ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.github/scripts/release/`  | `release.yml`                              | `get-release-versions.sh`, `create-draft.sh`, `publish.sh`                                                                                            |
| `.github/scripts/desktop/`  | `desktop-release.yml`                      | `make-source-tarball.sh`, `install-linux-deps.sh`, `configure-linux.sh`, `smoke-test-linux-deb.sh`, `stage-windows-zip.ps1`, `smoke-test-windows.ps1` |
| `.github/scripts/homebrew/` | `homebrew-release.yml`, `homebrew-tap.yml` | `render-formula.py`, `package-bottle.sh`, `update-tap.sh`                                                                                             |
| `.github/scripts/android/`  | `android-release.yml`                      | `stage-apk.sh`, `upload-crashlytics-symbols.sh`                                                                                                       |

A new script goes in the directory of the workflow that calls it; one shared by two workflows of the same pipeline, like `render-formula.py`, goes in that pipeline's directory.
The APK and AAB verification scripts stay in `android/scripts/`, because `android.yml` runs them on every merge too and they belong to the app rather than to the release.
Those scripts are Bash when they mostly drive other tools (`gh`, `tar`, `apt`, `cmake`, `brew`, `git`, Gradle), Python for `homebrew/render-formula.py`, which renders the one formula template both the bottle build and the tap use, and PowerShell for the Windows steps.

A tag carrying a suffix, such as `desktop-v1.1.0-rc1`, is published as a prerelease; a bare `MAJOR.MINOR.PATCH` version is published as a full release.
`gh release create` does not infer this from the tag, so the workflow derives it from the version string and passes `--prerelease` explicitly.
Only a final desktop release is marked Latest when published, which is where the README's desktop download links and anyone landing on the releases page are pointed.

### When a release run fails

A failed upload leaves the release as a draft, visible only to maintainers on the releases page.
Fix the cause and use **Re-run failed jobs** on the run; the skipped jobs downstream of the failure, including `publish`, run once it passes.
Re-running the whole workflow works too: `create-draft` reuses the existing draft, and every upload replaces an asset of the same name.
`create-draft` refuses to touch a release that is already published, so a stray re-run cannot rewrite a shipped release.

To abandon a release instead, delete the draft with `gh release delete <tag>` and, if the tag itself was wrong, the tag with `git push --delete origin <tag>`.

Google Play is the exception to "nothing is public until publish": the Android workflow uploads the AAB to the internal track before the GitHub release is published, so an Android run that fails after that step has already reached internal testers.

## Desktop releases

A `desktop-v*` tag produces a release titled `Desktop v<version>` with five assets:

| Artifact                                       | Platform    | Workflow               | Job              |
| ---------------------------------------------- | ----------- | ---------------------- | ---------------- |
| `hawkeye-<version>.tar.gz`                     | Source      | `desktop-release.yml`  | `source-tarball` |
| `hawkeye-<version>.arm64_sonoma.bottle.tar.gz` | macOS arm64 | `homebrew-release.yml` | `bottle-arm64`   |
| `hawkeye_<version>_amd64.deb`                  | Linux amd64 | `desktop-release.yml`  | `deb-amd64`      |
| `hawkeye_<version>_arm64.deb`                  | Linux arm64 | `desktop-release.yml`  | `deb-arm64`      |
| `hawkeye-<version>-windows-x64.zip`            | Windows x64 | `desktop-release.yml`  | `windows-x64`    |

The `.deb` files use Debian policy naming with underscores, which is why the install command in [Installation](../installation.md) globs `hawkeye_*.deb` and not `hawkeye-*.deb`.

Inside `desktop-release.yml`, every platform job depends only on `source-tarball`, so one platform failing does not stop the others from building and uploading; it does keep the release a draft until that job is re-run.

### Homebrew

Homebrew is split in two, around the publish step.

`homebrew-release.yml` runs after all of `desktop-release.yml` succeeds and takes the source tarball's checksum from it.
Draft assets are not publicly downloadable, so its `bottle-arm64` job downloads the tarball from the draft with `gh release download` and builds the bottle from that local copy, which brew still verifies against the same checksum.

`homebrew-tap.yml` runs only after `publish`, because the formula it pushes to [PX4/homebrew-px4](https://github.com/PX4/homebrew-px4) points at the release's public download URLs, which do not resolve while the release is a draft.
The tap therefore only ever moves to a complete, published release.
If `update-tap` itself fails, the release is public but `brew install hawkeye` keeps serving the previous version until that job is re-run; this is the one failure with a user-visible consequence.

The formula points at the source tarball rather than GitHub's automatic source archive for two reasons: the automatic archive leaves out the `lib/c_library_v2` submodule the build needs, and its checksum is not guaranteed to stay the same, while the formula pins a `sha256`.

## Android releases

An `android-v*` tag runs only `android-release.yml` between the draft and publish steps.
It builds and verifies the APK and AAB, uploads native symbols to Crashlytics, uploads `hawkeye-<version>-android.apk` to the draft, and sends the AAB to Google Play; see [Google Play internal testing](#google-play-internal-testing).
The release is titled `Android v<version>`.

`publish` passes `--latest=false` for Android releases, so they never take the Latest slot.
Latest belongs to the last desktop release, because that is where the README's desktop download links go.
The Android install instructions point at the releases page instead, filtered to `android-v` tags.

Android versions only have to increase from one `android-v*` tag to the next; desktop versions do not affect them.
Google Play refuses any `versionCode` it has already seen or that is lower than the current one, and Play already holds `1.0.0` from the `v1.0.0` tag, so the first Android release under this scheme has to be `android-v1.0.1` or higher.

## How the version is derived

The `version` job strips the `desktop-v` or `android-v` prefix from the tag and exports the result, and every other job reads it from there.
The two build systems receive it differently:

| Build system | How it receives the version      |
| ------------ | -------------------------------- |
| CMake        | `-DHAWKEYE_VERSION=<version>`    |
| Gradle       | `-PhawkeyeVersionName=<version>` |

`android/app/build.gradle.kts` computes the Android `versionCode` from that string as `major * 100000000 + minor * 100000 + patch * 100 + rc`, so CI passes one value and Gradle derives the other:

| Tag                  | versionName | versionCode |
| -------------------- | ----------- | ----------- |
| `android-v0.4.0-rc1` | `0.4.0-rc1` | 400001      |
| `android-v0.4.0`     | `0.4.0`     | 400099      |
| `android-v1.2.3`     | `1.2.3`     | 100200399   |
| no tag               | `0.0.0-dev` | 1           |

The rc component is what makes prerelease tags safe: a final release takes 99, an `rcN` suffix takes N (1 through 98), and the `dev` and `ci` fallbacks take 0, so every rc sorts below its final release, above the previous release, and each code can be uploaded to Google Play exactly once.
A local build with no `-PhawkeyeVersionName` falls back to `0.0.0-dev`, so debug builds need no extra flags.
The version is parsed strictly: a tag that is not `MAJOR.MINOR.PATCH` with an optional `rcN` suffix, that has a component outside `0..999`, or whose major exceeds 20 (past which the derivation overflows Google Play's version code cap) fails the Android build rather than producing a misleading version code.

## The Android APK

The `android` job builds a single universal APK containing `arm64-v8a` and `x86_64`.
There is no `armeabi-v7a` build, so 32-bit ARM devices are not supported.
The minimum supported platform is Android 10 (API 29).

The APK is signed with the project's upload key, which the job decodes from the `ANDROID_UPLOAD_KEYSTORE_BASE64`, `ANDROID_UPLOAD_KEYSTORE_PASSWORD`, and `ANDROID_UPLOAD_KEY_ALIAS` repository secrets, and the asset installs as downloaded.
A fork without those secrets falls back to the pre-signing behavior: the artifact is built unsigned, named `hawkeye-<version>-android-unsigned.apk` to make that obvious, and has to be signed before it will install; see [Signing an APK yourself](https://github.com/PX4/Hawkeye/blob/main/android/README.md#signing-an-apk-yourself) in the Android README.

Because the APK cannot be launched on a CI runner without an emulator, `android/scripts/verify-release-apk.sh` asserts on its contents instead:

- Both `lib/arm64-v8a/libhawkeye.so` and `lib/x86_64/libhawkeye.so` are present.
- All four asset trees are packaged: `assets/models/`, `assets/shaders/`, `assets/fonts/`, and `assets/themes/`, along with `assets/NOTICE.md`, which the in-app About screen renders.
  These are symlinks into the repository root, so the check catches a runner that failed to materialize them.
- The `versionName` AGP recorded matches the tag, and the `versionCode` is one Android will accept.
- With `--signed`, which the job passes whenever the keystore secrets are present, `apksigner verify` confirms the signature.

That script takes the APK output directory and the expected version, so you can run the same check locally against your own build.

## Google Play internal testing

The same `android` job also runs `bundleRelease`, checks the resulting AAB with `android/scripts/verify-release-bundle.sh`, and uploads it to the Google Play internal test track under the Dronecode Foundation account.
The upload step runs only when the `PLAY_SERVICE_ACCOUNT_JSON` secret is present; it authenticates as a Google Cloud service account granted release-to-testing permission on the app in the Play Console.
Prerelease tags upload like any other tag; rc builds are what the internal track is for.
Promotion beyond internal testing is manual in the Play Console.

The AAB is not attached to the GitHub release.
Google Play is its only destination, and Play App Signing re-signs it with the app signing key Google holds, so a Play install and a sideloaded APK carry different signatures and cannot upgrade over each other.

Anyone who sideloaded a self-signed APK from a release cut before signing landed (v0.3.0 and earlier) has to uninstall it once before an official signed build will install; release notes should carry that reminder until it stops being relevant.

### Store listing wording

No Play listing text lives in this repository; the workflow uploads the AAB and nothing else, so the listing is maintained by hand in the Play Console.
Because the project is open source and anyone may publish a fork, the listing has to make it obvious which app this is.
Whoever edits it should keep three things in the description:

- That this is the official Hawkeye app, published by the Dronecode Foundation.
- A link to <https://github.com/PX4/Hawkeye>.
- That Hawkeye is a visualization and analysis tool rather than a flight-safety device.

Forks are asked to change their app name, icon, and `applicationId` before publishing, and to carry a non-affiliation notice; the policy they are pointed at is [FORKS.md](https://github.com/PX4/Hawkeye/blob/main/FORKS.md) in the repository root.
If a listing turns up that misrepresents itself as this app, report it through Google Play's [impersonation report](https://support.google.com/googleplay/android-developer/answer/16341334).

### Privacy policy and Data safety

Google Play requires both a privacy policy URL and a completed Data safety form for every app, including apps that collect nothing, and both must be in place before the app is promoted beyond the internal test track.
Neither can be scripted; they are filled in by hand in the Play Console.

- The privacy policy URL is <https://px4.github.io/Hawkeye/privacy>, published from [`docs/privacy.md`](../privacy.md).
- The Data safety form declares three data types, all *collected*, none *shared*, and all marked **optional** because crash reporting can be turned off in Settings:
  - **Crash logs** and **Diagnostics**, under *App info and performance*.
  - **Device or other IDs**, because Crashlytics sends a Crashlytics installation UUID and a Firebase installation ID with each report. This one is easy to miss: it comes from the SDK rather than from anything Hawkeye asks for, and Firebase's own [Play data disclosure](https://firebase.google.com/docs/android/play-data-disclosure) is the authority on what has to be declared.
  - Every type is answered *not processed ephemerally* (Google retains reports for 90 days) with purpose **Analytics**, whose Play definition covers diagnosing and fixing crashes. Security practices declare data encrypted in transit; data deletion is declared as not offered, since reports key to an anonymous install ID that no user can identify as theirs.

That declaration holds because of what the app actually does, and it is worth knowing why rather than taking it on faith.
Google defines collection as transmitting data off the device, and explicitly exempts data that is only processed locally.
Hawkeye's flight logs, log library, and settings never leave the device: there is no Hawkeye server and no analytics, and Android backup is disabled so the platform does not copy them either.
Live telemetry travels only between the app and the vehicle or simulator the user connected to.
The single outbound path is Firebase Crashlytics, which sends a crash trace, device state, and an installation identifier when the app fails. It carries nothing from a flight log, which is why the location categories stay unchecked.

The form and the policy have to agree, since a mismatch is a common cause of listing rejection.
Anything that changes the answer, in particular adding an analytics or advertising dependency, widening what Crashlytics reports, or re-enabling `android:allowBackup`, means updating `docs/privacy.md` and the Data safety form together.

Crashlytics also needs two repository secrets, and a release built without them is not broken, just unreported: `FIREBASE_GOOGLE_SERVICES_JSON` (base64 of `google-services.json`) and `FIREBASE_SERVICE_ACCOUNT_JSON` (a service account key for the Firebase project, holding `roles/firebasecrashlytics.admin`; the Gradle upload task reads it through `GOOGLE_APPLICATION_CREDENTIALS`).
The two are gated independently. Without `FIREBASE_GOOGLE_SERVICES_JSON` the build ships with crash reporting dormant, exactly as a fork's CI does. Without `FIREBASE_SERVICE_ACCOUNT_JSON` the build still reports crashes, but the symbol upload is skipped and native stack traces arrive as raw addresses.
The symbol upload runs before the APK is published, so a failure there stops the release rather than shipping a build whose native crashes cannot be read.

Verifying crash reporting on a device has one trap worth knowing.
Crashlytics persists the user's choice itself, and reinstalling does not reset it, so a device that ran an earlier build keeps whatever that build left behind.
When the choice is off, reports are still captured but never uploaded, and logcat reads `Crashlytics automatic data collection DISABLED by API`.
Turn the switch back on in Settings before treating this as a broken configuration.
Clearing the app's data works too, at the cost of that device's log library.

## Checking a release build before tagging

`.github/workflows/android.yml` has a `release-build` job that runs the same `assembleRelease` and `bundleRelease` tasks and the same verification scripts the release uses, including the signed path when the keystore secrets are present.
It runs on pushes to `main` and on manual dispatch, and is skipped on pull requests to keep review turnaround fast.

Trigger it from a branch before tagging:

```sh
gh workflow run android.yml --ref my-branch
```

The push-to-`main` run of that job also warms the native build cache the release job reads, because a tag run restores caches from the default branch.
A `--ref my-branch` dispatch does not warm it, since caches written on a branch are not visible to a later tag run.

The workflow's path filter covers the repository root `src/`, `lib/`, `fonts/`, `models/`, `shaders/`, and `themes/` directories in addition to `android/`.
The Android native library compiles source files out of the root `src/` tree and its assets are symlinks to the root asset directories, so a desktop-side change can break the APK and has to trigger Android CI.
