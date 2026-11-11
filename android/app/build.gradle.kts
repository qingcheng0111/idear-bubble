plugins {
    id("com.android.application")
}

// 构建时把三端共用的网页核心复制成 APK 里的 assets/index.html
abstract class CopyWebAssets : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        source.get().asFile.copyTo(out.resolve("index.html"))
    }
}

val copyWebAssets = tasks.register<CopyWebAssets>("copyWebAssets") {
    source.set(rootProject.layout.projectDirectory.file("../web/idear泡泡.html"))
}

android {
    namespace = "com.idear.bubble"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.idear.bubble"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyWebAssets, CopyWebAssets::outputDir)
    }
}
