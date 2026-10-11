package com.fongmi.android.tv.utils;

import android.Manifest;
import android.net.Uri;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.impl.PermissionCallback;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Path;
import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.util.function.Consumer;

public class PermissionUtil {

    public static void requestAudio(FragmentActivity activity, Consumer<Boolean> callback) {
        PermissionX.init(activity).permissions(Manifest.permission.RECORD_AUDIO).request(new PermissionCallback(callback));
    }

    public static void requestFile(FragmentActivity activity, Consumer<Boolean> callback) {
        if (hasFileAccess(callback)) return;
        SpiderDebug.log("permission", "request file access managerAvailable=%s", Setting.hasFileManager());
        if (Setting.hasFileManager()) PermissionX.init(activity).permissions().requestManageExternalStoragePermissionNow(new PermissionCallback(result -> finishFileRequest(callback, result)));
        else PermissionX.init(activity).permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new PermissionCallback(result -> finishFileRequest(callback, result)));
    }

    public static void requestFile(Fragment fragment, Consumer<Boolean> callback) {
        if (hasFileAccess(callback)) return;
        SpiderDebug.log("permission", "request file access managerAvailable=%s", Setting.hasFileManager());
        if (Setting.hasFileManager()) PermissionX.init(fragment).permissions().requestManageExternalStoragePermissionNow(new PermissionCallback(result -> finishFileRequest(callback, result)));
        else PermissionX.init(fragment).permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new PermissionCallback(result -> finishFileRequest(callback, result)));
    }

    public static boolean requiresFileAccess(String url) {
        if (TextUtils.isEmpty(url)) return false;
        // Recognize the loader's leading aliases without rewriting URI path traversal.
        if (url.startsWith("../")) url = "file://" + url.substring(3);
        else if (url.startsWith("./")) url = "file://tvbox/" + url.substring(2);
        else if (url.startsWith("clan://")) url = "file://tvbox/" + url.substring(7);
        Uri uri = Uri.parse(url);
        if (uri.getScheme() != null && !"file".equalsIgnoreCase(uri.getScheme())) return false;
        // Match the local loader, including shared-storage aliases such as file://tvbox/.
        File file = Path.local(url);
        if (file == null) return true;
        return !inside(Path.files(), file) && !inside(Path.cache(), file);
    }

    private static boolean inside(File root, File file) {
        try {
            String rootPath = root.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.equals(rootPath) || filePath.startsWith(rootPath + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean requiresSourceFileAccess() {
        int localSources = Setting.SOURCE_SITES_JSON | Setting.SOURCE_SITES_JS | Setting.SOURCE_SITES_PY
                | Setting.SOURCE_SITES_RAW | Setting.SOURCE_LIVES_FILE;
        return (Setting.getSourceBlockMask() & localSources) != localSources;
    }

    private static boolean hasFileAccess(Consumer<Boolean> callback) {
        if (!Setting.hasFileAccess()) return false;
        SpiderDebug.log("permission", "file access already granted");
        if (callback != null) callback.accept(true);
        return true;
    }

    private static void finishFileRequest(Consumer<Boolean> callback, boolean result) {
        boolean granted = Setting.hasFileAccess();
        SpiderDebug.log("permission", "file access request result callback=%s granted=%s", result, granted);
        if (callback != null) callback.accept(granted);
    }

    public static void requestNotify(FragmentActivity activity) {
        PermissionX.init(activity).permissions(PermissionX.permission.POST_NOTIFICATIONS).request(new PermissionCallback());
    }
}
