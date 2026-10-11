# Changelog

## 4.0.0-alpha.2 — 2026-10-11

- Fixed raspberry, blueberry, blackberry, and maloberry bushes requiring sustained mining and playing stone sounds. All four bushes now break immediately with an empty hand and use the grass breaking sound, matching the original mod.
- Restored the bushes' original green map color.
- Updated the conversion tool to read the shared legacy berry-bush properties so regenerating registrations preserves the fix.
- Expanded the existing plant GameTest to check all four bushes for instant breaking, grass sound, and their original berry drops.

Validation: all 45 required Minecraft GameTests passed with the gameplay fix; the release jar was built with Java 17 and passed the packaged-jar audit.

## 4.0.0-alpha.1 — 2026-10-11

- Initial Minecraft 1.20.1 Forge port release. The original release remains available on [GitHub Releases](https://github.com/veyndor1/Tiny-Progressions-1.20.1/releases/tag/v4.0.0-alpha.1).
