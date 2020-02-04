package com.github.warren_bank.rtsp_ipcam_viewer.main.recycler_view;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.github.warren_bank.rtsp_ipcam_viewer.R;
import com.github.warren_bank.rtsp_ipcam_viewer.common.data.SharedPrefs;
import com.github.warren_bank.rtsp_ipcam_viewer.mock.data.Sample;
import com.github.warren_bank.rtsp_ipcam_viewer.mock.data.SampleGroup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public final class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.RecyclerViewHolder> {
    public Context context;
    public ArrayList<Sample> videos = new ArrayList<>();

    public VideoListAdapter(Context context, String[] uris) {
        super();
        this.context = context;
        SampleListLoader loaderTask = new SampleListLoader();
        loaderTask.execute(context, uris);
    }

    @Override
    public int getItemCount() {
        return (videos == null) ? 0 : videos.size();
    }

    // helper

    public static void saveVideos(VideoListAdapter adapter) {
        String jsonVideos = Sample.toJson(adapter.videos);
        SharedPrefs.setVideos(adapter.context, jsonVideos);
    }

    private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
        videos.clear();
        if (sawError) {

        } else {
            for (SampleGroup group : groups) {
                videos.addAll(group.samples);
            }
            notifyDataSetChanged();
        }
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.main_recycler_view_holder, parent, false);
        return new RecyclerViewHolder(view, parent, VideoListAdapter.this);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        if (position >= getItemCount()) return;
        Sample video = videos.get(position);
        holder.bind(video);
    }

    public final class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private View view;
        private CheckBox check;
        private TextView text;

        private ViewGroup viewGroup;
        private VideoListAdapter adapter;

        private Sample data;

        public RecyclerViewHolder(View view, ViewGroup viewGroup, VideoListAdapter adapter) {
            super(view);

            this.view = view;
            this.check = view.findViewById(R.id.check1);
            this.text = view.findViewById(R.id.text1);

            this.viewGroup = viewGroup;
            this.adapter = adapter;

            check.setOnClickListener(this);
            text.setOnClickListener(this);
        }

        public void bind(Sample data) {
            this.data = data;

            check.setChecked(data.is_enabled);
            text.setText(data.name, TextView.BufferType.NORMAL);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            if (view == check) {
                data.is_enabled = (!data.is_enabled);
                check.setChecked(data.is_enabled);
                adapter.notifyItemChanged(position);
                saveVideos(adapter);
                return;
            }

            if (view == text) {
//                VideoDialog.edit(
//                        adapter.context,
//                        viewGroup,
//                        data,
//                        new VideoDialog.ResultListener() {
//                            @Override
//                            public void onResult(boolean is_edited) {
//                                if (is_edited) {
//                                    adapter.notifyItemChanged(position);
//                                    saveVideos(adapter);
//                                }
//                            }
//                        }
//                );
                return;
            }
        }
    }

    private final class SampleListLoader extends AsyncTask<Object, Void, List<SampleGroup>> {
        private static final String TAG = "SampleListLoader";
        private boolean sawError;

        @Override
        protected List<SampleGroup> doInBackground(Object... params) {
            List<SampleGroup> result = new ArrayList<>();
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
