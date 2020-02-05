package com.github.warren_bank.rtsp_ipcam_viewer.list_view.recycler_view;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspDefaultClient;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.rtsp.core.Client;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import android.content.Context;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.warren_bank.rtsp_ipcam_viewer.R;
import com.github.warren_bank.rtsp_ipcam_viewer.common.helpers.Utils;
import com.github.warren_bank.rtsp_ipcam_viewer.mock.data.Sample;
import com.github.warren_bank.rtsp_ipcam_viewer.mock.data.SampleGroup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

public final class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.RecyclerViewHolder> {
    private static final String TAG = "VideoAdapter";
    public Context context;
    public ArrayList<Sample> videos = new ArrayList<>();
    private int minHeight;

    public VideoAdapter(Context context, String[] uris, int minHeight) {
        super();
        this.context = context;
        this.minHeight = minHeight;
        setHasStableIds(true);
        SampleListLoader loaderTask = new SampleListLoader();
        loaderTask.execute(context, uris);
    }

    private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
        videos.clear();
        if (sawError) {

        } else {
            for (SampleGroup group : groups) {
                List<Sample> samples = group.samples;
                for (Sample sample : samples) {
                    videos.add(sample);
                }

            }
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return (videos == null) ? 0 : videos.size();
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_recycler_view_holder, parent, false);

        return new RecyclerViewHolder(view, minHeight);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        if (position >= getItemCount()) return;

        Sample video = videos.get(position);

