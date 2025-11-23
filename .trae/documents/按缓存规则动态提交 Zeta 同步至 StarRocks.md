# 目标
根据 `ojp-cache` 的缓存规则，按需动态提交/停止 Zeta（SeaTunnel）CDC 作业，仅同步被启用缓存的库/表到 StarRocks，避免整库同步。

## 现状确认
- 静态整库提交：`docker-compose-cdc-sync-zeta.yml` 中 `seatunnel-submit` 通过 `auto-submit.sh` 提交目录下所有作业（整库 `shopdb.*` 等）。
- 已有动态能力：
  - 动态提交/取消：`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/service/SeatunnelJobService.java:53`、`:128`、`:170`、`:200`
  - 规则触发：`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/controller/CacheRuleController.java:51` 会在规则创建/更新后调用同步，并持久化 `jobId` 映射到规则 `seatunnelJobIds`（`:63`-`:70`）
  - 规则模型：`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/entity/CacheRule.java:17`（`tables`、`connHash`、`seatunnelJobIds`）
  - 作业配置项：`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/config/SeatunnelJobProperties.java:1`（`masterBaseUrl`、`submitPath`、`stopPath`、StarRocks 连接等）

## 行为定义
- 当添加/更新缓存规则且 `enabled=true`：对 `tables` 中每个表创建一个 Zeta 作业（MySQL CDC → StarRocks），并记录返回的 `jobId`。
- 当规则禁用或表从规则中移除：停止对应作业并清理 `jobId` 映射。
- 当规则的 `connHash` 变化：全部停止旧作业，按新连接重建作业。

## 具体改动
1. 停用整库静态提交
   - 取消/禁用 `seatunnel-submit` 容器或为 `auto-submit.sh` 加保护开关（如 `AUTO_SUBMIT=false` 才不执行），防止提交整库作业。
2. 启动期规则对齐（新增）
   - 新增 `ApplicationRunner` 组件（如 `CacheRuleStartupReconciler`）：
     - 启动时从存储加载所有规则，针对 `enabled` 的规则调用 `SeatunnelJobService.synchroniseRule(rule, null)` 进行对齐；
     - 捕获提交失败，带指数退避重试或记录待补偿列表。
3. 规则变更路径（沿用现有）
   - 保持 `CacheRuleController` 的同步与持久化逻辑（`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/controller/CacheRuleController.java:51`）。
4. 连接串格式统一
   - `SeatunnelJobService.parseEndpoint` 期望 `connHash` 为 `mysql://user:pass@host:port/database`；必要时在写入规则前规范化，或增强解析以支持现有格式。
5. StarRocks 表存在性（可选增强）
   - 新增 `StarRocksTableEnsurer`：在提交作业前检查目标库/表是否存在，不存在则基于 MySQL 表结构生成 `CREATE TABLE` 并通过 JDBC (`starrocks:9030`) 执行；或提供预创建脚本与约定。

## 配置与参数
- `ojp.cache.seatunnel.masterBaseUrl`：如 `http://seatunnel-master:8080`
- `ojp.cache.seatunnel.submitPath`：`/hazelcast/rest/maps/submit-job`
- `ojp.cache.seatunnel.stopPath`：`/hazelcast/rest/maps/stop-job`
- `ojp.cache.seatunnel.starrocksBaseUrl`：`jdbc:mysql://starrocks:9030`
- `ojp.cache.seatunnel.starrocksNodeUrls`：如 `starrocks:8040`
- `ojp.cache.seatunnel.starrocksUsername/password`
- `ojp.cache.seatunnel.mysqlServerIdBase`：默认 `5400`；`SeatunnelJobService.computeServerId` 保证每作业唯一（`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/service/SeatunnelJobService.java:282`-`:285`）
- 并行度与 checkpoint：`parallelism`、`checkpointInterval`（同上 `SeatunnelJobProperties`）

## API 与示例
- 创建规则：`POST /api/cache/rules`
```json
{
  "name": "shopdb-orders-cache",
  "enabled": true,
  "connHash": "mysql://user:pass@mysql:3306/shopdb",
  "tables": ["orders", "customers"]
}
```
- 效果：为 `orders` 与 `customers` 各提交一个作业，作业名形如 `ojp-cache-<rule>-shopdb-orders`（`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/service/SeatunnelJobService.java:271`-`:280`），返回 `jobId` 并写入 `seatunnelJobIds`。
- 删除规则：`DELETE /api/cache/rules/{ruleId}` 停止相关作业并清理索引（`ojp/ojp-cache/src/main/java/org/openjdbcproxy/cache/controller/CacheRuleController.java:74`）。

## 验证方案
- 提交后在 Seatunnel Master `REST` 查看作业列表并核对返回 `jobId`；
- 在 StarRocks 查询目标表：`SELECT COUNT(*) FROM shopdb.orders;` 验证快照与增量；
- 禁用规则或移除表后确认作业被停止；
- 重启应用后，启动对齐组件重新提交必要作业。

## 风险与注意
- 目标表不存在会导致作业失败：建议启用自动建表或预置 DDL；
- `connHash` 缺少凭据/库名将被拒绝（解析在 `SeatunnelJobService.parseEndpoint`）；
- Seatunnel Master REST 路径需与实际版本对应（当前使用 Hazelcast REST）。

## 交付物
- 更新部署编排：禁用静态整库提交；
- 新增启动对齐组件（`ApplicationRunner`）；
- （可选）新增 StarRocks 自动建表组件；
- 配置文件更新示例与环境变量约定。

请确认以上方案后，我将按该计划实施修改与验证。