package com.turboio.games.geometrycats;

import java.util.Random;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.turboio.games.geometrycats.controls.PlayerControl;
import com.turboio.games.geometrycats.controls.SeekerControl;
import com.turboio.games.geometrycats.controls.YarnControl;

public class App extends SimpleApplication implements ActionListener, AnalogListener {
    Sound sound;
    Spatial player;
    Node yarnNode;
    BitmapText yarnCounterText;
    long bulletCooldown = 0;
    float reload = 0;
    long yarnCount = 2;

    BitmapText killCounterText;
    long killCount = 0;

    private long enemySpawnCooldown;
    private float enemySpawnChance = 100;
    private Node enemyNode;

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 800);
        settings.setTitle("Geometry Cats");
        App app = new App();
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        sound = new Sound(assetManager);

        // setup camera for 2D games
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0, 0, 0.5f));
        getFlyByCamera().setEnabled(false);

        // turn off stats view (you can leave it on, if you want)
        setDisplayStatView(false);
        setDisplayFps(true);

        // Create a Picture for the background
        Picture background = new Picture("Background");
        background.setImage(assetManager, "Textures/background.png", true);
        background.setWidth(settings.getWidth());
        background.setHeight(settings.getHeight());
        background.setPosition(0, 0);
        guiNode.attachChild(background);

        // add a player to the main scene (gui)
        player = getSpatial("player");
        player.setUserData("alive", true);
        player.move((float) settings.getWidth() /2, (float) settings.getHeight() /2, 0);
        guiNode.attachChild(player);

        // add a yarn node for attaching yarn 'bullets' and add to main scene
        yarnNode = new Node("yarn");
        guiNode.attachChild(yarnNode);

        // add key listeners
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "left");
        inputManager.addListener(this, "right");
        inputManager.addListener(this, "up");
        inputManager.addListener(this, "down");
        inputManager.addListener(this, "return");

        // add mouse listener
        inputManager.addMapping("mousePick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "mousePick");

        // add control to the player
        player.addControl(new PlayerControl(settings.getWidth(), settings.getHeight()));

        // add yarn counter
        BitmapFont f = assetManager.loadFont("Interface/Fonts/Default.fnt");
        yarnCounterText = new BitmapText(f, false);
        yarnCounterText.setColor(ColorRGBA.LightGray);
        yarnCounterText.setText("Yarn: 0");
        yarnCounterText.setSize(24);
        yarnCounterText.setLocalTranslation(
                (settings.getWidth() - yarnCounterText.getLineWidth()) / 2f,
                settings.getHeight() - yarnCounterText.getLineHeight(),
                0);
        guiNode.attachChild(yarnCounterText);

        // add kill counter
        killCounterText = new BitmapText(f, false);
        killCounterText.setColor(ColorRGBA.LightGray);
        killCounterText.setText("Kills: 0");
        killCounterText.setSize(24);
        killCounterText.setLocalTranslation(
                (settings.getWidth() - killCounterText.getLineWidth()) / 2f,
                settings.getHeight() - killCounterText.getLineHeight() * 2,
                0);
        guiNode.attachChild(killCounterText);

        enemyNode = new Node("enemies");
        guiNode.attachChild(enemyNode);


    }

    @Override
    public void simpleUpdate(float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            spawnEnemies();
            handleCollisions();
            handleReload(tpf);
            killCounterText.setText("Kills: " + killCount);
        } else if (System.currentTimeMillis() - (Long) player.getUserData("dieTime") > 4000f) {
            // spawn player
            player.setLocalTranslation(500,500,0);
            guiNode.attachChild(player);
            yarnCount = 0;
            killCount = 0;
            enemySpawnChance = 100;
            player.setUserData("alive",true);
        }


    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            if (name.equals("up")) {
                player.getControl(PlayerControl.class).up = isPressed;
            } else if (name.equals("down")) {
                player.getControl(PlayerControl.class).down = isPressed;
            } else if (name.equals("left")) {
                player.getControl(PlayerControl.class).left = isPressed;
            } else if (name.equals("right")) {
                player.getControl(PlayerControl.class).right = isPressed;
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            if (name.equals("mousePick")) {
                //shoot yarn
                if (yarnCount <= 0) {
                    return;
                }
                if (System.currentTimeMillis() - bulletCooldown > 100f) {
                    bulletCooldown = System.currentTimeMillis();
                    Vector3f aim = getAimDirection();
                    Vector3f offset = new Vector3f(aim.y/3,-aim.x/3,0);

                    // initialize yarn
                    Spatial bullet = getSpatial("yarn");
                    Vector3f finalOffset = aim.add(offset).mult(30);
                    Vector3f trans = player.getLocalTranslation().add(finalOffset);
                    bullet.setLocalTranslation(trans);
                    bullet.addControl(new YarnControl(aim, settings.getWidth(), settings.getHeight()));
                    yarnNode.attachChild(bullet);
                    sound.shoot();
                    yarnCount--;
                }
            }
        }
    }


    /**
     * getSpatial creates a node, adds texture and material, sets radius and returns it
     */
    private Spatial getSpatial(String name) {
        Node node = new Node(name);

        // load picture, note the convention of name matching png filename
        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/" + name + ".png");
        pic.setTexture(assetManager, tex, true);

        // adjust picture
        float width = tex.getImage().getWidth();
        float height = tex.getImage().getHeight();
        pic.setWidth(width);
        pic.setHeight(height);
        pic.move(-width / 2f, -height / 2f, 0);

        // add a material to the picture
        Material picMat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
        picMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        node.setMaterial(picMat);

        // set the radius of the spatial
        // (using width only as a simple approximation)
        node.setUserData("radius", width / 2);

        // attach the picture to the node and return it
        node.attachChild(pic);
        return node;
    }

    /**
     * getAimDirection calculates a vector based on player and mouse coordinates
     */
    private Vector3f getAimDirection() {
        Vector2f mouse = inputManager.getCursorPosition();
        Vector3f playerPos = player.getLocalTranslation();
        Vector3f dif = new Vector3f(mouse.x-playerPos.x,mouse.y-playerPos.y,0);
        return dif.normalizeLocal();
    }

    public static float getAngleFromVector(Vector3f vec) {
        Vector2f vec2 = new Vector2f(vec.x,vec.y);
        return vec2.getAngle();
    }

    public static Vector3f getVectorFromAngle(float angle) {
        return new Vector3f(FastMath.cos(angle),FastMath.sin(angle),0);
    }

    private void spawnEnemies() {
        if (System.currentTimeMillis() - enemySpawnCooldown >= 17) {
            enemySpawnCooldown = System.currentTimeMillis();

            if (enemyNode.getQuantity() < 50) {
                if (new Random().nextInt((int) enemySpawnChance) == 0) {
                    createSeeker();
                }
            }

            //increase Spawn Time
            if (enemySpawnChance >= 1.1f) {
                enemySpawnChance -= 0.005f;
            }
        }
    }

    private void createSeeker() {
        Spatial seeker = getSpatial("seeker");
        seeker.setLocalTranslation(getSpawnPosition((float)seeker.getUserData("radius")));
        seeker.addControl(new SeekerControl(player));
        seeker.setUserData("active",false);
        sound.spawn();
        enemyNode.attachChild(seeker);
    }

    private Vector3f getSpawnPosition(float radius) {
        Vector3f pos;
        do {
            pos = new Vector3f(new Random().nextInt(settings.getWidth()), new Random().nextInt((int) (settings.getHeight()-200-radius)),0);
        } while (pos.distanceSquared(player.getLocalTranslation()) < 8000);
        return pos;
    }

    private void handleReload(float tpf) {
        // reload yarn every 1s
        reload += tpf;
        if (reload > 1) {
            reload = 0;
            yarnCount += 2;
        }

        yarnCounterText.setText("Yarn: " + yarnCount);
    }

    private void handleCollisions() {
        // should the player die?
        for (int i=0; i<enemyNode.getQuantity(); i++) {
            if ((Boolean) enemyNode.getChild(i).getUserData("active")) {
                if (checkCollision(player,enemyNode.getChild(i))) {
                    killPlayer();
                }
            }
        }

        //should an enemy die?
        int i=0;
        while (i < enemyNode.getQuantity()) {
            int j=0;
            while (j < yarnNode.getQuantity()) {
                if (checkCollision(enemyNode.getChild(i),yarnNode.getChild(j))) {
                    enemyNode.detachChildAt(i);
                    yarnNode.detachChildAt(j);
                    killCount++;
                    sound.hit();
                    break;
                }
                j++;
            }
            i++;
        }
    }

    private boolean checkCollision(Spatial a, Spatial b) {
        float distance = a.getLocalTranslation().distance(b.getLocalTranslation());
        float maxDistance =  (Float)a.getUserData("radius") + (Float)b.getUserData("radius");
        return distance <=maxDistance;
    }

    private void killPlayer() {
        player.removeFromParent();
        player.getControl(PlayerControl.class).reset();
        player.setUserData("alive", false);
        player.setUserData("dieTime", System.currentTimeMillis());
        sound.death();
        enemyNode.detachAllChildren();
    }

}
