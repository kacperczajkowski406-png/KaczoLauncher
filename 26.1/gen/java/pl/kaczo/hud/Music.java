package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * Własna muzyka Kaczo Client („Kaczo Theme” – oryginalny utwór wygenerowany na potrzeby klienta, bez praw autorskich).
 * Gra w menu (a opcjonalnie także w grze) i płynnie się wycisza; zastępuje wtedy standardową muzykę Minecrafta.
 * Głośność reguluje suwak „Muzyka” w ustawieniach dźwięku gry razem z suwakiem w menu moda.
 */
public final class Music {
    private Music() { }

    private static final SoundEvent EVENT = SoundEvent.fromNamespaceAndPath(Identifier.fromNamespaceAndPath("kaczohud", "menu_music"));
    private static Track cur = null;
    private static int failures = 0;
    private static long nextTry = 0;

    /** Odtwarzany utwór: zapętlony, z płynną głośnością (fade in / fade out). */
    private static final class Track extends AbstractTickableSoundInstance {
        float target = 1f;
        float gain = 0.7f;
        boolean stopping = false;
        long startedAt = 0;

        Track() {
            super(EVENT, SoundSource.MUSIC, RandomSource.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.001f;
            this.relative = true;
            this.attenuationType = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            float want = stopping ? 0f : target * gain;
            volume += (want - volume) * 0.06f;
            if (stopping && volume < 0.01f) stop();
        }
    }

    /** 0 = wyłączona, 1 = tylko w menu, 2 = w menu i w grze. */
    private static int mode() { return Modules.GENERAL.num("music"); }

    public static void tick(Minecraft mc) {
        int mode = mode();
        boolean inWorld = mc.level != null;
        boolean want = mode == 2 || (mode == 1 && !inWorld);
        long now = System.currentTimeMillis();
        boolean playing = cur != null && mc.getSoundManager().isActive(cur);
        if (want) {
            try { mc.getMusicManager().stopPlaying(); } catch (Throwable ignored) { }      // wycisz standardową muzykę
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
            if (cur.isStopped() || !playing) cur = null;
        }
    }
}
