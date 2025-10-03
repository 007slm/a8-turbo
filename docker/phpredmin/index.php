<?php
// 设置应用程序根目录
chdir(dirname(__FILE__));

// 自动加载类函数
function __autoload($class)
{
    $path = '../';
    if (preg_match('/^(.*)_Controller$/', $class, $matches)) {
        $class = $matches[1];
        $dir   = 'controllers';
    } elseif (preg_match('/^(.*)_Model$/', $class, $matches)) {
        $class = $matches[1];
        $dir   = 'models';
    } elseif (preg_match('/^(.*)_Helper$/', $class, $matches)) {
        $class = $matches[1];
        $dir   = 'helpers';
    } else {
        $dir = 'libraries';
    }
    include_once($path.$dir.'/'.(strtolower($class)).'.php');
}

// 加载配置文件
if (file_exists('../config.php')) {
    include_once('../config.php');
} else {
    include_once('../config.dist.php');
}

// 设置时区
if (isset($config['timezone'])) {
    date_default_timezone_set($config['timezone']);
}

// 设置驱动目录路径
if (!isset($config['drivers_path'])) {
    $config['drivers_path'] = '../libraries/drivers/';
}

// 移除认证检查，始终设置为已认证
$authenticated = true;

if ($authenticated) {
    // 初始化应用程序
    $app = App::instance();
    $app->config = $config;
    $app->drivers = '../libraries/drivers/';
    
    // 初始化错误处理
    $error = new Error();
    
    // 路由请求
    Router::instance()->route();
} else {
    header('WWW-Authenticate: Basic realm="PHPRedis Administrator"');
    header('HTTP/1.0 401 Unauthorized');
    echo 'Not Authorized';
    die();
}