# Contributing

Thank you for helping improve the Minecraft 1.20.1 port. Please direct port-related issues and pull requests to this repository.

## Report a bug

Provide the mod, Minecraft, Forge, and Java versions; concise reproduction steps; expected and actual behavior; and a relevant error excerpt. If other mods are involved, include their names and versions and indicate whether the problem reproduces in a separate instance.

Remove account tokens, usernames, personal file paths, IP addresses, and other private information from logs and screenshots before submitting them. Do not upload entire launcher profiles, game directories, server worlds, or session records.

For a security vulnerability, follow [SECURITY.md](SECURITY.md) rather than publishing exploit details in an ordinary issue.

## Submit a change

1. Open an issue for substantial behavior changes.
2. Create a focused branch and describe the problem and resulting behavior.
3. Build with Java 17 and run the checks relevant to the change.
4. Preserve upstream credits, existing registry identifiers, and applicable notices.
5. Explain validation and known compatibility limits in the pull request.

Changes should retain established gameplay behavior unless the change is intentional and documented. Keep unrelated formatting, generated outputs, logs, screenshots from private sessions, and local settings out of the patch.

## Development references

- [Build and test commands](docs/DEVELOPMENT.md)
- [Configuration](docs/CONFIGURATION.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Upstream attribution](CREDITS.md)

Submitting a contribution does not transfer ownership of upstream material or introduce a new license for it. Discuss licensing questions before contributing material with incompatible terms.
