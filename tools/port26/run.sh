#!/bin/bash
# Buduje mod dla Minecrafta 26.x: generuje kod z nazw yarn na Mojang i poprawia go pętlą „kompiluj -> napraw”.
# Uruchamiane z katalogu głównego repozytorium. Wymaga: tools/explore/out/map.json (z mappings.py).
set -u
MAP=tools/explore/out/map.json
cd kaczo-mod-26
mkdir -p gen/java gen/resources
echo "// puste" > gen/java/Placeholder.java; sed -i 's#^// puste#package placeholder; class Placeholder {}#' gen/java/Placeholder.java
cp -r ../kaczo-mod-src/src/main/resources/* gen/resources/
# najpierw pobierz jar Minecrafta (kompilacja pustego projektu)
gradle compileJava --no-daemon -q -Pminecraft_version=$MC -Pfabric_version=$FAPI -Ploom_version=$LOOM > /tmp/prep.log 2>&1 || { echo "::error::przygotowanie projektu nie powiodło się: $(tail -c 700 /tmp/prep.log | tr '\n' '|')"; exit 1; }
MCJAR=$(find $GRADLE_USER_HOME -name "minecraft-merged.jar" | head -1)
echo "MCJAR=$MCJAR"
unzip -Z1 "$MCJAR" | grep '\.class$' | sed 's/\.class$//' > /tmp/classes.txt
cd ..
python3 tools/port26/migrate26.py gen $MAP /tmp/classes.txt kaczo-mod-src/src/main/java kaczo-mod-26/gen/java kaczo-mod-26/overlay/java | tee /tmp/gen.log
cd kaczo-mod-26
ok=0
for i in $(seq 1 30); do
  gradle compileJava --no-daemon -Pminecraft_version=$MC -Pfabric_version=$FAPI -Ploom_version=$LOOM > /tmp/build.log 2>&1 && { ok=1; break; }
  python3 ../tools/port26/migrate26.py fix ../$MAP gen/java /tmp/build.log "$MCJAR" | tee /tmp/fix.log
  [ ${PIPESTATUS[0]} -eq 3 ] && break
done
if [ $ok -eq 1 ]; then
  gradle build --no-daemon -Pminecraft_version=$MC -Pfabric_version=$FAPI -Ploom_version=$LOOM > /tmp/final.log 2>&1 && { echo "BUILD OK"; exit 0; }
fi
# zgłoś błędy jako adnotacje
{ cat /tmp/gen.log; grep -E "error:|symbol:|location:" /tmp/build.log | sed -E 's#/home/runner/work/[^/]+/[^/]+/##' | awk '!s[$0]++' | head -70; grep -E "NIEROZWI|poprawek" /tmp/fix.log | head -40; } > /tmp/report.txt
echo "::error::$(head -c 6000 /tmp/report.txt | tr '\n' '|')"
exit 1
