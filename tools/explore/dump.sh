#!/bin/bash
# Zrzuca nazwy klas i sygnatury z jara Minecrafta 26.x oraz Fabric API do tools/explore/out
OUT=tools/explore/out; mkdir -p $OUT
rm -f $OUT/net.minecraft.*.txt $OUT/fabric.*.txt
cp /tmp/cp.txt $OUT/classpath.txt
MCJAR=$(find /tmp/gh-home -name "minecraft-merged.jar" | head -1)
echo "MCJAR=$MCJAR" > $OUT/mcjar.txt
unzip -Z1 "$MCJAR" | grep '\.class$' | sed 's/\.class$//' > $OUT/classes.txt
for c in \
 net.minecraft.client.Minecraft net.minecraft.client.gui.GuiGraphicsExtractor net.minecraft.client.gui.Font net.minecraft.client.gui.screens.Screen \
 net.minecraft.client.Options net.minecraft.client.player.LocalPlayer net.minecraft.client.Camera net.minecraft.client.KeyMapping net.minecraft.client.renderer.GameRenderer \
 net.minecraft.client.gui.Gui net.minecraft.client.gui.components.AbstractWidget net.minecraft.client.gui.components.Button net.minecraft.world.entity.Entity net.minecraft.world.entity.LivingEntity \
 net.minecraft.client.sounds.SoundManager net.minecraft.client.resources.sounds.AbstractSoundInstance net.minecraft.client.resources.sounds.SoundInstance \
 net.minecraft.client.gui.screens.LoadingOverlay net.minecraft.client.gui.screens.TitleScreen net.minecraft.client.renderer.entity.EntityRenderDispatcher \
 net.minecraft.client.input.MouseButtonEvent net.minecraft.client.input.KeyEvent net.minecraft.client.input.CharacterEvent net.minecraft.client.input.InputWithModifiers \
 com.mojang.blaze3d.platform.InputConstants com.mojang.blaze3d.platform.Window com.mojang.blaze3d.platform.NativeImage \
 net.minecraft.client.renderer.texture.DynamicTexture net.minecraft.client.renderer.texture.TextureManager net.minecraft.resources.Identifier net.minecraft.util.Mth \
 net.minecraft.network.chat.Component net.minecraft.network.chat.contents.TranslatableContents net.minecraft.client.sounds.MusicManager net.minecraft.client.multiplayer.ClientPacketListener \
 net.minecraft.client.gui.components.BossHealthOverlay net.minecraft.client.gui.components.LogoRenderer net.minecraft.client.renderer.PanoramaRenderer net.minecraft.client.CameraType \
 net.minecraft.world.entity.player.Inventory net.minecraft.world.item.ItemStack net.minecraft.sounds.SoundEvent net.minecraft.sounds.SoundSource net.minecraft.client.gui.screens.DeathScreen \
 net.minecraft.world.phys.EntityHitResult net.minecraft.world.entity.player.Player net.minecraft.client.multiplayer.ClientLevel net.minecraft.client.multiplayer.ServerData \
 net.minecraft.client.resources.sounds.AbstractTickableSoundInstance net.minecraft.client.gui.screens.Overlay net.minecraft.client.gui.components.events.GuiEventListener \
 net.minecraft.client.renderer.RenderPipelines net.minecraft.world.scores.PlayerScoreEntry net.minecraft.world.scores.Scoreboard net.minecraft.world.scores.Objective net.minecraft.world.scores.PlayerTeam net.minecraft.world.scores.DisplaySlot net.minecraft.client.ClientClockManager net.minecraft.world.clock.ClockManager net.minecraft.network.chat.numbers.StyledFormat 'net.minecraft.client.KeyMapping$Category' net.minecraft.client.input.PreeditEvent net.minecraft.client.renderer.Panorama net.minecraft.client.gui.screens.Screen net.minecraft.client.gui.components.events.AbstractContainerEventHandler 'net.minecraft.client.sounds.SoundManager' 'net.minecraft.client.resources.sounds.SoundInstance$Attenuation' com.mojang.blaze3d.platform.InputConstants net.minecraft.world.food.FoodData net.minecraft.client.OptionInstance ; do
  javap -p -cp "$MCJAR" $c > $OUT/$c.txt 2>&1
done
# Fabric API: moduły są zagnieżdżone w fabric-api.jar (META-INF/jars)
mkdir -p /tmp/fapi; : > $OUT/fabric-classes.txt
for j in $(cat $OUT/classpath.txt | grep -i fabric-api); do
  unzip -qo $j 'META-INF/jars/*' -d /tmp/fapi 2>/dev/null
done
for j in /tmp/fapi/META-INF/jars/*.jar; do unzip -Z1 $j 2>/dev/null | grep -E 'net/fabricmc/fabric/api/client/' | grep '\.class$' | sed "s#^#$(basename $j) #" >> $OUT/fabric-classes.txt; done
for c in net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry net.fabricmc.fabric.api.client.screen.v1.ScreenEvents net.fabricmc.fabric.api.client.screen.v1.Screens net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement; do
  CPX=$(ls /tmp/fapi/META-INF/jars/*.jar | tr '\n' ':')
  javap -p -cp "$CPX$MCJAR" $c > $OUT/fabric.$c.txt 2>&1
done
exit 0
