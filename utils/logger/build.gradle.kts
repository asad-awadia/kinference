group = rootProject.group
version = rootProject.version


kotlin {
    jvm()

    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib"))
            }
        }

        val jvmMain by getting {
            dependencies {
                api("org.slf4j:slf4j-api:1.7.30")
            }
        }

        val jsMain by getting {
            dependencies {
                api(npm("loglevel", "1.7.1"))
            }
        }
    }
}