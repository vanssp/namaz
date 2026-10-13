pluginManagement {
    repositories {
        // dl.google.com с части сетей отвечает 404, поэтому зеркало стоит первым.
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "Namaz"
include(":app")
