package com.github.warren_bank.rtsp_ipcam_viewer.mock.data;

import java.util.ArrayList;
import java.util.List;

public class SampleGroup {
    public final String title;
    public final List<Sample> samples;

    public SampleGroup(String title) {
        this.title = title;
        this.samples = new ArrayList<>();
    }
}
