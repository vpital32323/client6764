package com.nova.gui;

import com.nova.Nova;
import com.nova.module.Category;
import com.nova.module.Module;
import com.nova.render.Render2D;
import com.nova.setting.BooleanSetting;
import com.nova.setting.NumberSetting;
import com.nova.setting.Setting;
import com.nova.util.Anim;
import com.nova.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Glass-style ClickGUI: blurred background, translucent rounded window, sidebar with categories,
 * module cards with switches, sliders and key binding.
 *
 * Controls: LMB on a card = toggle module, RMB = open settings (sliders, switches, bind).
 * Mouse wheel = scroll. Click "Bind" row, then press a key (Backspace = clear, Esc = cancel).
 */
public class ClickGui extends Screen {
    // virtual window size (everything is laid out in these units and scaled to the screen)
    private static final float W = 420, H = 270;
    private static final float SB_X = 8, SB_W = 80;
    private static final float CT_X = 96, CT_R = 8, CT_TOP = 36, CT_BOT = H - 8;
    private static final float GAP = 8, CARD_H = 24;
    private static final float BOOL_H = 18, NUM_H = 28, BIND_H = 18;
    private static final int MAX_BLUR = 7;

    private enum Kind { CAT, CARD, BOOL, SLIDER, BIND }
    private record Hit(Kind k, float x, float y, float w, float h, Module m, Setting s, Category c) {}

    private static class State {
        final Anim on, hover = new Anim(0), exp = new Anim(0);
        State(boolean enabled) { on = new Anim(enabled ? 1 : 0); }
    }

    private final List<Category> cats = new ArrayList<>();
    private final Map<Module, State> states = new HashMap<>();
    private final Map<Category, Anim> catHover = new HashMap<>();
    private final Map<Setting, Anim> switchAnim = new HashMap<>();
    private final Map<Setting, Anim> sliderAnim = new HashMap<>();
    private final Set<Module> expanded = new HashSet<>();
    private final List<Hit> hits = new ArrayList<>();

    private final Anim open = new Anim(0), tab = new Anim(0), selY = new Anim(0), scroll = new Anim(0);
    private boolean closing;
    private Category cat;
    private float scrollTarget, maxScroll;
    private Module binding;
    private NumberSetting dragging;
    private float dragX, dragW;
    private float ox, oy, gs;       // window origin and scale of the last frame
    private double tabAlpha = 1;
    private final int origBlur;

    public ClickGui() {
        super(Text.literal("ClickGUI"));
        for (Category c : Category.values()) if (!Nova.modules.byCategory(c).isEmpty()) cats.add(c);
        cat = cats.get(0);
        origBlur = MinecraftClient.getInstance().options.getMenuBackgroundBlurriness().getValue();
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() { closing = true; } // really closes after the fade-out

    @Override
    public void removed() {
        client.options.getMenuBackgroundBlurriness().setValue(origBlur);
        super.removed();
    }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        open.update(closing ? 0 : 1, 11);
        if (closing && open.value < 0.03) { super.close(); return; }
        double e = Anim.ease(open.value);

        // animated background blur (vanilla blur, strength follows the open animation)
        client.options.getMenuBackgroundBlurriness().setValue((int) Math.round(MAX_BLUR * e));
        client.gameRenderer.renderBlur(delta);
        ctx.fill(0, 0, width, height, Theme.argb(0x000000, (int) (70 * e)));

        float bs = Math.min(1.5f, Math.min(width / (W + 30f), height / (H + 30f)));
        gs = (float) (bs * (0.92 + 0.08 * e));
        ox = (width - W * gs) / 2f;
        oy = (height - H * gs) / 2f + (float) ((1 - e) * 10);
        float lmx = (mx - ox) / gs, lmy = (my - oy) / gs;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(ox, oy, 0f);
        ctx.getMatrices().scale(gs, gs, 1f);
        drawWindow(ctx, lmx, lmy, e);
        ctx.getMatrices().pop();
        ctx.draw();
    }

