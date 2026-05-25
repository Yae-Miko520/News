plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.java.zhangbinwei"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.java.zhangbinwei"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // 添加 WebView 兼容库
    implementation("androidx.webkit:webkit:1.10.0")
    //数据解析
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")
    implementation("org.jsoup:jsoup:1.16.2")
// JSON处理
    //图片加载
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //网络请求
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    //下拉刷新
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    // 核心库
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")

    // UI 库，包含 StyledPlayerView
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    // 添加ViewPager2依赖
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
}