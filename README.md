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

lingma 登录方案 
JetBrains IDEs：打开 IDE 的设置页面，找到通义灵码设置，在未登录状态即可看到 AK/SK 登录入口，输入 AccessKey ID、AccessKey Secret 后，选择身份（未加入任何企业无需选择），然后单击登录即可；

LTAI5tGUqE79BLKAsvG7QPe5
lb9rrSzvgwAp5zZmOngvFvNseT4gff