    private void drawWindow(DrawContext ctx, float lmx, float lmy, double ga) {
        hits.clear();

        // window
        Render2D.shadow(ctx, 0, 0, W, H, 10, 0x000000, (int) (120 * ga));
        Render2D.rectV(ctx, 0, 0, W, H, 10, a(0x181824, 150 * ga), a(0x0A0A12, 175 * ga));
        Render2D.outline(ctx, 0, 0, W, H, 10, 1f, a(0xFFFFFF, 45 * ga), a(0xFFFFFF, 12 * ga));

        // sidebar
        Render2D.rect(ctx, SB_X, 8, SB_W, H - 16, 8, a(0x000000, 70 * ga));
        gradText(ctx, "Nova", SB_X + 12, 17, 1.7f, ga);
        txt(ctx, "client", SB_X + 13, 35, 0xFF7C7C90, ga);

        float catTop = 54;
        selY.update(cats.indexOf(cat) * 24, 14);
        float py = catTop + (float) selY.value;
        Render2D.rectH(ctx, SB_X + 6, py, SB_W - 12, 20, 6, a(Theme.A1, 80 * ga), a(Theme.A2, 80 * ga));
        Render2D.rect(ctx, SB_X + 6, py + 5, 2, 10, 1, a(0xFFFFFF, 230 * ga));
        for (int i = 0; i < cats.size(); i++) {
            Category c = cats.get(i);
            float cy = catTop + i * 24;
            boolean over = !closing && lmx >= SB_X + 6 && lmx < SB_X + SB_W - 6 && lmy >= cy && lmy < cy + 20;
            Anim hv = catHover.computeIfAbsent(c, k -> new Anim(0));
            hv.update(over ? 1 : 0, 16);
            double t = c == cat ? 1 : hv.value * 0.6;
            txt(ctx, c.label, SB_X + 17, cy + 6, Theme.lerp(0xFF8A8A9C, 0xFFFFFFFF, t), ga);
            hits.add(new Hit(Kind.CAT, SB_X + 6, cy, SB_W - 12, 20, null, null, c));
        }

        // header
        List<Module> mods = Nova.modules.byCategory(cat);
        scaledTxt(ctx, cat.label, CT_X + 2, 11, 1.3f, 0xFFFFFFFF, ga);
        String count = mods.size() + (mods.size() == 1 ? " module" : " modules");
        txt(ctx, count, W - CT_R - textRenderer.getWidth(count) - 2, 15, 0xFF7C7C90, ga);
        Render2D.rectH(ctx, CT_X, 31, W - CT_R - CT_X, 1, 0.5f, a(Theme.A1, 150 * ga), a(Theme.A2, 10 * ga));

        // cards
        tab.update(1, 9);
        tabAlpha = Anim.ease(tab.value);
        scroll.update(scrollTarget, 14);
        float start = CT_TOP - (float) scroll.value;
        float[] colY = {start, start};
        float cw = (W - CT_R - CT_X - GAP) / 2f;

        scissor(ctx, CT_X - 2, CT_TOP, W - CT_R + 2, CT_BOT);
        for (Module m : mods) {
            int col = colY[0] <= colY[1] ? 0 : 1;
            float cx = CT_X + col * (cw + GAP);
            float cy = colY[col] + (float) ((1 - tabAlpha) * 8);
            float ch = drawCard(ctx, m, cx, cy, cw, lmx, lmy, ga);
            colY[col] += ch + GAP;
        }
        ctx.disableScissor();

        float total = Math.max(colY[0], colY[1]) - start;
        maxScroll = Math.max(0, total - GAP - (CT_BOT - CT_TOP));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

        // scroll bar
        if (maxScroll > 1) {
            float trackH = CT_BOT - CT_TOP;
            float barH = Math.max(18, trackH * trackH / (trackH + maxScroll));
            float barY = CT_TOP + (trackH - barH) * (float) (scroll.value / maxScroll);
            Render2D.rect(ctx, W - 5, barY, 2, barH, 1, a(0xFFFFFF, 70 * ga));
        }
    }

