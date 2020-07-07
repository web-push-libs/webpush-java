val ossrhUsername: String by project
val ossrhPassword: String by project

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("web-push")
                description.set("A Web Push library for Java.")
                url.set("https://github.com/web-push-libs/webpush-java")
                scm {
                    connection.set("scm:git:git@github.com:web-push-libs/webpush-java.git")
                    developerConnection.set("scm:git:git@github.com:web-push-libs/webpush-java.git")
                    url.set("git@github.com:web-push-libs/webpush-java.git")
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("martijndwars")
                        name.set("Martijn Dwars")
                        email.set("ikben@martijndwars.nl")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
            url = uri(getRepository())
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

fun getRepository() {
    if (version.toString().endsWith("SNAPSHOT")) {
        "https://oss.sonatype.org/content/repositories/snapshots/"
    } else {
        "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    }
}
