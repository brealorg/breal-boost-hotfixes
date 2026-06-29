package app.morphe.extension.boostforreddit.giphy;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import java.util.Locale;

public final class MediaTapActionPreference extends ListPreference {
    private static final String ACTION_IMAGE_VIEWER = "image_viewer";
    private static final String ACTION_VIDEO_VIEWER = "video_viewer";
    private static final String ACTION_BROWSER = "browser";
    private static final String ACTION_DISABLED = "disabled";

    private static final CharSequence[] ENTRIES = new CharSequence[]{
            "Image viewer",
            "Video viewer",
            "Browser",
            "Disabled"
    };

    private static final CharSequence[] VALUES = new CharSequence[]{
            ACTION_IMAGE_VIEWER,
            ACTION_VIDEO_VIEWER,
            ACTION_BROWSER,
            ACTION_DISABLED
    };

    public MediaTapActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        configure();
    }

    public MediaTapActionPreference(Context context) {
        super(context);
        configure();
    }

    private void configure() {
        setEntries(ENTRIES);
        setEntryValues(VALUES);

        String current = getValue();
        if (current == null) {
            current = defaultForKey(getKey());
            setDefaultValue(current);
        }

        setSummary(labelFor(current));
    }

    @Override
    public void setValue(String value) {
        String normalized = normalize(value, defaultForKey(getKey()));
        super.setValue(normalized);
        setSummary(labelFor(normalized));
    }

    @Override
    public CharSequence getSummary() {
        return labelFor(normalize(getValue(), defaultForKey(getKey())));
    }

    private static String defaultForKey(String key) {
        if (key != null && (key.contains("direct_reddit_gif") || key.contains("giphy_preview"))) {
            return "video_viewer";
        }
        return "image_viewer";
    }

    private static String labelFor(String action) {
        String normalized = normalize(action, ACTION_IMAGE_VIEWER);

        if (ACTION_VIDEO_VIEWER.equals(normalized)) {
            return "Video viewer";
        }

        if (ACTION_BROWSER.equals(normalized)) {
            return "Browser";
        }

        if (ACTION_DISABLED.equals(normalized)) {
            return "Disabled";
        }

        return "Image viewer";
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim().toLowerCase(Locale.US);

        if (ACTION_IMAGE_VIEWER.equals(normalized)
                || ACTION_VIDEO_VIEWER.equals(normalized)
                || ACTION_BROWSER.equals(normalized)
                || ACTION_DISABLED.equals(normalized)) {
            return normalized;
        }

        return fallback;
    }
}