        holder.bind(video);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerViewHolder holder) {
        holder.stop();
    }

    @Override
    public long getItemId(int position) {
        Sample video = videos.get(position);
        String url = video.name;
        long uniqueID = Utils.getUniqueLongFromString(url);

        return uniqueID;
    }

    public static final class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener, GestureDetector.OnGestureListener {

        private PlayerView view;
        private TextView title;
        private SimpleExoPlayer exoPlayer;
        private DefaultHttpDataSourceFactory dataSourceFactory;
        private GestureDetectorCompat gestureDetector;

        private Sample data;


        public RecyclerViewHolder(View view, int defaultHeight) {
            super(view);

            this.view = (PlayerView) view;
            this.title = (TextView) view.findViewById(R.id.exo_title);

            if (defaultHeight > 0) {
                this.view.setMinimumHeight(defaultHeight);
                this.title.setMaxHeight(defaultHeight);

                if (this.title.getTextSize() > defaultHeight) {
                    this.title.setTextSize(
                            (defaultHeight > 10)
                                    ? (float) (defaultHeight - 2)
                                    : 0f
                    );
                }
            }

            Context context = view.getContext();
            DefaultTrackSelector trackSelector = new DefaultTrackSelector();
            RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
            this.exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector);

            String userAgent = context.getResources().getString(R.string.user_agent);
            this.dataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);

            this.gestureDetector = new GestureDetectorCompat(context, this);
            this.gestureDetector.setIsLongpressEnabled(true);

            this.view.setOnTouchListener(this);
            this.view.setUseController(false);
            this.view.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
            this.view.setPlayer(this.exoPlayer);

            this.exoPlayer.setVolume(0f);  // mute all videos in list view
        }

        public void bind(Sample data) {
            this.data = data;
            this.title.setText(data.name);
            Uri uri;
            MediaSource mediaSource;
            if (this.data instanceof Sample.PlaylistSample) {
                Log.e(TAG, "Show is PlaylistSample !! has child : " + ((Sample.PlaylistSample) this.data).children.length);
            } else if (this.data instanceof Sample.UriSample) {
                stop();
                uri = ((Sample.UriSample) data).uri;
                if (Util.isRtspUri(uri)) {
                    Log.e(TAG, "Show  is RTSP uri : " + uri);
                    mediaSource = new RtspMediaSource.Factory(RtspDefaultClient.factory()
                            .setFlags(Client.FLAG_ENABLE_RTCP_SUPPORT)
                            .setNatMethod(Client.RTSP_NAT_DUMMY)
                            .setPlayer(exoPlayer))
                            .createMediaSource(uri);
                    exoPlayer.prepare(mediaSource);
                    play();
                } else {
                    Log.e(TAG, "Show  Extractor uri : " + uri);
                    List<MediaSource> mediaSources = new ArrayList<>();
                    Sample.UriSample sample = (Sample.UriSample) data;
                    mediaSource = createLeafMediaSource(sample);
                    Sample.SubtitleInfo subtitleInfo = sample.subtitleInfo;
                    if (subtitleInfo != null) {
                        Format subtitleFormat =
                                Format.createTextSampleFormat(
                                        /* id= */ null,
                                        subtitleInfo.mimeType,
                                        C.SELECTION_FLAG_DEFAULT,
                                        subtitleInfo.language);
                        MediaSource subtitleMediaSource =
                                new SingleSampleMediaSource.Factory(dataSourceFactory)
                                        .createMediaSource(subtitleInfo.uri, subtitleFormat, C.TIME_UNSET);
                        mediaSource = new MergingMediaSource(mediaSource, subtitleMediaSource);
                    }
                    mediaSources.add(mediaSource);
//                    if (seenAdsTagUri && mediaSources.size() == 1) {
//                        Uri adTagUri = samples[0].adTagUri;
//                        if (!adTagUri.equals(loadedAdTagUri)) {
//                            releaseAdsLoader();
//                            loadedAdTagUri = adTagUri;
//                        }
//                        MediaSource adsMediaSource = createAdsMediaSource(mediaSources.get(0), adTagUri);
//                        if (adsMediaSource != null) {
//                            mediaSources.set(0, adsMediaSource);
//                        } else {
//                            showToast(R.string.ima_not_loaded);
//                        }
//                    } else if (seenAdsTagUri && mediaSources.size() > 1) {
//                        showToast(R.string.unsupported_ads_in_concatenation);
//                        releaseAdsLoader();
//                    } else {
//                        releaseAdsLoader();
//                    }
//                    exoPlayer.prepare(mediaSource);
//                    play();
                }

            }
        }

        private MediaSource createLeafMediaSource(Sample.UriSample parameters) {
            Sample.DrmInfo drmInfo = parameters.drmInfo;
//            int errorStringId = R.string.error_drm_unknown;
            DrmSessionManager<ExoMediaCrypto> drmSessionManager = null;
            if (drmInfo == null) {
                drmSessionManager = DrmSessionManager.getDummyDrmSessionManager();
            } else if (Util.SDK_INT < 18) {
//                errorStringId = R.string.error_drm_unsupported_before_api_18;
            } else if (!MediaDrm.isCryptoSchemeSupported(drmInfo.drmScheme)) {
//                errorStringId = R.string.error_drm_unsupported_scheme;
            } else {
                MediaDrmCallback mediaDrmCallback = createMediaDrmCallback(drmInfo.drmLicenseUrl, drmInfo.drmKeyRequestProperties);
                drmSessionManager =
                        new DefaultDrmSessionManager.Builder()
                                .setUuidAndExoMediaDrmProvider(drmInfo.drmScheme, FrameworkMediaDrm.DEFAULT_PROVIDER)
                                .setMultiSession(drmInfo.drmMultiSession)
                                .setUseDrmSessionsForClearContent(drmInfo.drmSessionForClearTypes)
                                .build(mediaDrmCallback);
            }

            if (drmSessionManager == null) {
//                showToast(errorStringId);
//                finish();
                return null;
            }

//            DownloadRequest downloadRequest = ((DemoApplication) getApplication())
//                            .getDownloadTracker()
//                            .getDownloadRequest(parameters.uri);
//            if (downloadRequest != null) {
//                return DownloadHelper.createMediaSource(downloadRequest, dataSourceFactory);
//            }
            return createLeafMediaSource(parameters.uri, parameters.extension, drmSessionManager);
        }

        private MediaSource createLeafMediaSource(Uri uri, String extension, DrmSessionManager<?> drmSessionManager) {
            @C.ContentType int type = Util.inferContentType(uri, extension);
            switch (type) {
                case C.TYPE_DASH:
                    return new DashMediaSource.Factory(dataSourceFactory)
                            .setDrmSessionManager(drmSessionManager)
                            .createMediaSource(uri);
                case C.TYPE_SS:
                    return new SsMediaSource.Factory(dataSourceFactory)
                            .setDrmSessionManager(drmSessionManager)
                            .createMediaSource(uri);
                case C.TYPE_HLS:
                    return new HlsMediaSource.Factory(dataSourceFactory)
                            .setDrmSessionManager(drmSessionManager)
                            .createMediaSource(uri);
                case C.TYPE_OTHER:
                    return new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setDrmSessionManager(drmSessionManager)
                            .createMediaSource(uri);
                default:
                    throw new IllegalStateException("Unsupported type: " + type);
            }
        }

        private HttpMediaDrmCallback createMediaDrmCallback(
                String licenseUrl, String[] keyRequestPropertiesArray) {
            HttpDataSource.Factory licenseDataSourceFactory = buildHttpDataSourceFactory();
            HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
            if (keyRequestPropertiesArray != null) {
                for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                    drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                            keyRequestPropertiesArray[i + 1]);
                }
            }
            return drmCallback;
        }

        public HttpDataSource.Factory buildHttpDataSourceFactory() {
            return new DefaultHttpDataSourceFactory("ExoPlayerDemo");
        }

        public void play() {
            try {
                exoPlayer.setPlayWhenReady(true);
            } catch (Exception e) {
            }
        }

        public void pause() {
            try {
                exoPlayer.setPlayWhenReady(false);
            } catch (Exception e) {
            }
        }

        public void stop() {
            try {
                exoPlayer.stop(true);
            } catch (Exception e) {
            }
        }

        public void release() {
            try {
                exoPlayer.release();
            } catch (Exception e) {
            }
        }

        // open selected video in fullscreen view
        private void doOnClick() {
//            VideoActivity.open(
//                    view.getContext(),
//                    (data.URL_high_res != null) ? data.URL_high_res : data.URL_low_res
//            );
        }

        // toggle play/pause of selected video
        private void doOnLongClick() {
            try {
                exoPlayer.setPlayWhenReady(
                        !exoPlayer.getPlayWhenReady()
                );
            } catch (Exception e) {
            }
        }

        // interface: View.OnTouchListener

        public boolean onTouch(View v, MotionEvent e) {
            return this.gestureDetector.onTouchEvent(e);
        }

        // interface: GestureDetector.OnGestureListener

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            doOnClick();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            doOnLongClick();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }
    }

    private final class SampleListLoader extends AsyncTask<Object, Void, List<SampleGroup>> {
        private static final String TAG = "SampleListLoader";
        private boolean sawError;

        @Override
//    protected List<SampleGroup> doInBackground(String... uris) {
        protected List<SampleGroup> doInBackground(Object... params) {
            List<SampleGroup> result = new ArrayList<>();
//      Context context = getApplicationContext();
            Context context = (Context) params[0];
            String[] uris = (String[]) params[1];


            String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
            DataSource dataSource =
                    new DefaultDataSource(context, userAgent, /* allowCrossProtocolRedirects= */ false);
            for (String uri : uris) {
                DataSpec dataSpec = new DataSpec(Uri.parse(uri));
                InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
                try {
                    readSampleGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sample list: " + uri, e);
                    sawError = true;
                } finally {
                    Util.closeQuietly(dataSource);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<SampleGroup> result) {
            onSampleGroups(result, sawError);
        }

        private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                readSampleGroup(reader, groups);
            }
            reader.endArray();
        }

        private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
            String groupName = "";
            ArrayList<Sample> samples = new ArrayList<>();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        groupName = reader.nextString();
                        break;
                    case "samples":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            samples.add(readEntry(reader, false));
                        }
                        reader.endArray();
                        break;
                    case "_comment":
                        reader.nextString(); // Ignore.
                        break;
                    default:
                        throw new ParserException("Unsupported name: " + name);
                }
            }
            reader.endObject();

            SampleGroup group = getGroup(groupName, groups);
            group.samples.addAll(samples);
        }

        private Sample readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
            String sampleName = null;
            Uri uri = null;
            String extension = null;
            boolean isLive = false;
            String drmScheme = null;
            String drmLicenseUrl = null;
            String[] drmKeyRequestProperties = null;
            String[] drmSessionForClearTypes = null;
            boolean drmMultiSession = false;
            ArrayList<Sample.UriSample> playlistSamples = null;
            String adTagUri = null;
            String sphericalStereoMode = null;
            List<Sample.SubtitleInfo> subtitleInfos = new ArrayList<>();
            Uri subtitleUri = null;
            String subtitleMimeType = null;
            String subtitleLanguage = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        sampleName = reader.nextString();
                        break;
                    case "uri":
                        uri = Uri.parse(reader.nextString());
                        break;
                    case "extension":
                        extension = reader.nextString();
                        break;
                    case "drm_scheme":
                        drmScheme = reader.nextString();
                        break;
                    case "is_live":
                        isLive = reader.nextBoolean();
                        break;
                    case "drm_license_url":
                        drmLicenseUrl = reader.nextString();
                        break;
                    case "drm_key_request_properties":
                        ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            drmKeyRequestPropertiesList.add(reader.nextName());
                            drmKeyRequestPropertiesList.add(reader.nextString());
                        }
                        reader.endObject();
                        drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
                        break;
                    case "drm_session_for_clear_types":
                        ArrayList<String> drmSessionForClearTypesList = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            drmSessionForClearTypesList.add(reader.nextString());
                        }
                        reader.endArray();
                        drmSessionForClearTypes = drmSessionForClearTypesList.toArray(new String[0]);
                        break;
                    case "drm_multi_session":
                        drmMultiSession = reader.nextBoolean();
                        break;
                    case "playlist":
                        Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
                        playlistSamples = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            playlistSamples.add((Sample.UriSample) readEntry(reader, /* insidePlaylist= */ true));
                        }
                        reader.endArray();
                        break;
                    case "ad_tag_uri":
                        adTagUri = reader.nextString();
                        break;
                    case "spherical_stereo_mode":
                        Assertions.checkState(
                                !insidePlaylist, "Invalid attribute on nested item: spherical_stereo_mode");
                        sphericalStereoMode = reader.nextString();
                        break;
                    case "subtitle_uri":
                        subtitleUri = Uri.parse(reader.nextString());
                        break;
                    case "subtitle_mime_type":
                        subtitleMimeType = reader.nextString();
                        break;
                    case "subtitle_language":
                        subtitleLanguage = reader.nextString();
                        break;
                    default:
                        throw new ParserException("Unsupported attribute name: " + name);
                }
            }
            reader.endObject();
            Sample.DrmInfo drmInfo =
                    drmScheme == null
                            ? null
                            : new Sample.DrmInfo(
                            Util.getDrmUuid(drmScheme),
                            drmLicenseUrl,
                            drmKeyRequestProperties,
                            Sample.toTrackTypeArray(drmSessionForClearTypes),
                            drmMultiSession);
            Sample.SubtitleInfo subtitleInfo =
                    subtitleUri == null
                            ? null
                            : new Sample.SubtitleInfo(
                            subtitleUri,
                            Assertions.checkNotNull(
                                    subtitleMimeType, "subtitle_mime_type is required if subtitle_uri is set."),
                            subtitleLanguage);
            if (playlistSamples != null) {
                Sample.UriSample[] playlistSamplesArray = playlistSamples.toArray(new Sample.UriSample[0]);
                return new Sample.PlaylistSample(sampleName, playlistSamplesArray);
            } else {
                return new Sample.UriSample(
                        sampleName, true,
                        uri,
                        extension,
                        isLive,
                        drmInfo,
                        adTagUri != null ? Uri.parse(adTagUri) : null,
                        sphericalStereoMode,
                        subtitleInfo);
            }
        }

        private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
            for (int i = 0; i < groups.size(); i++) {
                if (Util.areEqual(groupName, groups.get(i).title)) {
                    return groups.get(i);
                }
            }
            SampleGroup group = new SampleGroup(groupName);
            groups.add(group);
            return group;
        }

    }
}
