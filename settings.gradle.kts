// GitHub Actions 上直连官方源最稳定（阿里云镜像偶发 5xx，且 Gradle 对 5xx 不做仓库切换）；
// 本地开发优先走阿里云镜像加速。CI=true 由 GitHub Actions 自动注入。
// 注意：pluginManagement 块在脚本编译前求值，无法引用脚本顶层变量，因此各自内联判断。
pluginManagement {
    val onCi = System.getenv("CI") == "true"
    repositories {
        if (onCi) {
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    val onCi = System.getenv("CI") == "true"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (onCi) {
            google()
            mavenCentral()
        } else {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "parseVideoAndroid"
include(":app")
