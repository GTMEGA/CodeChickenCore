plugins {
    id("com.falsepattern.fpgradle-mc") version ("0.15.1")
}

group = "codechicken"

minecraft_fp {
    mod {
        modid = "CodeChickenCore"
        name = "CodeChicken Core"
        rootPkg = "$group.core"
    }
    core {
        coreModClass = "launch.CodeChickenCorePlugin"
    }
    tokens {
        tokenClass = "asm.Tags"
    }
    publish {
        maven {
            repoUrl = "https://mvn.falsepattern.com/gtmega_releases"
            repoName = "mega"
            artifact = "codechickencore-mc1.7.10"
        }
    }
}
