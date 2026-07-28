plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "games"

include("core")
include("lobby")
include("blocksumo")
include("lazertag")
include("marathon")
include("battle")
include("minesweeper")
include("holeymoley")
include("parkourtag")
include("proxy")
include("messaging")
include("fatserver")