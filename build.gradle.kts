plugins {
    id("com.github.ben-manes.versions") version "0.17.0"
    id("java")
    id("io.spring.dependency-management") version "1.0.4.RELEASE"

    id("org.springframework.boot") version "1.5.10.RELEASE" apply false
}

val junitJupiterVersion by extra { "5.0.2" }

allprojects {
    repositories {
        jcenter()
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("io.spring.dependency-management")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:1.5.10.RELEASE")
            mavenBom("io.netty:netty-bom:4.1.21.Final")
        }
        dependencies {
            dependency("org.projectlombok:lombok:1.16.18")

            dependency("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
            dependency("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
            dependency("org.assertj:assertj-core:3.8.0")
        }
    }
}

task<Wrapper>("wrapper") {
    gradleVersion = "4.5.1"
    distributionType = Wrapper.DistributionType.ALL
}