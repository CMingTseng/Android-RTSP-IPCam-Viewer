package com.github.warren_bank.rtsp_ipcam_viewer.list_view.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;

import com.github.warren_bank.rtsp_ipcam_viewer.R;
import com.github.warren_bank.rtsp_ipcam_viewer.common.data.VideoType;
import com.github.warren_bank.rtsp_ipcam_viewer.list_view.recycler_view.RecyclerViewCallback;
import com.github.warren_bank.rtsp_ipcam_viewer.list_view.recycler_view.VideoAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public final class ListActivity extends AppCompatActivity {
    private static final String EXTRA_JSON_VIDEOS = "JSON_VIDEOS";

    private static ArrayList<VideoType> videos;

    private RecyclerView mList;
    private VideoAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_view_activities_listactivity);
        final Context context = this.getBaseContext();
//        Intent intent = getIntent();

//        this.videos = (intent.hasExtra(EXTRA_JSON_VIDEOS))
//            ? VideoType.fromJson(intent.getStringExtra(EXTRA_JSON_VIDEOS))
//            : VideoType.filterByEnabled(SharedPrefs.getVideos(this))
//        ;
        ArrayList<String> uriList = new ArrayList<>();
        AssetManager assetManager = getAssets();
        try {
            for (String asset : assetManager.list("")) {
                if (asset.endsWith(".exolist.json")) {
                    uriList.add("asset:///" + asset);
                }
            }
        } catch (IOException e) {

        }
        String[] uris = new String[uriList.size()];
        uriList.toArray(uris);
        Arrays.sort(uris);
        this.mList = (RecyclerView) findViewById(R.id.recycler_view);
        this.mAdapter = new VideoAdapter(context, uris, 150);
        final ItemTouchHelper helper = new ItemTouchHelper(new RecyclerViewCallback(this.mAdapter));
        this.mList.setLayoutManager(new LinearLayoutManager(context));
        this.mList.setHasFixedSize(true);
//        this.mAdapter = RecyclerViewInit.adapter(this, this.mList, this.videos);

        helper.attachToRecyclerView(this.mList);

        // add divider between list items
        this.mList.addItemDecoration(
                new DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mList.setAdapter(null);
    }

    public static void open(Context context, String jsonVideos) {
        Intent intent = new Intent(context, ListActivity.class);
        if ((jsonVideos != null) && (!jsonVideos.isEmpty())) {
            intent.putExtra(EXTRA_JSON_VIDEOS, jsonVideos);
        }
        context.startActivity(intent);
    }

}
