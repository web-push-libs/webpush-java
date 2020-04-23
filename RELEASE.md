# Release process

0. Update CHANGELOG.md. Include changes to the source code, changes to the version of compile dependencies, etc. Do NOT include changes to the buildscript, version of test dependencies, etc.

1. Update version string in `build.gradle` (1x), `README.md` (2x) to the new (non-SNAPSHOT) version.

```
./scripts/version.sh OLD_VERSION NEW_VERSION
```

2. Commit "Release x.y.z", tag this commit with the new version "x.y.z".

```
git add README.md build.gradle
git commit -m "Release NEW_VERSION"
git tag -a NEW_VERSION -m "Version NEW_VERSION"
git push --tags
```

3. [Deploy to OSSRH with Gradle](http://central.sonatype.org/pages/gradle.html):

```
./gradlew -Prelease clean publish
```

4. [Releasing the Deployment](http://central.sonatype.org/pages/releasing-the-deployment.html):

```
./gradlew -Prelease closeAndReleaseRepository
```

5. Increment to next version and add a -SNAPSHOT suffix

```
./scripts/version.sh OLD_VERSION NEW_VERSION-SNAPSHOT
```

6. Create a commit for the new version "Set version to a.b.c-SNAPSHOT"

```
git add README.md build.gradle
git commit -m "Set version to NEW_VERSION-SNAPSHOT"
```

