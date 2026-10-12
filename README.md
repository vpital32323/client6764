# Nova Client (Fabric 1.21.1)

Right Shift - open the glass ClickGUI (blur + transparency + animations).
GUI: LMB on a card = toggle, RMB = settings (sliders, switches, Bind), mouse wheel = scroll.
Bind: open settings, click "Bind", press a key (Backspace = clear, Esc = cancel).

Colours of the whole client: src/main/java/com/nova/util/Theme.java (A1, A2).

Modules: Sprint, Fly, Fullbright, ESP, ChinaHat, HUD.

## Build
- PC: install JDK 21, open the folder in IntelliJ IDEA, run Gradle task build. Jar: build/libs/
- Phone: push to GitHub, the Actions workflow builds the jar (Artifacts tab).

## Install
Put the jar from build/libs (not *-sources.jar) and Fabric API into .minecraft/mods with Fabric Loader for 1.21.1.
