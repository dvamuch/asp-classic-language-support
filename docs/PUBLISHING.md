# Publishing to JetBrains Marketplace

Marketplace updates are published by the
[`Publish to JetBrains Marketplace`](../.github/workflows/publish-marketplace.yml)
GitHub Actions workflow. The workflow runs the ordinary test suite and plugin
structure checks before calling the IntelliJ Platform Gradle Plugin's
`publishPlugin` task.

## One-time token setup

1. Open the JetBrains Marketplace profile page and create a permanent token in
   **My Tokens**.
2. In the GitHub repository, open **Settings | Secrets and variables |
   Actions**.
3. Create a repository secret named `JETBRAINS_MARKETPLACE_TOKEN` and paste the
   token as its value.

Never add the token to `gradle.properties`, a workflow file, an issue, or a
chat message. GitHub passes it to Gradle as `PUBLISH_TOKEN` only for the publish
job.

## Publishing a stable release

1. Set the release version in `build.gradle.kts`, update the change notes and
   commit the release preparation.
2. Push a tag with the same version, conventionally `v1.0.1` for Gradle version
   `1.0.1`.
3. Create and publish a GitHub Release from that tag.

Publishing the GitHub Release starts the workflow automatically. A normal
release is uploaded to the Marketplace `default` channel. The workflow refuses
to upload when the GitHub tag and Gradle version differ. JetBrains Marketplace
also rejects a second upload with an already published version number.

## Publishing a prerelease

A GitHub Release marked as a prerelease is uploaded to the `eap` channel. Users
of that build must add the corresponding custom Marketplace repository in their
IDE; custom-channel updates are not delivered to stable-channel users.

## Manual publishing

Open **Actions | Publish to JetBrains Marketplace | Run workflow**, select the
Git ref to build, and choose `default`, `beta`, or `eap`. Manual runs are useful
for retrying a failed Marketplace upload or publishing a custom-channel build.
They intentionally do not require a GitHub Release, so verify the Gradle version
before starting the run.

The first version of a Marketplace plugin must be uploaded manually. This
repository already has a Marketplace listing, so subsequent versions can use
the automated workflow.
