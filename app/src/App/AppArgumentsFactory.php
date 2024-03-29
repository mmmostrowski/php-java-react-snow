<?php declare(strict_types=1);

namespace TechBit\Snow\App;

use InvalidArgumentException;

final class AppArgumentsFactory
{

    public function create(array $argv, bool $isDeveloperMode, array $additional): AppArguments
    {
        array_shift($argv);

        $targetFps = 33;
        $animationDurationSec = 60 * 10; # 10 minutes

        $serverSessionId = null;
        $serverCanvasWidth = 0;
        $serverCanvasHeight = 0;
        $serverPipesDir = "";
        if ($this->isServer($argv)) {
            $serverPipesDir = $additional['pipesDir'];
            $this->read($argv);
            $serverSessionId = $this->read($argv);
            $serverCanvasWidth = (int)$this->read($argv);
            $serverCanvasHeight = (int)$this->read($argv);
            $targetFps = (int)$this->read($argv);
            $animationDurationSec = (int)$this->read($argv);

            if (!$serverSessionId || $targetFps <= 0 || $animationDurationSec <= 0 || $serverCanvasWidth <= 0 || $serverCanvasHeight <= 0) {
                throw new InvalidArgumentException("Invalid parameters. Expected: snow server [sessionid] [width] [height] [fps] [duration]");
            }
        }
        $customScene = $this->isResource($argv) ? $this->readResource($argv) : null;
        $presetName = $this->read($argv);
        $windForces = getenv('WIND') === false ? null : explode(',', getenv('WIND'));

        return new AppArguments($isDeveloperMode,
            $windForces, $presetName, $customScene,
            $targetFps, $animationDurationSec,
            $serverSessionId, $serverCanvasWidth, $serverCanvasHeight, 
            $serverPipesDir);
    }

    private function isResource(array $argv): bool
    {
        $value = $argv[0] ?? '';

        if (empty($value)) {
            return false;
        }

        return str_starts_with($value, 'base64:') || @file_exists($value) || @file_get_contents($value);
    }

    private function isServer(array $argv): bool
    {
        $value = $argv[0] ?? '';
        return $value == 'server';
    }

    private function readResource(array &$argv): string
    {
        $value = $this->read($argv);

        if (str_starts_with($value, 'base64:')) {
            return (string)@base64_decode(preg_replace('/^base64:/', '', $value));
        }

        return (string)@file_get_contents($value);
    }

    private function read(array &$argv): string
    {
        if (empty($argv)) {
            return '';
        }
        return array_shift($argv);
    }
}