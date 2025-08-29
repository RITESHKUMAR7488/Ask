pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add JitPack for MotionToast
        maven { url = uri("https://jitpack.io") }
        // Add CometChat Maven repository
        maven {
            url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat/maven/")
        }
        // Additional repository for CometChat dependencies
        maven {
            url = uri("https://maven.google.com")
        }
    }
}

rootProject.name = "Ask"
include(":app")