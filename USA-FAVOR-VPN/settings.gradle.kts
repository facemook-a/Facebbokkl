pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories {
 google(); mavenCentral()
 providers.gradleProperty("auraSdkRepository").orNull?.let { endpoint ->
  val uri=java.net.URI(endpoint)
  require(uri.scheme=="https"&&uri.userInfo==null) { "Use the partner-authorized HTTPS repository; no credentials in URLs" }
  maven { url=uri; content { includeGroupByRegex(".*[Aa]nchor[Ff]ree.*"); includeGroupByRegex(".*[Pp]ango.*"); includeGroupByRegex(".*[Aa]ura.*") } }
 }
} }
rootProject.name = "USA-FAVOR-VPN"
include(":Admin-App", ":Client-App", ":Shared")

project(":Admin-App").projectDir=file("Admin-App/app")
project(":Client-App").projectDir=file("Client-App/app")

if (providers.gradleProperty("legacyGateway").orNull == "true") include(":OpenVPN-Core")
