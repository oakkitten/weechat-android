import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.7")

    // "api" because we are calling `SSHConnection.getKnownHosts` from the app
    // and it returns something from inside sshlib
    api("com.github.connectbot:sshlib:2.2.21")

    implementation("com.neovisionaries:nv-websocket-client:2.14")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions { jvmTarget = "11" }
}
