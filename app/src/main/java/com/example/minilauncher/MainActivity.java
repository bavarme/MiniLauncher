package com.example.minilauncher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.content.ComponentName;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Домашний экран в стиле Apple TV: часы сверху слева, фоновые фото с плавной сменой,
 * подсветка фона цветом выбранного приложения, ряд избранного (док) внизу
 * и сетка всех приложений, которая открывается прокруткой вниз.
 * Только стандартные View, без AndroidX и без сети.
 */
public class MainActivity extends Activity {

    private static final long WALLPAPER_INTERVAL_MS = 10 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 15_000L);
        }
    };

    private final Runnable rotateWallpaper = new Runnable() {
        @Override public void run() {
            nextWallpaper();
        }
    };

    private final Set<String> hidden = new HashSet<>();
    private String[] dockPkgs = new String[0];
    private String[] wallpapers = new String[0];
    private int columns = 5;
    // Масштаб интерфейса: 1.0 у экрана 960×540dp (1080p), больше на 4K, меньше на маленьких экранах
    private float ui = 1f;

    private FrameLayout root;
    private ImageView wallA, wallB;
    private boolean showingA = true;
    private int wallIndex = -1;
    private long wallChangedAt;

    private View glow;
    private int glowColor = 0xFF303030;
    private ValueAnimator glowAnim;

    private ScrollView scroll;
    private LinearLayout content, dockRow, grid;
    private TextView greeting, clock, date, title, appsLabel;

    private final List<View> dockTargets = new ArrayList<>();
    private final List<View> gridTargets = new ArrayList<>();
    private View lastFocus;
    private String appSignature = "";

    static final class App {
        String label;
        Intent intent;
        Drawable art;
        boolean banner;
        int color;
    }

    /** Ячейка 16:9: высота рассчитывается от ширины. */
    static final class AspectFrame extends FrameLayout {
        AspectFrame(Context c) {
            super(c);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
        }
    }

    // ------------------------------------------------------------ жизненный цикл

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        DisplayMetrics m = getResources().getDisplayMetrics();
        float wDp = m.widthPixels / m.density;
        float hDp = m.heightPixels / m.density;
        ui = Math.max(0.8f, Math.min(2.5f, Math.min(wDp / 960f, hDp / 540f)));
        // Плиток в ряд столько, сколько помещается при размере плитки около 190dp
        columns = Math.max(3, Math.round(wDp / (190f * ui)));
        dockPkgs = getResources().getStringArray(R.array.dock_apps);
        for (String p : getResources().getStringArray(R.array.hidden_apps)) hidden.add(p);
        hidden.add(getPackageName());
        try {
            wallpapers = getAssets().list("wallpapers");
        } catch (IOException e) {
            wallpapers = new String[0];
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());
        buildApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(clockTick);
        updateClock();

        // Перестраиваем сетку только если установили или удалили приложение
        if (!signature().equals(appSignature)) buildApps();

        if (lastFocus != null && lastFocus.isAttachedToWindow()) {
            lastFocus.requestFocus();
        } else if (!dockTargets.isEmpty()) {
            dockTargets.get(0).requestFocus();
        }

        long elapsed = SystemClock.elapsedRealtime() - wallChangedAt;
        if (wallIndex < 0 || elapsed >= WALLPAPER_INTERVAL_MS) {
            nextWallpaper();
        } else {
            handler.postDelayed(rotateWallpaper, WALLPAPER_INTERVAL_MS - elapsed);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Нажали «Домой», когда лаунчер уже открыт: возвращаемся к доку
        scroll.smoothScrollTo(0, 0);
        if (!dockTargets.isEmpty()) dockTargets.get(0).requestFocus();
    }

    @Override
    public void onBackPressed() {
        // Назад из сетки возвращает к доку, на самом верху ничего не делает
        if (scroll.getScrollY() > 0 && !dockTargets.isEmpty()) {
            dockTargets.get(0).requestFocus();
        }
    }

    // ------------------------------------------------------------ интерфейс

    private View buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        wallA = wallLayer();
        wallB = wallLayer();
        root.addView(wallA, match());
        root.addView(wallB, match());

        // Затемнение сверху (часы) и снизу (док), чтобы текст читался
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB3000000, 0x40000000, 0x26000000, 0x8C000000, 0xE6000000}));
        root.addView(scrim, match());

        glow = new View(this);
        setGlow(glowColor);
        root.addView(glow, match());

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        root.addView(scroll, match());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));

        // Первый экран: шапка, док. Ниже — сетка приложений.
        int screenH = getResources().getDisplayMetrics().heightPixels;
        FrameLayout hero = new FrameLayout(this);
        content.addView(hero, new LinearLayout.LayoutParams(-1, screenH));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        greeting = text(12, 0xB3FFFFFF, "sans-serif-medium");
        greeting.setLetterSpacing(0.25f);
        clock = text(60, 0xFFFFFFFF, "sans-serif-thin");
        date = text(12, 0x99FFFFFF, "sans-serif-medium");
        date.setLetterSpacing(0.2f);
        header.addView(greeting);
        header.addView(clock);
        header.addView(date);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        hp.setMargins(dp(48), dp(36), 0, 0);
        hero.addView(header, hp);

        LinearLayout pills = new LinearLayout(this);
        pills.setGravity(Gravity.CENTER_VERTICAL);
        pills.addView(pill("⚙  Настройки", () -> openAction(Settings.ACTION_SETTINGS)));
        pills.addView(pill("◉  Wi-Fi", () -> openAction(Settings.ACTION_WIFI_SETTINGS)));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        pp.setMargins(0, dp(40), dp(48), 0);
        hero.addView(pills, pp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bp.setMargins(dp(48), 0, dp(48), dp(30));
        hero.addView(bottom, bp);

        title = text(34, 0xFFFFFFFF, "sans-serif-light");
        title.setSingleLine(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.setMargins(dp(8), 0, 0, dp(14));
        bottom.addView(title, tp);

        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER_VERTICAL);
        dockRow.setPadding(dp(20), dp(22), dp(20), dp(22));
        GradientDrawable band = new GradientDrawable();
        band.setCornerRadius(dp(26));
        band.setColor(0x1FFFFFFF);
        band.setStroke(dp(1), 0x14FFFFFF);
        dockRow.setBackground(band);
        bottom.addView(dockRow, new LinearLayout.LayoutParams(-1, -2));

        appsLabel = text(11, 0x99FFFFFF, "sans-serif-medium");
        appsLabel.setText("ВСЕ ПРИЛОЖЕНИЯ");
        appsLabel.setLetterSpacing(0.22f);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-2, -2);
        alp.setMargins(dp(68), dp(8), 0, dp(18));
        content.addView(appsLabel, alp);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2);
        glp.setMargins(dp(68), 0, dp(68), dp(160));
        content.addView(grid, glp);

        return root;
    }

    private ImageView wallLayer() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        v.setAlpha(0f);
        return v;
    }

    private TextView text(float sp, int color, String family) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * ui);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        t.setShadowLayer(dp(10), 0, dp(1), 0xB3000000);
        return t;
    }

    private TextView pill(String label, Runnable action) {
        TextView p = text(12, 0xE6FFFFFF, "sans-serif-medium");
        p.setText(label);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(16), 0, dp(16), 0);
        p.setShadowLayer(0, 0, 0, 0);
        p.setFocusable(true);
        p.setClickable(true);
        p.setOnClickListener(v -> action.run());

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(17));
        bg.setColor(0x66000000);
        bg.setStroke(dp(1), 0x33FFFFFF);
        p.setBackground(bg);
        p.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0xF2FFFFFF : 0x66000000);
            p.setTextColor(has ? 0xFF111111 : 0xE6FFFFFF);
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(200).start();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(34));
        lp.setMargins(dp(10), 0, 0, 0);
        p.setLayoutParams(lp);
        return p;
    }

    // ------------------------------------------------------------ приложения

    private Intent leanbackIntent() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
    }

    /** Подпись списка приложений: если она не менялась, перестраивать сетку не нужно. */
    private String signature() {
        StringBuilder sb = new StringBuilder();
        for (ResolveInfo ri : getPackageManager().queryIntentActivities(leanbackIntent(), 0)) {
            sb.append(ri.activityInfo.packageName).append(';');
        }
        return sb.toString();
    }

    private void buildApps() {
        appSignature = signature();
        PackageManager pm = getPackageManager();

        Map<String, App> byPkg = new LinkedHashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(leanbackIntent(), 0)) {
            ActivityInfo ai = ri.activityInfo;
            if (hidden.contains(ai.packageName) || byPkg.containsKey(ai.packageName)) continue;

            App a = new App();
            a.label = String.valueOf(ri.loadLabel(pm));
            a.intent = leanbackIntent().setComponent(new ComponentName(ai.packageName, ai.name));
            // У приложений для ТВ бывает широкий баннер (16:9), он выглядит лучше иконки
            Drawable banner = ai.loadBanner(pm);
            if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
            a.banner = banner != null;
            a.art = a.banner ? banner : ri.loadIcon(pm);
            a.color = dominantColor(a.art);
            byPkg.put(ai.packageName, a);
        }

        List<App> dock = new ArrayList<>();
        for (String p : dockPkgs) {
            App a = byPkg.remove(p);
            if (a != null) dock.add(a);
        }
        List<App> rest = new ArrayList<>(byPkg.values());
        Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));
        if (dock.isEmpty()) {
            int n = Math.min(5, rest.size());
            dock.addAll(rest.subList(0, n));
            rest = new ArrayList<>(rest.subList(n, rest.size()));
        }

        dockRow.removeAllViews();
        grid.removeAllViews();
        dockTargets.clear();
        gridTargets.clear();

        for (int i = 0; i < dock.size(); i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) lp.setMargins(dp(12), 0, 0, 0);
            dockRow.addView(tile(dock.get(i), true, dockTargets), lp);
        }

        LinearLayout row = null;
        for (int i = 0; i < rest.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(i % columns == 0 ? 0 : dp(16), 0, 0, dp(26));
            row.addView(tile(rest.get(i), false, gridTargets), lp);
        }
        // Добиваем последнюю строку пустыми местами, чтобы плитки не растягивались
        if (row != null) {
            for (int k = rest.size() % columns; k != 0 && k < columns; k++) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }

        appsLabel.setVisibility(rest.isEmpty() ? View.GONE : View.VISIBLE);
        if (!dock.isEmpty()) {
            title.setText(dock.get(0).label);
            setGlow(dock.get(0).color);
        } else {
            title.setText("");
        }
    }

    /** Плитка: карточка с баннером или иконкой. Подпись под ней видна только в сетке. */
    private View tile(App app, boolean inDock, List<View> targets) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);

        FrameLayout card = new AspectFrame(this);
        card.setFocusable(true);
        card.setClickable(true);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(12));
            }
        });

        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(dp(12));
        base.setColor(app.banner ? 0xFF1C1C1E : darken(app.color));
        card.setBackground(base);

        ImageView img = new ImageView(this);
        img.setImageDrawable(app.art);
        if (app.banner) {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, match());
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setPadding(dp(28), dp(20), dp(28), dp(20));
            card.addView(img, match());
        }

        // Белая рамка при фокусе
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(12));
        ring.setStroke(dp(3), 0xFFFFFFFF);
        ring.setAlpha(0);
        card.setForeground(ring);

        TextView label = text(15, 0xFFFFFFFF, "sans-serif-medium");
        label.setText(app.label);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);

        box.addView(card, new LinearLayout.LayoutParams(-1, -2));
        if (!inDock) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(10), 0, 0);
            box.addView(label, lp);
        }

        card.setOnClickListener(v -> openApp(app));
        card.setOnFocusChangeListener((v, has) -> {
            float s = has ? (inDock ? 1.12f : 1.08f) : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(has ? 260 : 180)
                    .setInterpolator(has ? new OvershootInterpolator(1.5f) : new DecelerateInterpolator())
                    .start();
            ring.setAlpha(has ? 255 : 0);
            label.animate().alpha(has ? 1f : 0f).setDuration(180).start();
            if (has) {
                lastFocus = v;
                title.setText(app.label);
                animateGlow(app.color);
            }
        });

        targets.add(card);
        return box;
    }

    private void openApp(App app) {
        try {
            startActivity(app.intent);
        } catch (Exception ignored) {
            // приложение недоступно, ничего не делаем
        }
    }

    private void openAction(String action) {
        try {
            startActivity(new Intent(action));
        } catch (Exception ignored) {
            // настроек нет, ничего не делаем
        }
    }

    // ------------------------------------------------------------ обои

    private void nextWallpaper() {
        handler.removeCallbacks(rotateWallpaper);
        if (wallpapers.length > 0) {
            wallIndex = (wallIndex + 1) % wallpapers.length;
            Bitmap bmp = decodeWallpaper(wallpapers[wallIndex]);
            ImageView incoming = showingA ? wallB : wallA;
            ImageView outgoing = showingA ? wallA : wallB;
            incoming.setImageBitmap(bmp);
            incoming.animate().alpha(1f).setDuration(1200).start();
            outgoing.animate().alpha(0f).setDuration(1200).start();
            showingA = !showingA;
        }
        wallChangedAt = SystemClock.elapsedRealtime();
        handler.postDelayed(rotateWallpaper, WALLPAPER_INTERVAL_MS);
    }

    private Bitmap decodeWallpaper(String name) {
        try (InputStream in = getAssets().open("wallpapers/" + name)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;                       // меньше памяти, для ТВ достаточно
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeStream(in, null, o);
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------ часы и подсветка

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        int h = now.get(Calendar.HOUR_OF_DAY);
        greeting.setText(h < 5 ? "ДОБРОЙ НОЧИ" : h < 12 ? "ДОБРОЕ УТРО"
                : h < 17 ? "ДОБРЫЙ ДЕНЬ" : "ДОБРЫЙ ВЕЧЕР");
        boolean is24 = android.text.format.DateFormat.is24HourFormat(this);
        clock.setText(new SimpleDateFormat(is24 ? "H:mm" : "h:mm", Locale.getDefault())
                .format(now.getTime()));
        date.setText(new SimpleDateFormat("EEEE  d MMMM", Locale.getDefault())
                .format(now.getTime()).toUpperCase(Locale.getDefault()));
    }

    private void setGlow(int color) {
        glowColor = color;
        glow.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.argb(0x73, Color.red(color), Color.green(color), Color.blue(color)),
                        0x00000000}));
    }

    private void animateGlow(int to) {
        if (glowAnim != null) glowAnim.cancel();
        glowAnim = ValueAnimator.ofObject(new ArgbEvaluator(), glowColor, to);
        glowAnim.setDuration(400);
        glowAnim.addUpdateListener(a -> setGlow((Integer) a.getAnimatedValue()));
        glowAnim.start();
    }

    /** Средний цвет картинки: из него делаем подсветку и фон иконок. */
    private static int dominantColor(Drawable d) {
        Bitmap bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, 8, 8);
        d.draw(c);
        long r = 0, g = 0, b = 0;
        int n = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int p = bmp.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                r += Color.red(p);
                g += Color.green(p);
                b += Color.blue(p);
                n++;
            }
        }
        bmp.recycle();
        if (n == 0) return 0xFF3A3A3A;
        return Color.rgb((int) (r / n), (int) (g / n), (int) (b / n));
    }

    private static int darken(int c) {
        return Color.rgb(Color.red(c) / 2, Color.green(c) / 2, Color.blue(c) / 2);
    }

    // ------------------------------------------------------------ утилиты

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(-1, -1);
    }

    /** Размер в dp, умноженный на масштаб экрана: на любом ТВ все отступы пропорциональны. */
    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value * ui,
                getResources().getDisplayMetrics()));
    }
}
