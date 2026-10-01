# Installing Ikasan Studio

The public beta is being prepared. Until a Marketplace release is published, install an approved candidate ZIP or [build the plugin](../README.md#build-and-contribute). This guide does not imply that a listing is already available. See the [README release status](../README.md) and [supported versions](SupportedVersions.md).

## Marketplace releases

Once published, use the official listing linked from this repository. Check the publisher, version and compatibility before installing, then restart IntelliJ if prompted.

A release in Marketplace's **beta channel** is separate from its default channel. To access it, open **Settings → Plugins → gear → Manage Plugin Repositories**, add `https://plugins.jetbrains.com/plugins/beta/list`, then search for **Ikasan Studio** in the Marketplace tab. This repository exposes beta updates for other plugins too. Follow the listing's release instructions; a hidden preview may instead require a direct listing link from the maintainers.

See JetBrains' [custom release channels](https://plugins.jetbrains.com/docs/marketplace/custom-release-channels.html) and [hidden plugins](https://plugins.jetbrains.com/docs/marketplace/hidden-plugin.html) documentation. These routes become useful only after the corresponding release has been published.

## Candidate ZIP or offline installation

Obtain the candidate ZIP from the maintainers, or build it into `build/distributions/`. In IntelliJ choose **Settings → Plugins → gear → Install Plugin from Disk**, select the ZIP and restart if prompted. Keep its version and source so feedback identifies the build you tested. Installing an exact candidate ZIP remains part of release verification.

Offline installation does not make project creation or builds offline: Maven dependencies, archetypes and optional harness downloads must already be available locally.

## Next steps

Run IntelliJ with its supplied runtime. Install and select Java 11 for an Ikasan 3.3.9 application or Java 17 for an Ikasan 4.1.6 application; the project SDK is separate from the IDE runtime. Continue with [Your first module](GettingStarted.md), [known limitations](KnownLimitations.md) and the [video tutorials](../tutorials/README.md).

Ikasan Studio is distributed under the [BSD 3-Clause License](../LICENSE.txt). Third-party dependencies retain their own licences and notices.
