plugins {
    java
}

group = "dev.aspid812"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.+")
}

tasks.test {
    useJUnit()
}
