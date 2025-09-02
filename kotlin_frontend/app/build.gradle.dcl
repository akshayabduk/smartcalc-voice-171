androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.appcompat:appcompat:1.6.1")
    }
}
