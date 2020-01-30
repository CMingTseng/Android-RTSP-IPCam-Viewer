package com.github.warren_bank.rtsp_ipcam_viewer.mock.data;

import com.google.android.exoplayer2.util.Assertions;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

public final class SubtitleInfo {

    @Nullable
    public static SubtitleInfo createFromIntent(Intent intent, String extrasKeySuffix) {
        if (!intent.hasExtra(C.SUBTITLE_URI_EXTRA + extrasKeySuffix)) {
            return null;
        }
        return new SubtitleInfo(
                Uri.parse(intent.getStringExtra(C.SUBTITLE_URI_EXTRA + extrasKeySuffix)),
                intent.getStringExtra(C.SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix),
                intent.getStringExtra(C.SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix));
    }

    public final Uri uri;
    public final String mimeType;
    @Nullable
    public final String language;

    public SubtitleInfo(Uri uri, String mimeType, @Nullable String language) {
        this.uri = Assertions.checkNotNull(uri);
        this.mimeType = Assertions.checkNotNull(mimeType);
        this.language = language;
    }

    public void addToIntent(Intent intent, String extrasKeySuffix) {
        intent.putExtra(C.SUBTITLE_URI_EXTRA + extrasKeySuffix, uri.toString());
        intent.putExtra(C.SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix, mimeType);
        intent.putExtra(C.SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix, language);
    }
}
