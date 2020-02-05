/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.warren_bank.rtsp_ipcam_viewer.mock.data;


import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import androidx.annotation.Nullable;

import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.ACTION_VIEW_LIST;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.AD_TAG_URI_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.DRM_KEY_REQUEST_PROPERTIES_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.DRM_LICENSE_URL_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.DRM_MULTI_SESSION_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.DRM_SCHEME_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.DRM_SCHEME_UUID_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.DRM_SESSION_FOR_CLEAR_TYPES_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.EXTENSION_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.IS_LIVE_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.SUBTITLE_LANGUAGE_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.SUBTITLE_MIME_TYPE_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.SUBTITLE_URI_EXTRA;
import static com.github.warren_bank.rtsp_ipcam_viewer.mock.data.C.URI_EXTRA;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;

public abstract class Sample {
    @Nullable
    public final String name;
    public boolean is_enabled;

    public Sample(String name) {
        this(name, true);
    }

    public Sample(String name, boolean is_enabled) {
        this.name = name;
        this.is_enabled = is_enabled;
    }

    public abstract void addToIntent(Intent intent);

    public static final class UriSample extends Sample {
        private static final String TAG = "UriSample";
        public final Uri uri;
        public final String extension;
        public final boolean isLive;
        public final DrmInfo drmInfo;
        public final Uri adTagUri;
        @Nullable
        public final String sphericalStereoMode;
        @Nullable
        public SubtitleInfo subtitleInfo;


        public String URL_low_res;
        public String URL_high_res;

        public UriSample(String name, boolean is_enabled, Uri uri, String extension, boolean isLive, DrmInfo drmInfo, Uri adTagUri, @Nullable String sphericalStereoMode, @Nullable SubtitleInfo subtitleInfo) {
            super(name, is_enabled);
            this.uri = uri;
            this.extension = extension;
            this.isLive = isLive;
            this.drmInfo = drmInfo;
            this.adTagUri = adTagUri;
            this.sphericalStereoMode = sphericalStereoMode;
            this.subtitleInfo = subtitleInfo;
            this.URL_high_res = uri.getPath();
            this.URL_low_res = uri.getPath();
        }

        public static UriSample createFromIntent(Uri uri, Intent intent, String extrasKeySuffix) {
            String extension = intent.getStringExtra(EXTENSION_EXTRA + extrasKeySuffix);
            String adsTagUriString = intent.getStringExtra(AD_TAG_URI_EXTRA + extrasKeySuffix);
            boolean isLive =
                    intent.getBooleanExtra(IS_LIVE_EXTRA + extrasKeySuffix, /* defaultValue= */ false);
            Uri adTagUri = adsTagUriString != null ? Uri.parse(adsTagUriString) : null;
            return new UriSample(
                    /* name= */ null, false,
                    uri,
                    extension,
                    isLive,
                    DrmInfo.createFromIntent(intent, extrasKeySuffix),
                    adTagUri,
                    /* sphericalStereoMode= */ null,
                    SubtitleInfo.createFromIntent(intent, extrasKeySuffix));
        }

        @Override
        public void addToIntent(Intent intent) {
//      intent.setAction(PlayerActivity.ACTION_VIEW).setData(uri);
//      intent.putExtra(PlayerActivity.IS_LIVE_EXTRA, isLive);
//      intent.putExtra(PlayerActivity.SPHERICAL_STEREO_MODE_EXTRA, sphericalStereoMode);
            addPlayerConfigToIntent(intent, /* extrasKeySuffix= */ "");
        }

        public void addToPlaylistIntent(Intent intent, String extrasKeySuffix) {
//      intent.putExtra(PlayerActivity.URI_EXTRA + extrasKeySuffix, uri.toString());
//      intent.putExtra(PlayerActivity.IS_LIVE_EXTRA + extrasKeySuffix, isLive);
            addPlayerConfigToIntent(intent, extrasKeySuffix);
        }

        private void addPlayerConfigToIntent(Intent intent, String extrasKeySuffix) {
            intent
                    .putExtra(EXTENSION_EXTRA + extrasKeySuffix, extension)
                    .putExtra(
                            AD_TAG_URI_EXTRA + extrasKeySuffix, adTagUri != null ? adTagUri.toString() : null);
            if (drmInfo != null) {
                drmInfo.addToIntent(intent, extrasKeySuffix);
            }
            if (subtitleInfo != null) {
                subtitleInfo.addToIntent(intent, extrasKeySuffix);
            }
        }
    }

    public static ArrayList<Sample> fromJson(String jsonVideos) {
        ArrayList<Sample> videos;
        Gson gson = new Gson();
        videos = gson.fromJson(jsonVideos, new TypeToken<ArrayList<Sample>>() {
        }.getType());
        return videos;
    }

    public static String toJson(ArrayList<Sample> videos) {
        if (videos == null) return "";

        return new Gson().toJson(videos);
    }

