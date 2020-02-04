package com.github.warren_bank.rtsp_ipcam_viewer.main.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.github.warren_bank.rtsp_ipcam_viewer.R;
import com.github.warren_bank.rtsp_ipcam_viewer.common.activities.ExitActivity;
import com.github.warren_bank.rtsp_ipcam_viewer.common.activities.FilePicker;
import com.github.warren_bank.rtsp_ipcam_viewer.common.data.VideoType;
import com.github.warren_bank.rtsp_ipcam_viewer.common.helpers.FileUtils;
import com.github.warren_bank.rtsp_ipcam_viewer.grid_view.activities.GridActivity;
import com.github.warren_bank.rtsp_ipcam_viewer.list_view.activities.ListActivity;
import com.github.warren_bank.rtsp_ipcam_viewer.main.dialogs.add_video.VideoDialog;
import com.github.warren_bank.rtsp_ipcam_viewer.main.dialogs.grid_view_columns.GridColumnsDialog;
import com.github.warren_bank.rtsp_ipcam_viewer.main.recycler_view.RecyclerViewCallback;
import com.github.warren_bank.rtsp_ipcam_viewer.main.recycler_view.VideoListAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/*
 * -------------------------------------
open 'add video' dialog
open list
open grid
read file
  import
  open list
  open grid

list items
  check: toggle video 'enabled' field
  click: open 'edit video' dialog
 * -------------------------------------
 */

public class MainActivity extends AppCompatActivity {

    private RecyclerView mList;
    private VideoListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activities_mainactivity);
        final Context context = this.getBaseContext();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationIcon(null);
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
        mAdapter = new VideoListAdapter(context, uris);
        final ItemTouchHelper helper = new ItemTouchHelper(new RecyclerViewCallback(mAdapter));
        mList.setLayoutManager(new LinearLayoutManager(context));
        mList.setHasFixedSize(true);
        mList.setAdapter(mAdapter);
        helper.attachToRecyclerView(mList);

        // add divider between list items
        mList.addItemDecoration(
                new DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activities_mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_add_video:
                VideoDialog.add(
                        MainActivity.this,
                        mList,
                        new VideoDialog.ResultListener() {
                            @Override
                            public void onResult(VideoType new_video) {
                                if (new_video != null) {
//                                    if (videos.add(new_video)) {
//                                        int position = videos.size() - 1;
//                                        mAdapter.notifyItemInserted(position);
//                                        VideoListAdapter.saveVideos(mAdapter);
//                                    }
                                }
                            }
                        }
                );
                return true;
            case R.id.action_open_list:
                ListActivity.open(MainActivity.this, null);
                return true;
            case R.id.action_open_grid_2col:
                GridActivity.open(MainActivity.this, null, 2);
                return true;
            case R.id.action_open_grid_Ncol:
                GridColumnsDialog.show(
                        MainActivity.this,
                        mList,
                        new GridColumnsDialog.ResultListener() {
                            @Override
                            public void onResult(int columns) {
                                if (columns > 1) {
                                    GridActivity.open(MainActivity.this, null, columns);
                                }
                            }
                        }
                );
                return true;
            case R.id.action_read_file:
                FilePicker.open(
                        /* activity= */ MainActivity.this,
                        /* listener= */ new FilePicker.ResultListener() {
                            @Override
                            public void onResult(String filepath) {
                                try {
                                    MainActivity self = MainActivity.this;
                                    String jsonVideos = FileUtils.getFileContents(filepath);

                                    // import
                                    ArrayList<VideoType> new_videos = VideoType.fromJson(jsonVideos);
//                                    int positionStart = self.videos.size();
//                                    int itemCount = new_videos.size();
//                                    self.videos.addAll(new_videos);
//                                    self.mAdapter.notifyItemRangeInserted(positionStart, itemCount);
//                                    jsonVideos = VideoType.toJson(self.videos);
//                                    SharedPrefs.setVideos(self, jsonVideos);
                                } catch (Exception e) {
                                }
                            }
                        },
                        /* showHiddenFiles=   */ true,
                        /* filterPattern=     */ ".*\\.(?:json|js|txt)$",
                        /* filterDirectories= */ false
                );
                return true;
            case R.id.action_exit:
                ExitActivity.open(MainActivity.this);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FilePicker.ResultHandler.onActivityResult(requestCode, resultCode, data);
    }
}
