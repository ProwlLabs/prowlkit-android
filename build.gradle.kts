import com.android.build.gradle.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val publishProperties = Properties().apply {
    val publishFile = file("publish.properties")
    if (publishFile.exists()) {
        publishFile.inputStream().use { load(it) }
    }
}

fun publishProperty(key: String): String? =
    publishProperties.getProperty(key)
        ?: findProperty(key) as String?
        ?: System.getenv(key)

val publishGroupId: String = publishProperty("GROUP") ?: "io.github.prowllabs"
val publishVersion: String = publishProperty("VERSION") ?: "0.0.0-SNAPSHOT"

val libraryPublishConfig = mapOf(
    "prowl-core" to Triple(
        "prowl-core",
        "ProwlKit Core",
        "OkHttp interceptor, storage, mocking, and masking for ProwlKit Android.",
    ),
    "prowl-ui" to Triple(
        "prowl-ui",
        "ProwlKit UI",
        "Jetpack Compose network inspector UI for ProwlKit Android.",
    ),
    "prowl" to Triple(
        "prowl",
        "ProwlKit",
        "Android network debugger with mocking — Chucker-style inspector with ProwlKit feature parity.",
    ),
    "prowl-grpc" to Triple(
        "prowl-grpc",
        "ProwlKit gRPC",
        "gRPC ClientInterceptor for ProwlKit Android network inspection.",
    ),
)

subprojects {
    val publishConfig = libraryPublishConfig[name] ?: return@subprojects

    plugins.withId("com.android.library") {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            val (artifactId, artifactName, artifactDescription) = publishConfig

            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        groupId = publishGroupId
                        this.artifactId = artifactId
                        version = publishVersion
                        from(components["release"])

                        pom {
                            name.set(artifactName)
                            description.set(artifactDescription)
                            url.set(publishProperty("POM_URL"))

                            licenses {
                                license {
                                    name.set(publishProperty("POM_LICENSE_NAME"))
                                    url.set(publishProperty("POM_LICENSE_URL"))
                                }
                            }

                            developers {
                                developer {
                                    id.set(publishProperty("POM_DEVELOPER_ID"))
                                    name.set(publishProperty("POM_DEVELOPER_NAME"))
                                }
                            }

                            scm {
                                url.set(publishProperty("POM_SCM_URL"))
                                connection.set(publishProperty("POM_SCM_CONNECTION"))
                                developerConnection.set(publishProperty("POM_SCM_DEV_CONNECTION"))
                            }
                        }
                    }
                }

                repositories {
                    mavenLocal()

                    val githubActor = publishProperty("GITHUB_ACTOR") ?: publishProperty("gpr.user")
                    val githubToken = publishProperty("GITHUB_TOKEN") ?: publishProperty("gpr.key")
                    if (!githubActor.isNullOrBlank() && !githubToken.isNullOrBlank()) {
                        maven {
                            name = "GitHubPackages"
                            url = uri("https://maven.pkg.github.com/ProwlLabs/prowlkit-android")
                            credentials {
                                username = githubActor
                                password = githubToken
                            }
                        }
                    }

                    val mavenCentralUsername = publishProperty("MAVEN_CENTRAL_USERNAME")
                        ?: publishProperty("ossrhUsername")
                    val mavenCentralPassword = publishProperty("MAVEN_CENTRAL_PASSWORD")
                        ?: publishProperty("ossrhPassword")
                    if (!mavenCentralUsername.isNullOrBlank() && !mavenCentralPassword.isNullOrBlank()) {
                        maven {
                            name = "MavenCentral"
                            val isSnapshot = publishVersion.endsWith("SNAPSHOT")
                            val releasesUrl = publishProperty("MAVEN_CENTRAL_RELEASES_URL")
                                ?: "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                            val snapshotsUrl = publishProperty("MAVEN_CENTRAL_SNAPSHOTS_URL")
                                ?: "https://central.sonatype.com/repository/maven-snapshots/"
                            url = uri(if (isSnapshot) snapshotsUrl else releasesUrl)
                            credentials {
                                username = mavenCentralUsername
                                password = mavenCentralPassword
                            }
                        }
                    }
                }
            }

            extensions.configure<SigningExtension> {
                val signingKey = publishProperty("SIGNING_KEY")
                val signingPassword = publishProperty("SIGNING_PASSWORD")
                if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                    sign(extensions.getByType<PublishingExtension>().publications["release"])
                }
            }
        }
    }
}

tasks.register("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publishes prowl-core, prowl-ui, and prowl to the local Maven repository."
    dependsOn(
        ":prowl-core:publishReleasePublicationToMavenLocal",
        ":prowl-ui:publishReleasePublicationToMavenLocal",
        ":prowl:publishReleasePublicationToMavenLocal",
        ":prowl-grpc:publishReleasePublicationToMavenLocal",
    )
}

tasks.register("publishAllToGitHubPackages") {
    group = "publishing"
    description = "Publishes all library modules to GitHub Packages."
    doFirst {
        val actor = publishProperty("GITHUB_ACTOR") ?: publishProperty("gpr.user")
        val token = publishProperty("GITHUB_TOKEN") ?: publishProperty("gpr.key")
        require(!actor.isNullOrBlank() && !token.isNullOrBlank()) {
            """
            GitHub Packages credentials missing.
            Add to publish.properties (or ~/.gradle/gradle.properties):
              GITHUB_ACTOR=your-github-username
              GITHUB_TOKEN=ghp_...   # classic PAT with write:packages (+ repo if private)

            Or publish from GitHub Actions: Actions → Publish → Run workflow (target: github).
            """.trimIndent()
        }
    }
    dependsOn(
        ":prowl-core:publishReleasePublicationToGitHubPackagesRepository",
        ":prowl-ui:publishReleasePublicationToGitHubPackagesRepository",
        ":prowl:publishReleasePublicationToGitHubPackagesRepository",
        ":prowl-grpc:publishReleasePublicationToGitHubPackagesRepository",
    )
}

tasks.register("publishAllToMavenCentral") {
    group = "publishing"
    description = "Publishes all library modules to Maven Central (Sonatype)."
    dependsOn(
        ":prowl-core:publishReleasePublicationToMavenCentralRepository",
        ":prowl-ui:publishReleasePublicationToMavenCentralRepository",
        ":prowl:publishReleasePublicationToMavenCentralRepository",
        ":prowl-grpc:publishReleasePublicationToMavenCentralRepository",
    )
}

tasks.register<Exec>("finalizeMavenCentralDeployment") {
    group = "publishing"
    description =
        "Moves a maven-publish deployment from the OSSRH Staging API buffer to the Central Portal."
    workingDir = rootProject.projectDir
    commandLine("bash", "scripts/finalize-maven-central.sh")
}

tasks.register("publishAndReleaseMavenCentral") {
    group = "publishing"
    description = "Publishes all modules to Maven Central and finalizes the Portal deployment."
    dependsOn("publishAllToMavenCentral", "finalizeMavenCentralDeployment")
    tasks.named("finalizeMavenCentralDeployment").configure { mustRunAfter("publishAllToMavenCentral") }
}
