<?php
$config = array(
    'default_controller' => 'Welcome',
    'default_action'     => 'Index',
    'production'         => false,
    'default_layout'     => 'layout',
    'timezone'           => 'Europe/Amsterdam',
    'auth' => array(
    ),
    'log' => array(
        'driver'    => 'std',
        'threshold' => 0, /* 禁用日志记录以避免权限问题 */
    ),
    'database'  => array(
        'driver' => 'redis',
        'mysql'  => array(
            'host'     => 'localhost',
            'username' => 'root',
            'password' => 'root'
        ),
        'redis' => array(
            array(
                'host'     => getenv('REDIS_HOST') ?: 'localhost',
                'port'     => getenv('REDIS_PORT') ?: 6379,
                'password' => getenv('REDIS_PASSWORD') ?: null,
                'database' => getenv('REDIS_DATABASE') ?: 0,
                'max_databases' => 16,
                'stats'    => array(
                    'enable'   => 1,
                    'database' => 0,
                ),
                'dbNames' => array(),
            ),
        ),
    ),
    'session' => array(
        'lifetime'       => 7200,
        'gc_probability' => 2,
        'name'           => 'phpredminsession'
    ),
    'gearman' => array(
        'host' => '127.0.0.1',
        'port' => 4730
    ),
    'terminal' => array(
        'enable'  => true,
        'history' => 200
    )
);