package stickfight2d.controllers;

import kuusisto.tinysound.Music;
import kuusisto.tinysound.Sound;
import kuusisto.tinysound.TinySound;
import stickfight2d.enums.SoundType;
import stickfight2d.misc.Debugger;

import java.util.HashMap;

import static stickfight2d.enums.SoundType.*;

public class SoundController extends Controller {

    //TODO: add sound list with different sounds
    //TODO: add music
    private final HashMap<SoundType, Music> musicData = new HashMap<>();
    private final HashMap<SoundType, Sound> soundData = new HashMap<>();

    private static SoundController instance;

    public static SoundController getInstance() {
        if (instance == null) {
            Debugger.log("Sound Controller instantiated");
            instance = new SoundController();
        }
        return instance;
    }

    private SoundController(){
        // Music
        musicData.put(INGAME_THEME_01, TinySound.loadMusic(INGAME_THEME_01.getFile()));
        musicData.put(MAIN_MENU_THEME_01, TinySound.loadMusic(MAIN_MENU_THEME_01.getFile()));
        musicData.put(GAME_WON_THEME, TinySound.loadMusic(GAME_WON_THEME.getFile()));

        // Sound
        soundData.put(HIT_SWORD_SWORD, TinySound.loadSound(HIT_SWORD_SWORD.getFile()));
    }

    @Override
    public void update(long diffMillis) {

    }

    public Music getMusic(SoundType soundType){
        return musicData.get(soundType);
    }

    public Sound getSound(SoundType soundType){
        return soundData.get(soundType);
    }

}
