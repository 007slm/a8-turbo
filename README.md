切换到 wsl开发环境 为了更好的使用codex 更好的使用mcp 同时利用idea的 远程开发 保持windows环境的干
净   


curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"


sdk install java 22.0.2-zulu

sdk install mvnd

# zsh 可能缓存了 PATH，刷新一下缓存
rehash  # 或者：hash -r

mvnd --version


mvnd package install -DskipTests -Dmaven.javadoc.skip=true


mvn  

