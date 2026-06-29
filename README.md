# Arceuus Buff Overlay

RuneLite plugin that overlays timers on the Arceuus spellbook for:

- Greater Corruption
- Death Charge
- Mark of Darkness

Behavior:
- In combat: active spells show seconds remaining over their spell icons.
- In combat: expired / not-cast spells are highlighted red.
- Out of combat: no red highlight and no timer text.

This is a local-development plugin. It tracks timers based on RuneLite menu clicks and visible spell widgets.

## Run

```bash
./gradlew run
```

## Notes

- Mark of Darkness duration defaults to 180 seconds.
- If Purging staff is equipped when Mark of Darkness is started, this plugin uses the Purging Staff multiplier.
- Greater Corruption duration defaults to 18 seconds; adjust in plugin config if you want a different timing.
- Because this is local timer tracking, it may need small tuning if Jagex changes spell behavior or widget names.
