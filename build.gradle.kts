plugins {
    id("com.github.ben-manes.versions") version "0.17.0"
}

allprojects {
    repositories {
        jcenter()
    }
}

task<Wrapper>("wrapper") {
    gradleVersion = "4.5.1"
    distributionType = Wrapper.DistributionType.ALL
}