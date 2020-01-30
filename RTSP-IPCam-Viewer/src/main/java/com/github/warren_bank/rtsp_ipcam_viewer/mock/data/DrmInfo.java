package com.github.warren_bank.rtsp_ipcam_viewer.mock.data;

import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import androidx.annotation.Nullable;

import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;

public final class DrmInfo {

    public static DrmInfo createFromIntent(Intent intent, String extrasKeySuffix) {
        String schemeKey = C.DRM_SCHEME_EXTRA + extrasKeySuffix;
        String schemeUuidKey = C.DRM_SCHEME_UUID_EXTRA + extrasKeySuffix;
        if (!intent.hasExtra(schemeKey) && !intent.hasExtra(schemeUuidKey)) {
            return null;
        }
        String drmSchemeExtra =
                intent.hasExtra(schemeKey)
                        ? intent.getStringExtra(schemeKey)
                        : intent.getStringExtra(schemeUuidKey);
        UUID drmScheme = Util.getDrmUuid(drmSchemeExtra);
        String drmLicenseUrl = intent.getStringExtra(C.DRM_LICENSE_URL_EXTRA + extrasKeySuffix);
        String[] keyRequestPropertiesArray =
                intent.getStringArrayExtra(C.DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix);
        String[] drmSessionForClearTypesExtra =
                intent.getStringArrayExtra(C.DRM_SESSION_FOR_CLEAR_TYPES_EXTRA + extrasKeySuffix);
        int[] drmSessionForClearTypes = toTrackTypeArray(drmSessionForClearTypesExtra);
        boolean drmMultiSession =
                intent.getBooleanExtra(C.DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, false);
        return new DrmInfo(
                drmScheme,
                drmLicenseUrl,
                keyRequestPropertiesArray,
                drmSessionForClearTypes,
                drmMultiSession);
    }

    public final UUID drmScheme;
    public final String drmLicenseUrl;
    public final String[] drmKeyRequestProperties;
    public final int[] drmSessionForClearTypes;
    public final boolean drmMultiSession;

    public DrmInfo(
            UUID drmScheme,
            String drmLicenseUrl,
            String[] drmKeyRequestProperties,
            int[] drmSessionForClearTypes,
            boolean drmMultiSession) {
        this.drmScheme = drmScheme;
        this.drmLicenseUrl = drmLicenseUrl;
        this.drmKeyRequestProperties = drmKeyRequestProperties;
        this.drmSessionForClearTypes = drmSessionForClearTypes;
        this.drmMultiSession = drmMultiSession;
    }

    public void addToIntent(Intent intent, String extrasKeySuffix) {
        Assertions.checkNotNull(intent);
        intent.putExtra(C.DRM_SCHEME_EXTRA + extrasKeySuffix, drmScheme.toString());
        intent.putExtra(C.DRM_LICENSE_URL_EXTRA + extrasKeySuffix, drmLicenseUrl);
        intent.putExtra(C.DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix, drmKeyRequestProperties);
        ArrayList<String> typeStrings = new ArrayList<>();
        for (int type : drmSessionForClearTypes) {
            // Only audio and video are supported.
            typeStrings.add(type == TRACK_TYPE_AUDIO ? "audio" : "video");
        }
        intent.putExtra(C.DRM_SESSION_FOR_CLEAR_TYPES_EXTRA + extrasKeySuffix, typeStrings.toArray(new String[0]));
        intent.putExtra(C.DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, drmMultiSession);
    }

    public static int[] toTrackTypeArray(@Nullable String[] trackTypeStringsArray) {
        if (trackTypeStringsArray == null) {
            return new int[0];
        }
        HashSet<Integer> trackTypes = new HashSet<>();
        for (String trackTypeString : trackTypeStringsArray) {
            switch (Util.toLowerInvariant(trackTypeString)) {
                case "audio":
                    trackTypes.add(com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO);
                    break;
                case "video":
                    trackTypes.add(com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid track type: " + trackTypeString);
            }
        }
        return Util.toArray(new ArrayList<>(trackTypes));
    }
}
