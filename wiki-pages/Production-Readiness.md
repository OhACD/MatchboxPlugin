# Production Readiness

This page defines the minimum bar for calling a Matchbox update production ready.

## Release Gates

A release is production ready only when all of the following are true:

- `./gradlew clean check jacocoTestReport build` passes locally and in CI
- Public API Javadocs generate without warning-heavy regressions
- GitHub Actions CI passes on the target release branch or tag
- Release artifacts are built from a version tag (`v*`)
- User-facing documentation is updated for all visible changes
- `CHANGELOG.md` reflects the shipped behavior
- At least one manual Paper server smoke test is completed on the supported runtime

## Required Validation

### Automated

- Unit tests pass
- Integration tests pass
- Stress and performance smoke tests complete without critical regressions
- JaCoCo coverage verification passes
- Javadoc build succeeds

### Manual

1. Create a session from commands and from API
2. Play through a full game round with at least two players
3. Verify chat routing, sign mode, and voting flows
4. Verify logs and statistics are populated through the API
5. Verify setup commands on a world-local map config
6. Verify legacy config import and startup migration on a test world copy
7. Verify shutdown cleanup restores players and nametags correctly

## Release Process

1. Update version and changelog
2. Run the full verification suite locally
3. Tag the release as `v<version>`
4. Let CI build, verify, and publish the release artifact
5. Publish release notes with any migration steps

## Non-Negotiables

Do not call a release production ready when any of these are unresolved:

- CI is red
- Coverage verification is failing
- Changelog or docs are stale for shipped behavior
- Public extension hooks are undocumented
- Manual smoke testing has not been completed
