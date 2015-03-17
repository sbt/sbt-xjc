import com.github.retronym.sbtxjc.SbtXjcPlugin

SbtXjcPlugin.xjcCommandLine += "-verbose"

SbtXjcPlugin.xjcBindings += "src/main/resources/bindings.xjb"
