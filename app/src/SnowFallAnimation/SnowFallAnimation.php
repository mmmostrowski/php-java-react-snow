<?php declare(strict_types=1);

namespace TechBit\Snow\SnowFallAnimation;

use TechBit\Snow\SnowFallAnimation\Config\Config;
use TechBit\Snow\SnowFallAnimation\Frame\IFramePainter;
use TechBit\Snow\SnowFallAnimation\Object\AnimationObjects;
use TechBit\Snow\App\IAnimation;


final class SnowFallAnimation implements IAnimation
{

    private IFramePainter $painter;

    public function __construct(
        private readonly AnimationContext $context,
        private readonly AnimationObjects $objects,
        private readonly Config $config,
    )
    {
        $this->painter = $this->context->painter();
    }
    
    public function initialize(): void
    {
        foreach ($this->objects->allConfigurableObjects() as $object) {
            $object->onConfigChange($this->config);
        }        

        foreach ($this->objects->allObjects() as $object) {
            $object->initialize($this->context);
        }
    }

    public function play(): void
    {
        $this->painter->startAnimation();

        $visibleObjects = $this->objects->allVisibleObjects();
        $aliveObjects = $this->objects->allAliveObjects();
        $configurableObjects = $this->objects->allConfigurableObjects();

        $this->painter->startFirstFrame();
        foreach ($visibleObjects as $object) {
            $object->renderFirstFrame();
        }
        $this->painter->endFirstFrame();

        $maxFrames = $this->context->config()->animationDurationInFrames() + 1;
        while (--$maxFrames) {
            $this->painter->startFrame();

            foreach ($configurableObjects as $object) {
                $object->onConfigChange($this->config);
            }        

            foreach ($aliveObjects as $object) {
                $object->update();
            }

            foreach ($visibleObjects as $object) {
                $object->renderLoopFrame();
            }

            $this->painter->endFrame();
        }

        $this->painter->stopAnimation();

        echo "\n";
        echo "                            \n";
        echo "  Thank you for watching!   \n";
        echo "                            \n";
    }

}