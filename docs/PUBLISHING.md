# Publishing Choreo to Maven Central

Choreo publishes via the [vanniktech `maven-publish`](https://github.com/vanniktech/gradle-maven-publish-plugin)
plugin to the **Sonatype Central Portal**. Coordinates:

```
io.github.tejbhansahu:choreo:<version>
```

## One-time setup

### 1. Central Portal account + namespace

1. Create an account at <https://central.sonatype.com>.
2. Register the namespace **`io.github.tejbhansahu`**. For an `io.github.*` namespace, verification
   is automatic once you prove the GitHub account — the portal asks you to create a throwaway public
   repo whose name it specifies. (You already own `github.com/tejbhansahu`, so this is quick.)
3. Generate a **user token** (portal → Account → Generate User Token). This gives a username/password
   pair used below — not your login password.

### 2. GPG signing key

Central requires signed artifacts.

```bash
# Generate a key (pick RSA 4096, no expiry is fine for a start)
gpg --gen-key

# Find the key id
gpg --list-secret-keys --keyid-format short

# Export the ASCII-armored private key (used as the in-memory signing key)
gpg --armor --export-secret-keys <KEY_ID>

# Publish the public key so Central can verify it
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

### 3. Provide credentials

**Locally** — put these in `~/.gradle/gradle.properties` (never commit them):

```properties
mavenCentralUsername=<central-portal-token-username>
mavenCentralPassword=<central-portal-token-password>

signingInMemoryKey=<the ASCII-armored private key, newlines as \n or a multiline value>
signingInMemoryKeyPassword=<gpg key passphrase>
```

**In CI** — add these as GitHub Actions repository **secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
| `SIGNING_IN_MEMORY_KEY` | ASCII-armored GPG private key |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | GPG key passphrase |

## Releasing

### Via CI (recommended)

1. Bump `version` in [`choreo/build.gradle.kts`](../choreo/build.gradle.kts) (drop `-SNAPSHOT` for a release).
2. Commit, tag, and create a **GitHub Release** (`v0.1.0`). The [`Publish`](../.github/workflows/publish.yml)
   workflow runs on macOS and uploads a deployment to Central.
3. Finish the release in the Central Portal (or switch the task to `publishAndReleaseToMavenCentral`
   to auto-release).

### Locally

```bash
# Dry run — publishes to your local Maven repo, no credentials needed:
./gradlew publishToMavenLocal

# Real upload to Central:
./gradlew publishToMavenCentral --no-configuration-cache
```

## Versioning

- Release versions: `0.1.0`, `0.2.0`, …
- Snapshots: append `-SNAPSHOT` (e.g. `0.2.0-SNAPSHOT`) — these go to the snapshots repo.
- Update [`CHANGELOG.md`](../CHANGELOG.md) for every release.
