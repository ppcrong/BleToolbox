package com.ppcrong.bletoolbox.uart.log;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.socks.library.KLog;

import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * LogListAdapter for Log RecyclerView
 */

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogViewHolder> {

    private static final SparseIntArray mColors = new SparseIntArray();

    static {
        mColors.put(Level.DEBUG, 0xFF009CDE);
        mColors.put(Level.VERBOSE, 0xFFB8B056);
        mColors.put(Level.INFO, Color.BLACK);
        mColors.put(Level.APPLICATION, 0xFF238C0F);
        mColors.put(Level.WARNING, 0xFFD77926);
        mColors.put(Level.ERROR, Color.RED);
    }

    /**
     * A helper class that contains predefined static level values:
     * <ul>
     * <li>{@link #DEBUG}</li>
     * <li>{@link #VERBOSE}</li>
     * <li>{@link #INFO}</li>
     * <li>{@link #APPLICATION}</li>
     * <li>{@link #WARNING}</li>
     * <li>{@link #ERROR}</li>
     * </ul>
     */
    public final class Level {
        /** Level used just for debugging purposes. It has lowest level */
        public final static int DEBUG = 0;
        /** Log entries with minor importance */
        public final static int VERBOSE = 1;
        /** Default logging level for important entries */
        public final static int INFO = 5;
        /** Log entries level for applications */
        public final static int APPLICATION = 10;
        /** Log entries with high importance */
        public final static int WARNING = 15;
        /** Log entries with very high importance, like errors */
        public final static int ERROR = 20;

        private Level() {
            // empty
        }
    }

    private Context mContext;
    private CopyOnWriteArrayList<LogData> mLogDataList;

    public LogListAdapter(Context context, CopyOnWriteArrayList<LogData> list) {
        this.mContext = context;
        this.mLogDataList = list;
    }

    @Override
    public LogListAdapter.LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View hrItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
        return new LogViewHolder(hrItem);
    }

    @Override
    public void onBindViewHolder(LogListAdapter.LogViewHolder holder, final int position) {
        KLog.i("position: " + position);

        LogData data = mLogDataList.get(position);

        // Level
        final int level = data.getLevel();

        // Time
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(data.getTime());
        holder.mTvTime.setText(mContext.getString(R.string.log_time, calendar));

        // Data and text color
        holder.mTvData.setText(data.getData());
        holder.mTvData.setTextColor(mColors.get(level));
    }

    @Override
    public int getItemCount() {
        return mLogDataList.size();
    }

    public class LogViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.time)
        TextView mTvTime;
        @BindView(R.id.data)
        TextView mTvData;

        public LogViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}
