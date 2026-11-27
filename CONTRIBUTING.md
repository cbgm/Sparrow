# Contributing to Sparrow

Thanks for your interest in Sparrow. Contributions are welcome, including bug reports, documentation fixes, tests, and code.

## Before you start

- For bugs and feature ideas, search existing [issues](https://github.com/cbgm/Sparrow/issues) before opening a new one.
- For substantial changes, open an issue first so the approach can be discussed before you invest time in an implementation.
- Keep changes focused and follow the existing module boundaries and patterns. See [Local development](docs/development/local-development.md) and [How to extend the project](docs/development/extending.md).
- Do not include credentials, signing keys, deployment state, personal data, or generated build artifacts in a change. Keep local configuration in the ignored `local.properties` and `.env` files.

## Build and test

Use the commands relevant to your change:

```text
gradlew.bat :androidApp:assembleDebug
gradlew.bat qualityCheck
gradlew.bat allTests
```

Use `./gradlew` instead of `gradlew.bat` on macOS and Linux. Server tooling and its tests are documented alongside the relevant component.

Some local builds may need configuration described in [Local development](docs/development/local-development.md). Never use production credentials in a pull request or test fixture.

## Pull requests

- Explain the problem and the behavior changed; link the related issue when applicable.
- Include or update tests for behavior changes and update user/developer documentation when needed.
- Run the relevant checks above and report anything you could not run.
- Keep unrelated refactors out of the change.

By submitting a contribution for inclusion, you agree that it is submitted under the Apache License, Version 2.0, as described in [LICENSE](LICENSE). Submit only work you have the right to contribute.
