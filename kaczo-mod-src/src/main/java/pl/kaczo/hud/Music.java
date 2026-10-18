package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * Własna muzyka Kaczo Client („Kaczo Theme” – oryginalny utwór wygenerowany na potrzeby klienta, bez praw autorskich).
 * Gra w menu (a opcjonalnie także w grze) i płynnie się wycisza; zastępuje wtedy standardową muzykę Minecrafta.
 * Głośność reguluje suwak „Muzyka” w ustawieniach dźwięku gry razem z suwakiem w menu moda.
 */
public final class Music {
    private Music() { }

    private static final SoundEvent EVENT = SoundEvent.of(Identifier.of("kaczohud", "menu_music"));
    private static Track cur = null;
    private static int failures = 0;
    private static long nextTry = 0;

    /** Odtwarzany utwór: zapętlony, z płynną głośnością (fade in / fade out). */
    private static final class Track extends MovingSoundInstance {
        float target = 1f;
        float gain = 0.7f;
        boolean stopping = false;
        long startedAt = 0;

        Track() {
            super(EVENT, SoundCategory.MUSIC, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.001f;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }

        @Override
        public void tick() {
            float want = stopping ? 0f : target * gain;
            volume += (want - volume) * 0.06f;
            if (stopping && volume < 0.01f) setDone();
        }
    }

    /** 0 = wyłączona, 1 = tylko w menu, 2 = w menu i w grze. */
    private static int mode() { return Modules.GENERAL.num("music"); }

    public static void tick(MinecraftClient mc) {
        int mode = mode();
        boolean inWorld = mc.world != null;
        boolean want = mode == 2 || (mode == 1 && !inWorld);
        long now = System.currentTimeMillis();
        boolean playing = cur != null && mc.getSoundManager().isPlaying(cur);
        if (want) {
            try { mc.getMusicTracker().stop(); } catch (Throwable ignored) { }      // wycisz standardową muzykę
            if (cur != null && cur.stopping) {                                      // wróciliśmy, zanim się wyciszył
                if (playing) cur.stopping = false; else cur = null;
            }
            if (cur != null) {
                if (playing) failures = 0;
                else if (now - cur.startedAt > 1500) cur = null;                    // przestał grać (np. stopAll) – utwórz od nowa
            }
            if (cur == null && now >= nextTry && failures <= 5) {
                cur = new Track();
                cur.startedAt = now;
                mc.getSoundManager().play(cur);
                nextTry = now + 2000;
                failures++;
            }
            if (cur != null) cur.gain = Modules.GENERAL.num("musicvol") / 100f;
        } else if (cur != null) {
            cur.stopping = true;
            if (cur.isDone() || !playing) cur = null;
        }
    }
}
