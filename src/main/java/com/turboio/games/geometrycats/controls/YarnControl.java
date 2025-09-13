package com.turboio.games.geometrycats.controls;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.turboio.games.geometrycats.App;

public class YarnControl extends AbstractControl {
    private final int screenWidth;
    private final int screenHeight;

    private final float speed = 1100f;

    public Vector3f direction;
    private float rotation;

    public YarnControl(Vector3f direction, int screenWidth, int screenHeight) {
        this.direction = direction;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    protected void controlUpdate(float tpf) {
        spatial.move(direction.mult(speed*tpf));
        // rotation
        float actualRotation = App.getAngleFromVector(direction);

        if (actualRotation != rotation) {
            spatial.rotate(0,0,actualRotation - rotation);
            rotation = actualRotation;
        }

        // check boundaries, delete node if we hit border
        Vector3f loc = spatial.getLocalTranslation();
        if (loc.x > screenWidth ||
                loc.y > screenHeight ||
                loc.x < 0 ||
                loc.y < 0) {
            spatial.removeFromParent();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

}
