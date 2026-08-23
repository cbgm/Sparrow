@file:Suppress("UnstableApiUsage")
rootProject.name = "Sparrow"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
include(":core")
include(":core:datastore")
include(":core:embedding")
include(":feature:identity")
include(":feature:contacts")
include(":feature:attachments")
include(":data:database")
include(":feature:contactimport")
include(":feature:chats")
include(":core:crypto")
include(":core:protocol")
include(":feature:messaging")
include(":feature:transport")
include(":feature:onboarding")
include(":startup")
include(":navigation")
include(":notification")
include(":core:ui")
include(":feature:settings")
include(":feature:search")
include(":feature:safety")
include(":quality:detekt-rules")
include(":resources")
include(":server:protocol")
include(":server:security")
include(":server:persistence")
include(":server:observability")
include(":server:node-registry")
include(":server:presence-directory")
include(":server:gateway")
include(":server:federation")
include(":server:mailbox")
include(":server:push")
