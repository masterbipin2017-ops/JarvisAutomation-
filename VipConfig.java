package com.jarvis.automation;

import java.util.HashMap;
import java.util.Map;

/**
 * Edit these constants to match your own headset / contacts.
 * Kept in one place so nothing else needs code changes.
 */
public final class VipConfig {

    // Set this to your headset's Bluetooth name (Settings > Bluetooth) and/or MAC address.
    // Leave one blank ("") if you only want to match on the other.
    public static final String TRUSTED_DEVICE_NAME = "My Headset";
    public static final String TRUSTED_DEVICE_ADDRESS = "";

    // Voice phrase -> E.164 phone number. Match is case-insensitive substring.
    public static final Map<String, String> VIP_CONTACTS = new HashMap<String, String>() {{
        put("call mom", "+10000000000");
        put("call dad", "+10000000001");
        put("call boss", "+10000000002");
    }};

    // Voice phrase -> Spotify URI (open a specific playlist/album).
    public static final Map<String, String> PLAYLISTS = new HashMap<String, String>() {{
        put("play focus playlist", "spotify:playlist:37i9dQZF1DWZeKCadgRdKQ");
        put("play workout", "spotify:playlist:37i9dQZF1DX76Wlfdnj7AP");
    }};

    // WhatsApp auto-reply text.
    public static final String AUTO_REPLY_TEXT =
            "I'm driving / hands-free right now — I'll reply properly soon.";

    private VipConfig() { }
}
