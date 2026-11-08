plugins {
    java
    application
    checkstyle
    jacoco
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.jackson.databind)
    implementation(libs.jakarta.annotation)
    implementation(libs.logback.classic)
    implementation(libs.mapstruct)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombok.mapstruct.binding)
    testAnnotationProcessor(libs.mapstruct.processor)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "org.example.finmanager.FinanceManagerApplication"
    applicationName = "finance-manager"
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("config/checkstyle/checkstyle.xml")
    configProperties = mapOf(
        "org.checkstyle.google.suppressionfilter.config" to
            file("config/checkstyle/suppressions.xml").absolutePath
    )
    maxWarnings = 0
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf("-parameters", "-Amapstruct.unmappedTargetPolicy=ERROR")
    )
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    // Сквозные тесты запускают приложение отдельным процессом с этим classpath.
    doFirst {
        systemProperty("finmanager.e2e.classpath", sourceSets.main.get().runtimeClasspath.asPath)
    }
}

// Критерий DoD: покрытие не ниже 50%; планка выше, чтобы падение покрытия замечалось сразу.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.run {
    standardInput = System.`in`
}
