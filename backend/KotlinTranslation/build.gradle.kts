import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Твои зависимости:
    implementation(files("../../StarApp/app/libs/nom-tam-fits-1.15.2.jar"))
    implementation("org.apache.commons:commons-math3:3.6.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    // Add JUnit 4 for IDE compatibility
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnitPlatform()
    reports.html.required.set(false)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.freeCompilerArgs += "-Xincremental=false"
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
    test {
        java.srcDirs("src/test/kotlin")
    }
}
