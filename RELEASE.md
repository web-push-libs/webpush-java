# Release process

1. Update version string in `build.gradle` (twice), `README.md` to the final version (i.e. non-SNAPSHOT).

```
./scripts/version.sh OLD_VERSION NEW_VERSION
```

2. Commit "Release x.y.z", tag this commit with the new version "x.y.z".

```
git commit -m "Release NEW_VERSION"
git tag -a NEW_VERSION
```

3. [Deploy to OSSRH with Gradle](http://central.sonatype.org/pages/gradle.html):

```
./gradlew -Prelease uploadArchives
```

4. [Releasing the Deployment](http://central.sonatype.org/pages/releasing-the-deployment.html):

```
./gradlew -Prelease closeAndReleaseRepository
```

5. Increment to next version with -SNAPSHOT suffix

```
./scripts/version.sh OLD_VERSION NEW_VERSION-SNAPSHOT
...
```

6. Create a commit for the new version "Set version to a.b.c-SNAPSHOT"

```
git add README.md build.gradle
git commit -m "Set version to NEW_VERSION-SNAPSHOT"
```

