package com.github.warren_bank.rtsp_ipcam_viewer.main.recycler_view;

import com.github.warren_bank.rtsp_ipcam_viewer.common.data.C;
import com.github.warren_bank.rtsp_ipcam_viewer.mock.data.Sample;

import java.util.ArrayList;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public final class RecyclerViewCallback extends ItemTouchHelper.SimpleCallback {

    private VideoListAdapter adapter;
    private ArrayList<Sample> videos;

    private int draggingFromPosition;
    private int draggingToPosition;

    public RecyclerViewCallback(VideoListAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.START | ItemTouchHelper.END);
        this.adapter              = adapter;
        this.videos               = adapter.videos;
        this.draggingFromPosition = C.INDEX_UNSET;
        this.draggingToPosition   = C.INDEX_UNSET;
    }

    @Override
    public boolean onMove(RecyclerView list, RecyclerView.ViewHolder origin, RecyclerView.ViewHolder target) {
        int fromPosition = origin.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        if (draggingFromPosition == C.INDEX_UNSET) {
            draggingFromPosition = fromPosition;
        }
        draggingToPosition = toPosition;
        adapter.notifyItemMoved(fromPosition, toPosition);
        VideoListAdapter.saveVideos(adapter);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        if (position >= videos.size()) return;

        videos.remove(position);
        adapter.notifyItemRemoved(position);
        VideoListAdapter.saveVideos(adapter);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (
            (draggingFromPosition < videos.size()) &&
            (draggingToPosition   < videos.size()) &&
            (draggingFromPosition != C.INDEX_UNSET)
        ) {
            videos.add(draggingToPosition, videos.remove(draggingFromPosition));
            adapter.notifyDataSetChanged();
            VideoListAdapter.saveVideos(adapter);
        }
        draggingFromPosition = C.INDEX_UNSET;
        draggingToPosition = C.INDEX_UNSET;
    }
}
