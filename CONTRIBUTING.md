# Contributing to Vitrum

Thanks for helping. Vitrum targets weak Fire TV hardware and must stay fully usable with a D-pad.

## Ground rules

- Keep code, comments and commit messages in English. User-facing text lives in both `values/strings.xml` and `values-it/strings.xml`.
- No Google Play Services, Firebase, analytics or telemetry. Network access is limited to the GitHub API and Open-Meteo (weather), plus an optional user-supplied background URL.
- Do not add third-party names, logos, artwork or fonts. The Inter fonts and generated brand assets are already bundled.
- Keep `minSdk 22`. Check that new dependencies do not require a newer minimum API.
- Every new control must be focusable and reachable with D-pad + OK + Back. Avoid focus traps and fixed pixel sizes.

## Before opening a pull request

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

- Add or update JVM tests for pure logic (versions, OTA parsing, filtering, settings).
- Test on a TV emulator if you change layout, focus, blur or overscan. Mention what you could not test on real hardware.
- Use small conventional commits (`feat:`, `fix:`, `docs:`, `chore:`).

## Release signing

Release keystores never enter the repository. Maintainers keep the keystore outside the project and provide `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` as GitHub Actions secrets. Losing the keystore breaks OTA updates for installed devices.
