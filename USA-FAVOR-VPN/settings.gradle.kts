pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "USA-FAVOR-VPN"
include(":Admin-App", ":Client-App", ":Shared", ":OpenVPN-Core")

project(":Admin-App").projectDir=file("Admin-App/app")
project(":Client-App").projectDir=file("Client-App/app")
