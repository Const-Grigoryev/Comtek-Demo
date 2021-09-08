plugins {
    java
    application
}

group = "dev.aspid812"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

application {
    mainClass.set("dev.aspid812.comtek_demo.Client")
}