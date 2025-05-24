package com.calmhostacct;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

import java.io.File;

@SimpleObject(external = true)
@DesignerComponent(
        category = ComponentCategory.EXTENSION,
        description = "Open source settings extension. Version v1",
        nonVisible = true,
        version = 7
)
@UsesPermissions(permissionNames = "android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_SETTINGS, android.permission.ACCESS_NOTIFICATION_POLICY")
public class OpenSettings extends AndroidNonvisibleComponent implements Component {

    private static final String LOG_TAG = "OpenSettings";
    private final Activity activity;
    private final AudioManager audioManager;
    private final ContentResolver contentResolver;
    private final Context context;
    private final boolean isRepl;
    private boolean showUI = true;
    private Ringtone ringtone;

    public OpenSettings(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        this.activity = container.$context();
        this.contentResolver = context.getContentResolver();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.isRepl = container.$form() instanceof ReplForm;
        Log.d(LOG_TAG, "OpenSettings Created");
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Brightness (0-255)")
    public int Brightness() {
        return Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Whether UI is shown when setting volume.")
    public boolean ShowUI() {
        return showUI;
    }

    @DesignerProperty(defaultValue = "True", editorType = "boolean")
    @SimpleProperty
    public void ShowUI(boolean value) {
        this.showUI = value;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Media volume as percent.")
    public int VolumeMusic() {
        return getVolume(AudioManager.STREAM_MUSIC);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Ringtone volume as percent.")
    public int VolumeRing() {
        return getVolume(AudioManager.STREAM_RING);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Alarm volume as percent.")
    public int VolumeAlarm() {
        return getVolume(AudioManager.STREAM_ALARM);
    }

    @DesignerProperty(defaultValue = "82", editorType = "non_negative_integer")
    @SimpleProperty
    public void VolumeMusic(int percent) {
        setVolume(percent, AudioManager.STREAM_MUSIC);
    }

    @DesignerProperty(defaultValue = "82", editorType = "non_negative_integer")
    @SimpleProperty
    public void VolumeRing(int percent) {
        if (Build.VERSION.SDK_INT < 28) {
            setVolume(percent, AudioManager.STREAM_RING);
        }
    }

    @DesignerProperty(defaultValue = "82", editorType = "non_negative_integer")
    @SimpleProperty
    public void VolumeAlarm(int percent) {
        setVolume(percent, AudioManager.STREAM_ALARM);
    }

    public int getVolume(int streamType) {
        int max = audioManager.getStreamMaxVolume(streamType);
        int current = audioManager.getStreamVolume(streamType);
        return (current * 100) / max;
    }

    public void setVolume(int percent, int streamType) {
        percent = Math.max(0, Math.min(percent, 100));
        int max = audioManager.getStreamMaxVolume(streamType);
        int volume = (max * percent) / 100;
        audioManager.setStreamVolume(streamType, volume, showUI ? AudioManager.FLAG_SHOW_UI : 0);
    }

    @SimpleFunction(description = "Get current ringtone. Type can be RINGTONE, NOTIFICATION, or ALARM.")
    public String RingtoneGet(String type) {
        setRingtone(type);
        return ringtone != null ? ringtone.getTitle(context) : "";
    }

    @SimpleFunction(description = "Play ringtone. Type can be RINGTONE, NOTIFICATION, or ALARM.")
    public void RingtonePlay(String type) {
        if (ringtone != null) {
            ringtone.stop();
        }
        setRingtone(type);
        if (ringtone != null) {
            ringtone.play();
        }
    }

    @SimpleFunction(description = "Stop currently playing ringtone.")
    public void RingtoneStop() {
        if (ringtone != null) {
            ringtone.stop();
        }
    }

    public void setRingtone(String type) {
        int ringtoneType;
        switch (type.toUpperCase()) {
            case "NOTIFICATION":
                ringtoneType = RingtoneManager.TYPE_NOTIFICATION;
                break;
            case "ALARM":
                ringtoneType = RingtoneManager.TYPE_ALARM;
                break;
            default:
                ringtoneType = RingtoneManager.TYPE_RINGTONE;
                break;
        }
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, ringtoneType);
        ringtone = RingtoneManager.getRingtone(context, uri);
    }

    public String completeFileName(String fileName) {
        File sd = Environment.getExternalStorageDirectory();
        if (fileName.startsWith("file:///")) {
            return fileName.substring(7);
        } else if (fileName.startsWith("//")) {
            return isRepl ? sd.getPath() + "/AppInventor/assets/" + fileName.substring(2) : fileName.substring(2);
        } else if (!fileName.startsWith("/")) {
            return sd + File.separator + fileName;
        } else if (!fileName.startsWith(sd.toString())) {
            return sd + fileName;
        }
        return fileName;
    }

    public static String fileExtension(String name) {
        int idx = name.lastIndexOf('.');
        return (idx == -1 || idx == name.length() - 1) ? "" : name.substring(idx + 1);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Brightness mode (MANUAL or AUTO)")
    public String BrightnessMode() {
        int mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
        return (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "AUTO" : "MANUAL";
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Font scale")
    public float FontScale() {
        return Settings.System.getFloat(activity.getContentResolver(), Settings.System.FONT_SCALE, 1.0f);
    }
}

