// Intentionally empty.
//
// Plugins are declared per module rather than in a root `plugins { ... apply false }`
// block. That keeps the root project free of any Android Gradle Plugin resolution,
// which is what allows `./gradlew :core:test` to run in environments that cannot
// reach dl.google.com. See docs/DECISIONS.md (D-002).
