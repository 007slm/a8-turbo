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

测试慢sql

SELECT count(*) FROM SA_TASK SA_Task  WHERE  ( (SA_Task.SSTATUSID = 'tesExecuting') OR (SA_Task.SSTATUSID = 'tesFinished') ) AND ( '84199D96C6CF4480B01ECD927C171876@03D29E854408485F9CE54306D276679E'  LIKE SA_Task.SEXECUTORFID) AND (SA_Task.SEXECUTORFID LIKE '84199D96C6CF4480B01ECD927C171876@03D29E854408485F9CE54306D276679E')