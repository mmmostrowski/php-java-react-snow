<?php declare(strict_types=1);

use TechBit\Snow\App\Bootstrap;

require_once __DIR__ . '/vendor/autoload.php';
require_once __DIR__ . '/lib/Perlin.php';

$arguments = Bootstrap::createArguments($argv, __DIR__,
    isDeveloperMode: getenv("PHP_SNOW_APP_MODE") === 'develop');

$app = Bootstrap::createApp($arguments);

exit(Bootstrap::run($app, $arguments));
