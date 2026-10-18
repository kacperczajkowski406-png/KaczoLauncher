#!/bin/bash
# Zrzuca nazwy klas i sygnatury z jara Minecrafta 26.x oraz Fabric API do tools/explore/out
OUT=tools/explore/out; mkdir -p $OUT
rm -f $OUT/net.minecraft.*.txt $OUT/fabric.*.txt
find ~/.gradle -name "*.jar" | grep -iE "minecraft|fabric-" > $OUT/jars.txt
MCJAR=$HOME/.gradle/caches/fabric-loom/26.1/minecraft-merged.jar
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
 net.minecraft.client.renderer.RenderPipelines net.minecraft.world.food.FoodData net.minecraft.client.OptionInstance ; do
  javap -p -cp "$MCJAR" $c > $OUT/$c.txt 2>&1
done
# Fabric API: najnowsza wersja każdego modułu
for m in $(grep -oE "fabric-[a-z0-9-]+-v[0-9]+" $OUT/jars.txt | sort -u); do
  j=$(grep "/$m-[0-9]" $OUT/jars.txt | sort -V | tail -1)
  [ -n "$j" ] && echo "$j"
done > $OUT/fabric-latest-jars.txt
for j in $(cat $OUT/fabric-latest-jars.txt); do unzip -Z1 $j 2>/dev/null | grep -E 'net/fabricmc/fabric/api/client/' | grep '\.class$' | sed "s#^#$(basename $j) #"; done > $OUT/fabric-classes.txt
