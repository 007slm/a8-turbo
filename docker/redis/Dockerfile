FROM redis/redis-stack-server

RUN apt update
RUN apt-get -y install wget
RUN wget https://github.com/redis-field-engineering/redis-smart-cache-cli/releases/download/v0.0.13/redis-smart-cache-cli_Linux_x86_64.tar.gz
RUN tar -xzvf redis-smart-cache-cli_Linux_x86_64.tar.gz
RUN cp smart-cache-cli /usr/local/bin
RUN rm redis-smart-cache-cli_Linux_x86_64.tar.gz
# 将自定义的 redis.conf 复制到容器默认配置目录
COPY redis.conf /usr/local/etc/redis/redis.conf

# 使用自定义配置启动 Redis
CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
