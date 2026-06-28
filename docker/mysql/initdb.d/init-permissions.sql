-- 授予 smartcache 用户 REPLICATION CLIENT 和 REPLICATION SLAVE 权限
GRANT REPLICATION CLIENT, REPLICATION SLAVE ON *.* TO 'smartcache'@'%';

-- 授予 smartcache 用户创建和管理数据库(schema)的权限
GRANT ALL PRIVILEGES ON *.* TO 'smartcache'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;