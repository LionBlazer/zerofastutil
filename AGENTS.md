# Repository Guidelines

## Repository Context
This repository is a fork of `fastutil`.

## Agent-Specific Instructions
- In this chat, the assistant must reply in Russian.

## Project Structure & Module Organization
This repository is a Java library with generated sources.
- `drv/`: driver templates (`*.drv`) used to generate type-specific classes.
- `src/it/unimi/dsi/fastutil/`: main Java sources (generated and hand-maintained utility classes).
- `test/it/unimi/dsi/fastutil/`: JUnit 4 test suite (`*Test.java`).
- `guava/`: Guava-based compatibility tests.
- `lib/`: local test dependencies (`junit-4.13.jar`, `hamcrest-all-1.3.jar`).
- Build/config files: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradle/wrapper/`.

## Build, Test, and Development Commands
- `./gradlew clean build`: full build and tests.
- `./gradlew assemble`: compile and package jar.
- `./gradlew test`: run JUnit tests.
- `./gradlew javadoc`: generate API docs.
- `./gradlew dist`: build distribution artifacts into `dist/lib`.
- `./gradlew publishToMavenLocal`: publish locally for dependency testing.
- `./gradlew publishReleaseToCentral`: publish signed release to Sonatype Central.
- `./gradlew clean`: remove build artifacts.

## Coding Style & Naming Conventions
- Target Java 8 (`release="8"` in Ant build).
- Use Eclipse formatter settings in `.settings/` (tabs enabled, tab size 4, indentation size 8).
- Keep package layout under `it.unimi.dsi.fastutil.<typepackage>`.
- Follow existing type-specific naming patterns, e.g. `IntArrayList`, `Int2ObjectOpenHashMap`, `ObjectOpenHashSet`.
- Prefer editing `drv/*.drv` for broad behavioral changes, then regenerate sources.

## Testing Guidelines
- Framework: JUnit 4 with Hamcrest.
- Test class names must end with `Test` and mirror the package/class under test.
- Run `./gradlew test` before submitting changes.
- When touching generated families, run the relevant package tests (for example, `ints`, `objects`, `io`) and regenerate sources consistently.

## Commit & Pull Request Guidelines
- Commit messages in history are short, imperative/past-tense summaries (e.g., `Fixed docs and tests`, `Added note about ...`).
- Keep subject lines concise and specific to one logical change.
- PRs should include:
  1. What changed and why.
  2. Regeneration/source strategy (local generated sources vs merged upstream sources artifact).
  3. Test evidence (`./gradlew test` result and impacted test areas).
  4. Linked issue/PR context when applicable.
