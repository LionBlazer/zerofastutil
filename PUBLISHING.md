# Publishing ZeroFastUtil to Maven Central

## 1. Sonatype Central Portal setup

1. Log in to https://central.sonatype.com.
2. Verify namespace `io.github.lionblazer` (already done).
3. Create a **User Token** in your account settings.

## 2. Local credentials

Put credentials and signing key in `~/.gradle/gradle.properties`:

```properties
sonatypeUsername=<Central Portal User Token username>
sonatypePassword=<Central Portal User Token password>
signingKey=<ASCII-armored private key; use \n for line breaks>
signingPassword=<GPG key passphrase>
```

`signingKey` must be your private key (not public key).

## 3. Versioning

- `projectVersion` is your published fork version.
- `upstreamFastutilVersion` is the upstream fastutil version used as source fallback for generation.

Both are in project `gradle.properties`.

## 4. Dry run

```bash
./gradlew clean build
./gradlew publishToMavenLocal
```

## 5. Publish release to Central

```bash
./gradlew clean publishReleaseToCentral
```

This runs:

1. `publishToSonatype`
2. `closeAndReleaseSonatypeStagingRepository`

## 6. Publish snapshot

Set `projectVersion` to a `-SNAPSHOT` version, then run:

```bash
./gradlew clean publishToSonatype
```
