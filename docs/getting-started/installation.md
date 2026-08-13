# Installation

## Overview

This guide explains how to set up a SecureChat development environment from scratch.

The project has been designed so that every developer works in an identical environment with as little manual configuration as possible.

SecureChat intentionally minimizes external tooling. Everything related to the build, quality checks and architecture validation is handled through Gradle.

---

# Requirements

The following software is required.

| Software | Required | Notes |
|----------|----------|------|
| Git | ✅ | Source control |
| JDK 21 | ✅ | Required by Gradle |
| Android Studio | ✅ | Latest stable version |
| Android SDK | ✅ | Installed through Android Studio |

Nothing else is required for normal development.

In particular, developers **do not need**

- Python
- Docker
- Node.js
- Ruby
- Bash

---

# Clone the Repository

Clone SecureChat normally.

```bash
git clone https://github.com/<organization>/SecureChat.git
```

Enter the repository.

```bash
cd SecureChat
```

---

# Open the Project

Open Android Studio.

Choose

```
Open
```

and select

```
SecureChat/
```

Do **not** open individual modules.

The project root contains

- Version Catalog
- Included Build Logic
- Gradle Settings
- Documentation

---

# First Gradle Sync

Android Studio automatically performs a Gradle Sync.

During the first synchronization Gradle will

- download dependencies
- compile build logic
- compile convention plugins
- generate Version Catalog accessors
- configure every project module

The initial synchronization can take several minutes depending on network speed.

Subsequent synchronizations are significantly faster.

---

# Run Initial Setup

Execute

```bash
./gradlew setup
```

This initializes the development environment.

Currently it performs

- installation of tracked Git hooks

Future versions may extend this task with additional project initialization.

---

# Verify Git Hooks

The setup task configures Git to use the repository-managed hooks.

Verify the configuration.

```bash
git config --get core.hooksPath
```

Expected output

```
.githooks
```

If another value is shown execute

```bash
./gradlew setup
```

again.

---

# Build Logic

SecureChat uses an included Gradle build.

```
build-logic/
```

This project contains

- convention plugins
- quality plugin
- architecture plugin
- custom Gradle tasks

The build logic is compiled automatically.

Developers normally never modify it unless working on the build infrastructure itself.

---

# Build the Project

Run

```bash
./gradlew build
```

This performs

- compilation
- tests
- KtLint verification
- Detekt
- architecture validation
- generated documentation verification

A successful build indicates that the environment has been configured correctly.

---

# Recommended IDE Settings

Enable

- Optimize imports on save
- Reformat code on save
- Show trailing whitespace
- UTF-8 file encoding

Disable

- Wildcard imports

The project formatting is enforced by KtLint, but enabling these settings reduces unnecessary formatting changes.

---

# Android SDK

Install the Android SDK version required by the project.

The compile SDK, minimum SDK and target SDK are configured centrally by the SecureChat convention plugins.

Individual modules must never override these values.

---

# Android Emulator

Create at least one Android Virtual Device.

Recommended configuration

| Setting | Recommendation |
|----------|----------------|
| Device | Pixel |
| API | Latest supported by the project |
| RAM | Default |
| Graphics | Hardware |

When testing messaging functionality it is recommended to use two emulators simultaneously.

---

# Verify the Quality Pipeline

Execute

```bash
./gradlew quality
```

This command performs

1. Source formatting
2. Static analysis
3. Architecture validation
4. Documentation verification

The command should complete without errors before opening a Pull Request.

---

# Repository Layout

The project root contains several important directories.

| Directory | Purpose |
|-----------|---------|
| androidApp | Android application |
| build-logic | Convention plugins and Gradle infrastructure |
| config | Tool configuration |
| core | Shared libraries |
| data | Data layer |
| docs | Project documentation |
| feature | Feature modules |
| navigation | Navigation |
| quality | Custom Detekt rules |
| shared | Shared application modules |
| startup | Startup module |

Developers should become familiar with these directories before making changes.

---

# Updating the Repository

Update the local repository normally.

```bash
git pull
```

If build logic changes are included, Gradle recompiles the convention plugins automatically during the next synchronization.

No additional steps are required.

---

# Common Commands

Build the project

```bash
./gradlew build
```

Run quality checks

```bash
./gradlew qualityCheck
```

Automatically format code

```bash
./gradlew qualityFix
```

Validate architecture

```bash
./gradlew validateArchitecture
```

Generate architecture documentation

```bash
./gradlew architectureReport
```

Run all developer checks

```bash
./gradlew quality
```

---

# Troubleshooting

## Build Logic Does Not Compile

Execute

```bash
./gradlew clean
```

followed by

```bash
./gradlew build
```

---

## Git Hooks Do Not Run

Execute

```bash
./gradlew setup
```

again.

---

## Architecture Validation Fails

Run

```bash
./gradlew architectureReport
```

Inspect the generated documentation to understand the reported violation.

---

## Detekt Reports Violations

Correct the reported issues.

Suppressions should only be used when a rule cannot reasonably be satisfied.

---

# Next Steps

After the project builds successfully continue with

- **First Build**
- **Development Workflow**
- **Project Structure**

These guides explain how development is performed within the SecureChat project.
