#!/bin/bash
# Zrzuca nazwy klas i sygnatury z jara Minecrafta 26.x oraz Fabric API do tools/explore/out
set -x
OUT=tools/explore/out; mkdir -p $OUT
cp tools/explore/out/map.json $OUT/ 2>/dev/null || cp out/map.json $OUT/ 2>/dev/null || true
find ~/.gradle -name "*.jar" | grep -iE "minecraft|fabric-api|fabric-rendering|fabric-screen|fabric-key|fabric-lifecycle" | head -60 > $OUT/jars.txt
MCJAR=""
for j in $(grep -i minecraft $OUT/jars.txt); do
  if unzip -l $j 2>/dev/null | grep -q "net/minecraft/client/Minecraft.class"; then MCJAR=$j; break; fi
done
echo "MCJAR=$MCJAR" > $OUT/mcjar.txt
unzip -Z1 "$MCJAR" | grep '\.class$' | sed 's/\.class$//' > $OUT/classes.txt
for c in net.minecraft.client.Minecraft net.minecraft.client.gui.GuiGraphics net.minecraft.client.gui.Font net.minecraft.client.gui.screens.Screen net.minecraft.client.Options net.minecraft.client.player.LocalPlayer net.minecraft.client.Camera net.minecraft.client.KeyMapping net.minecraft.client.renderer.GameRenderer net.minecraft.client.gui.Gui net.minecraft.client.gui.components.AbstractWidget net.minecraft.client.gui.components.Button net.minecraft.world.entity.Entity net.minecraft.client.sounds.SoundManager net.minecraft.client.resources.sounds.AbstractSoundInstance net.minecraft.client.gui.screens.LoadingOverlay net.minecraft.client.gui.screens.TitleScreen net.minecraft.client.renderer.entity.EntityRenderDispatcher; do
  javap -p -cp "$MCJAR" $c > $OUT/$c.txt 2>&1
done
for j in $(grep -i fabric $OUT/jars.txt); do unzip -Z1 $j 2>/dev/null | grep -E 'net/fabricmc/fabric/api/client/(rendering|screen|keybinding|event|message)' | grep '\.class$'; done | sort -u > $OUT/fabric-classes.txt
