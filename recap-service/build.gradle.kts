plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "kr.mafoo"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.springframework.cloud:spring-cloud-function-adapter-aws:3.2.8")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    implementation ("com.amazonaws:aws-lambda-java-core:1.2.2")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("io.awspring.cloud:spring-cloud-starter-aws:2.4.4")
    implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")
}

tasks.register<Zip>("buildZip") {
    from(tasks.compileJava)
    from(tasks.processResources)

    into("lib") {
        from(configurations.runtimeClasspath)
    }
}

