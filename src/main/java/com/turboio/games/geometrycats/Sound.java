package com.turboio.games.geometrycats;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;

public class Sound {
    private AudioNode music;
    private AudioNode shot;
    private AudioNode spawn;
    private AudioNode hit;
    private AudioNode death;

    private AssetManager assetManager;

    public Sound(AssetManager assetManager) {
        this.assetManager = assetManager;
        loadSounds();
    }

    private void loadSounds() {
//        music = new AudioNode(assetManager,"Sounds/Music.ogg");
//        music.setPositional(false);
//        music.setReverbEnabled(false);
//        music.setLooping(true);

        shot = new AudioNode(assetManager,"Sounds/Yarnshot.wav");
        shot.setPositional(false);
        shot.setLooping(false);
        shot.setReverbEnabled(false);

        hit  = new AudioNode(assetManager,"Sounds/Zombiehit.wav");
        hit.setPositional(false);
        hit.setLooping(false);
        hit.setReverbEnabled(false);

        death = new AudioNode(assetManager,"Sounds/Death.wav");
        death.setPositional(false);
        death.setLooping(false);
        death.setReverbEnabled(false);

        spawn = new AudioNode(assetManager,"Sounds/Zombiecat.wav");
        spawn.setPositional(false);
        spawn.setLooping(false);
        spawn.setReverbEnabled(false);
    }

    public void startMusic() {
        music.play();
    }

    public void shoot() {
        shot.playInstance();
    }

    public void hit() {
       hit.playInstance();
    }

    public void spawn() {
        spawn.playInstance();
    }

    public void death() {
        death.playInstance();
    }
}

