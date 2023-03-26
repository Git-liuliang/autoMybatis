plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.8.0"
}

group = "com.automybatis"
version = "1.2-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("com.alibaba:fastjson:2.0.26");
    implementation("org.freemarker:freemarker:2.3.31");
    implementation ("org.projectlombok:lombok:1.18.22");
    annotationProcessor ("org.projectlombok:lombok:1.18.22");


}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("223.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