    public static ArrayList<UriSample> UriSamplefromJson(String jsonVideos) {
        ArrayList<UriSample> videos;
        Gson gson = new Gson();
        videos = gson.fromJson(jsonVideos, new TypeToken<ArrayList<UriSample>>() {
        }.getType());
        return videos;
    }

    public static String UriSamplestoJson(ArrayList<UriSample> videos) {
        if (videos == null) return "";

        return new Gson().toJson(videos);
    }

    public static final class PlaylistSample extends Sample {

        public final UriSample[] children;

        public PlaylistSample(String name, UriSample... children) {
            super(name);
            this.children = children;
        }

        @Override
        public void addToIntent(Intent intent) {
//      intent.setAction(PlayerActivity.ACTION_VIEW_LIST);
//      for (int i = 0; i < children.length; i++) {
//        children[i].addToPlaylistIntent(intent, /* extrasKeySuffix= */ "_" + i);
//      }
        }
    }

    public static final class DrmInfo {

        public static DrmInfo createFromIntent(Intent intent, String extrasKeySuffix) {
            String schemeKey = DRM_SCHEME_EXTRA + extrasKeySuffix;
            String schemeUuidKey = DRM_SCHEME_UUID_EXTRA + extrasKeySuffix;
            if (!intent.hasExtra(schemeKey) && !intent.hasExtra(schemeUuidKey)) {
                return null;
            }
            String drmSchemeExtra =
                    intent.hasExtra(schemeKey)
                            ? intent.getStringExtra(schemeKey)
                            : intent.getStringExtra(schemeUuidKey);
            UUID drmScheme = Util.getDrmUuid(drmSchemeExtra);
            String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL_EXTRA + extrasKeySuffix);
            String[] keyRequestPropertiesArray =
                    intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix);
            String[] drmSessionForClearTypesExtra =
                    intent.getStringArrayExtra(DRM_SESSION_FOR_CLEAR_TYPES_EXTRA + extrasKeySuffix);
            int[] drmSessionForClearTypes = toTrackTypeArray(drmSessionForClearTypesExtra);
            boolean drmMultiSession =
                    intent.getBooleanExtra(DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, false);
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
            intent.putExtra(DRM_SCHEME_EXTRA + extrasKeySuffix, drmScheme.toString());
            intent.putExtra(DRM_LICENSE_URL_EXTRA + extrasKeySuffix, drmLicenseUrl);
            intent.putExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix, drmKeyRequestProperties);
            ArrayList<String> typeStrings = new ArrayList<>();
            for (int type : drmSessionForClearTypes) {
                // Only audio and video are supported.
                typeStrings.add(type == TRACK_TYPE_AUDIO ? "audio" : "video");
            }
            intent.putExtra(
                    DRM_SESSION_FOR_CLEAR_TYPES_EXTRA + extrasKeySuffix, typeStrings.toArray(new String[0]));
            intent.putExtra(DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, drmMultiSession);
        }
    }

    public static final class SubtitleInfo {

        @Nullable
        public static SubtitleInfo createFromIntent(Intent intent, String extrasKeySuffix) {
            if (!intent.hasExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix)) {
                return null;
            }
            return new SubtitleInfo(
                    Uri.parse(intent.getStringExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix)),
                    intent.getStringExtra(SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix),
                    intent.getStringExtra(SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix));
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
            intent.putExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix, uri.toString());
            intent.putExtra(SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix, mimeType);
            intent.putExtra(SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix, language);
        }
    }

    public static int[] toTrackTypeArray(@Nullable String[] trackTypeStringsArray) {
        if (trackTypeStringsArray == null) {
            return new int[0];
        }
        HashSet<Integer> trackTypes = new HashSet<>();
        for (String trackTypeString : trackTypeStringsArray) {
            switch (Util.toLowerInvariant(trackTypeString)) {
                case "audio":
                    trackTypes.add(TRACK_TYPE_AUDIO);
                    break;
                case "video":
                    trackTypes.add(TRACK_TYPE_VIDEO);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid track type: " + trackTypeString);
            }
        }
        return Util.toArray(new ArrayList<>(trackTypes));
    }

    public static Sample createFromIntent(Intent intent) {
        if (ACTION_VIEW_LIST.equals(intent.getAction())) {
            ArrayList<String> intentUris = new ArrayList<>();
            int index = 0;
            while (intent.hasExtra(URI_EXTRA + "_" + index)) {
                intentUris.add(intent.getStringExtra(URI_EXTRA + "_" + index));
                index++;
            }
            UriSample[] children = new UriSample[intentUris.size()];
            for (int i = 0; i < children.length; i++) {
                Uri uri = Uri.parse(intentUris.get(i));
                children[i] = UriSample.createFromIntent(uri, intent, /* extrasKeySuffix= */ "_" + i);
            }
            return new PlaylistSample(/* name= */ null, children);
        } else {
            return UriSample.createFromIntent(intent.getData(), intent, /* extrasKeySuffix= */ "");
        }
    }
}
