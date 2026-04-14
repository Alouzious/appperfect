package com.example.pitchperfect.utils;

import com.example.pitchperfect.R;

public class IconHelper {

    public static final class Navigation {
        public static final int HOME = android.R.drawable.ic_menu_agenda;
        public static final int PRACTICE = android.R.drawable.ic_menu_play_clip;
        public static final int FEEDBACK = android.R.drawable.ic_menu_info_details;
        public static final int PROFILE = android.R.drawable.ic_menu_myplaces;
    }

    public static final class Actions {
        public static final int UPLOAD = android.R.drawable.ic_menu_upload;
        public static final int RECORD = android.R.drawable.ic_media_play;
        public static final int STOP = android.R.drawable.ic_media_pause;
        public static final int DOWNLOAD = android.R.drawable.ic_menu_download;
        public static final int SETTINGS = android.R.drawable.ic_menu_preferences;
        public static final int LOGOUT = android.R.drawable.ic_menu_close_clear_cancel;
    }

    public static final class Audio {
        public static final int SPEAKER = android.R.drawable.ic_media_play;
        public static final int MICROPHONE = android.R.drawable.ic_menu_call;
        public static final int SOUND_OFF = android.R.drawable.ic_media_pause;
    }

    public static final class Status {
        public static final int SUCCESS = android.R.drawable.ic_dialog_info;
        public static final int ERROR = android.R.drawable.ic_dialog_alert;
        public static final int LOADING = android.R.drawable.ic_menu_search;
        public static final int COMPLETED = android.R.drawable.ic_menu_save;
    }

    public static final class CategoryIcons {
        // Map pitch category to icon
        public static int getCategoryIcon(String category) {
            if (category == null) return android.R.drawable.ic_menu_agenda;

            switch (category.toLowerCase()) {
                case "startup":
                    return android.R.drawable.ic_menu_add;
                case "saas":
                    return android.R.drawable.ic_menu_compass;
                case "investment":
                    return android.R.drawable.ic_menu_manage;
                case "product":
                    return android.R.drawable.ic_menu_view;
                default:
                    return android.R.drawable.ic_menu_agenda;
            }
        }
    }

    public static final class FeedbackIcons {
        public static final int PACE = android.R.drawable.ic_media_play;
        public static final int CLARITY = android.R.drawable.ic_menu_info_details;
        public static final int CONFIDENCE = android.R.drawable.ic_menu_myplaces;
        public static final int CONTENT = android.R.drawable.ic_menu_view;
        public static final int STRUCTURE = android.R.drawable.ic_menu_manage;
    }
}
