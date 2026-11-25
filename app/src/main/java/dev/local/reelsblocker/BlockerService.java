package dev.local.reelsblocker;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class BlockerService extends AccessibilityService {

    // Instagram Reels viewer. Inside Instagram, Reels are called clips.
    private static final String IG_PACKAGE = "com.instagram.android";
    private static final String[] IG_IDS = {
            "com.instagram.android:id/clips_viewer_view_pager"
    };

    // YouTube Shorts player. These may change with YouTube updates.
    private static final String YT_PACKAGE = "com.google.android.youtube";
    private static final String[] YT_IDS = {
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_player_page_container"
    };

    private static final long COOLDOWN_MS = 1500;
    private long lastBackAt = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!isBlockingOn()) return;

        String pkg = event.getPackageName().toString();
        String[] ids;
        if (pkg.equals(IG_PACKAGE)) {
            ids = IG_IDS;
        } else if (pkg.equals(YT_PACKAGE)) {
            ids = YT_IDS;
        } else {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastBackAt < COOLDOWN_MS) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            // Check the root really belongs to the app we care about.
            CharSequence rootPkg = root.getPackageName();
            if (rootPkg == null || !rootPkg.toString().equals(pkg)) return;

            for (String id : ids) {
                List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(id);
                if (found != null && !found.isEmpty()) {
                    boolean visible = false;
                    for (AccessibilityNodeInfo n : found) {
                        if (n.isVisibleToUser()) visible = true;
                        n.recycle();
                    }
                    if (visible) {
                        lastBackAt = now;
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        incrementCounter();
                        return;
                    }
                }
            }
        } finally {
            root.recycle();
        }
    }

    private boolean isBlockingOn() {
        SharedPreferences p = getSharedPreferences("settings", MODE_PRIVATE);
        if (!p.getBoolean("enabled", true)) return false;
        long pausedUntil = p.getLong("pausedUntil", 0);
        return System.currentTimeMillis() >= pausedUntil;
    }

    private void incrementCounter() {
        SharedPreferences p = getSharedPreferences("settings", MODE_PRIVATE);
        p.edit().putInt("blockedCount", p.getInt("blockedCount", 0) + 1).apply();
    }

    @Override
    public void onInterrupt() {
    }
}
