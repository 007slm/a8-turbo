# 第一阶段：编译 eBPF C 代码和 Go 程序
FROM golang:1.22-bookworm AS builder

# 安装编译工具
RUN apt-get update && apt-get install -y \
    clang \
    llvm \
    libbpf-dev \
    linux-headers-generic \
    git \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# 复制 Go 模块文件
COPY go.mod go.sum* ./
RUN go mod download

# 复制源代码
COPY . .

# 编译 eBPF C 代码
RUN clang -O2 -g -target bpf -D__TARGET_ARCH_x86 \
    -I/usr/include/x86_64-linux-gnu \
    -c internal/bpf/sentinel.bpf.c \
    -o sentinel.bpf.o

# 编译 Go 程序
RUN CGO_ENABLED=0 go build -mod=mod -o sentinel ./cmd/sentinel

# 第二阶段：运行时镜像
FROM debian:bookworm-slim

# 安装运行时依赖
RUN apt-get update && apt-get install -y \
    iproute2 \
    util-linux \
    ca-certificates \
    docker.io \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 从构建阶段复制编译产物
COPY --from=builder /build/sentinel /app/sentinel
COPY --from=builder /build/sentinel.bpf.o /app/sentinel.bpf.o

# 设置权限
RUN chmod +x /app/sentinel

# 创建授权文件目录
RUN mkdir -p /etc/ojp/license

ENTRYPOINT ["/app/sentinel"]