    private float drawCard(DrawContext ctx, Module m, float x, float y, float w, float lmx, float lmy, double ga) {
        State st = states.computeIfAbsent(m, k -> new State(k.isEnabled()));
        boolean over = !closing && lmx >= x && lmx < x + w && lmy >= y && lmy < y + CARD_H
                && lmy >= CT_TOP && lmy <= CT_BOT;
        double on = st.on.update(m.isEnabled() ? 1 : 0, 12);
        double hv = st.hover.update(over ? 1 : 0, 16);
        boolean isExp = expanded.contains(m);
        double ex = st.exp.update(isExp ? 1 : 0, 13);
        float ch = CARD_H + (float) (settingsHeight(m) * ex);
        if (y + ch < CT_TOP || y > CT_BOT) return ch; // off screen

        double al = ga * tabAlpha;

        // background
        Render2D.rect(ctx, x, y, w, ch, 7, a(0xFFFFFF, (16 + 12 * hv) * al));
        if (on > 0.01) {
            Render2D.rectH(ctx, x, y, w, ch, 7, a(Theme.A1, 70 * on * al), a(Theme.A2, 70 * on * al));
            Render2D.outline(ctx, x, y, w, ch, 7, 1f, a(Theme.A1, 170 * on * al), a(Theme.A2, 170 * on * al));
        }

        // title row
        txt(ctx, m.name, x + 9, y + 8, Theme.lerp(0xFFB4B4C6, 0xFFFFFFFF, Math.max(on, hv * 0.7)), al);
        float swW = 22;
        drawSwitch(ctx, x + w - 9 - swW, y + 7, swW, 10, on, al);
        if (m.key > 0) {
            String k = "[" + keyName(m.key) + "]";
            txt(ctx, k, x + w - 9 - swW - 5 - textRenderer.getWidth(k), y + 8, 0xFF7C7C90, al);
        }
        hits.add(new Hit(Kind.CARD, x, y, w, CARD_H, m, null, null));

        // settings (expand with animation)
        if (ex > 0.01) {
            double ra = al * ex;
            boolean live = isExp && ex > 0.95;
            scissor(ctx, x, y + CARD_H, x + w, y + ch);
            float ry = y + CARD_H + 2;
            for (Setting s : m.settings) {
                if (s instanceof BooleanSetting b) {
                    Anim sa = switchAnim.computeIfAbsent(s, k -> new Anim(b.value ? 1 : 0));
                    sa.update(b.value ? 1 : 0, 14);
                    txt(ctx, s.name, x + 9, ry + 5, 0xFFC4C4D2, ra);
                    drawSwitch(ctx, x + w - 9 - 18, ry + 4, 18, 9, sa.value, ra);
                    if (live) hits.add(new Hit(Kind.BOOL, x, ry, w, BOOL_H, m, s, null));
                    ry += BOOL_H;
                } else if (s instanceof NumberSetting n) {
                    Anim sa = sliderAnim.computeIfAbsent(s, k -> new Anim(n.fraction()));
                    sa.update(n.fraction(), 18);
                    String v = fmt(n.value);
                    txt(ctx, s.name, x + 9, ry + 3, 0xFFC4C4D2, ra);
                    txt(ctx, v, x + w - 9 - textRenderer.getWidth(v), ry + 3, 0xFF9C9CB0, ra);
                    float tx = x + 9, tw = w - 18, ty = ry + 17;
                    Render2D.rect(ctx, tx, ty, tw, 3, 1.5f, a(0xFFFFFF, 34 * ra));
                    float fw = (float) (tw * sa.value);
                    if (fw > 0.5f) Render2D.rectH(ctx, tx, ty, fw, 3, 1.5f, a(Theme.A1, 255 * ra), a(Theme.A2, 255 * ra));
                    Render2D.rect(ctx, tx + fw - 3.5f, ty - 2, 7, 7, 3.5f, a(0xFFFFFF, 255 * ra));
                    if (live) hits.add(new Hit(Kind.SLIDER, tx, ry + 10, tw, 16, m, s, null));
                    ry += NUM_H;
                }
            }
            // bind row
            txt(ctx, "Bind", x + 9, ry + 5, 0xFFC4C4D2, ra);
            String bt = binding == m ? "..." : (m.key > 0 ? keyName(m.key) : "None");
            float bw = textRenderer.getWidth(bt) + 10;
            Render2D.rect(ctx, x + w - 9 - bw, ry + 2, bw, 14, 4, a(binding == m ? Theme.A1 : 0xFFFFFF, (binding == m ? 120 : 28) * ra));
            txt(ctx, bt, x + w - 9 - bw + 5, ry + 5, 0xFFFFFFFF, ra);
            if (live) hits.add(new Hit(Kind.BIND, x, ry, w, BIND_H, m, null, null));
            ctx.disableScissor();
        }
        return ch;
    }

