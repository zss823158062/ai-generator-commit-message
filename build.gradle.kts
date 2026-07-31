plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(
            providers.gradleProperty("platformVersion").get()
        )

        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map { it.split(',') }
        )

        // 2026.2 将 VCS 实现类(IdeaTextPatchBuilder / UnifiedDiffWriter)拆分到独立模块,
        // 需显式声明以纳入编译类路径。
        bundledModule("intellij.platform.vcs.impl")
    }

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    toolchain {
        languageVersion.set(
            JavaLanguageVersion.of(providers.gradleProperty("javaVersion").get().toInt())
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    // 本插件无可搜索设置项,禁用 buildSearchableOptions(需启动 IDE 实例,易在无 GUI 环境失败)
    buildSearchableOptions = false
}

tasks {
    wrapper {
        gradleVersion = "8.13"
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        // 2026.2 平台 class 文件为 Java 25 (bytecode major 69),需以 25 为编译目标。
        // 显式覆盖 IntelliJ 插件依据 sinceBuild 推导出的低版本 --release。
        options.release.set(25)
    }
}
