package com.ppcrong.bletoolbox.uart.log;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
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
        holder.mTvData.setTextColor(LogManager.LOG_COLORS.get(level));
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