    private float settingsHeight(Module m) {
        float t = 4;
        for (Setting s : m.settings) t += s instanceof NumberSetting ? NUM_H : BOOL_H;
        return t + BIND_H + 2;
    }

    private void drawSwitch(DrawContext ctx, float x, float y, float w, float h, double on, double al) {
        Render2D.rect(ctx, x, y, w, h, h / 2f, a(0x3C3C4C, 210 * al));
        if (on > 0.01) Render2D.rectH(ctx, x, y, w, h, h / 2f, a(Theme.A1, 255 * on * al), a(Theme.A2, 255 * on * al));
        float kd = h - 3;
        Render2D.rect(ctx, x + 1.5f + (w - h) * (float) on, y + 1.5f, kd, kd, kd / 2f, a(0xFFFFFF, 255 * al));
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return false;
        float lx = (float) ((mx - ox) / gs), ly = (float) ((my - oy) / gs);
        for (Hit h : hits) {
            if (lx < h.x() || lx >= h.x() + h.w() || ly < h.y() || ly >= h.y() + h.h()) continue;
            if (h.k() != Kind.CAT && (ly < CT_TOP || ly > CT_BOT)) continue;
            switch (h.k()) {
                case CAT -> {
                    if (cat != h.c()) { cat = h.c(); tab.value = 0; scroll.value = 0; scrollTarget = 0; }
                    binding = null;
                }
                case CARD -> {
                    if (button == 0) h.m().toggle();
                    else if (button == 1 && !expanded.remove(h.m())) expanded.add(h.m());
                }
                case BOOL -> { if (button == 0) ((BooleanSetting) h.s()).toggle(); }
                case SLIDER -> {
                    if (button == 0) {
                        dragging = (NumberSetting) h.s();
                        dragX = h.x(); dragW = h.w();
                        dragging.setFraction((lx - dragX) / dragW);
                    }
                }
                case BIND -> { if (button == 0) binding = binding == h.m() ? null : h.m(); }
            }
            return true;
        }
        binding = null;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            float lx = (float) ((mx - ox) / gs);
            dragging.setFraction((lx - dragX) / dragW);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) vAmount * 22f));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                binding.key = (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) ? 0 : keyCode;
            }
            binding = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------ helpers
    private static int a(int rgb, double alpha255) {
        return Theme.argb(rgb, (int) Math.round(Math.max(0, Math.min(255, alpha255))));
    }

    private void txt(DrawContext ctx, String s, float x, float y, int argb, double alpha) {
        int al = (int) Math.round(((argb >>> 24) & 255) * alpha);
        if (al < 8) return; // vanilla draws very transparent text as opaque
        ctx.drawText(textRenderer, s, Math.round(x), Math.round(y), Theme.argb(argb, al), false);
    }

    private void scaledTxt(DrawContext ctx, String s, float x, float y, float scale, int argb, double alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0f);
        ctx.getMatrices().scale(scale, scale, 1f);
        txt(ctx, s, 0, 0, argb, alpha);
        ctx.getMatrices().pop();
    }

    /** Text with an accent-gradient colour running over the letters. */
    private void gradText(DrawContext ctx, String s, float x, float y, float scale, double alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0f);
        ctx.getMatrices().scale(scale, scale, 1f);
        float cx = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            txt(ctx, ch, cx, 0, Theme.lerp(Theme.A1, Theme.A2, i / (double) Math.max(1, s.length() - 1)), alpha);
            cx += textRenderer.getWidth(ch);
        }
        ctx.getMatrices().pop();
    }

    private void scissor(DrawContext ctx, float x1, float y1, float x2, float y2) {
        ctx.enableScissor((int) Math.floor(ox + x1 * gs), (int) Math.floor(oy + y1 * gs),
                (int) Math.ceil(ox + x2 * gs), (int) Math.ceil(oy + y2 * gs));
    }

    private static String fmt(double v) {
        return Math.abs(v - Math.round(v)) < 0.0001 ? String.valueOf(Math.round(v)) : String.format("%.2f", v);
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "K" + key;
        };
    }
}
