/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ppcrong.bletoolbox.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.socks.library.KLog;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder> {

    private static final String CATEGORY = "com.ppcrong.bletoolbox.LAUNCHER";

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final LayoutInflater mInflater;
    private final List<ResolveInfo> mApplications;

    public FeatureAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);

        final PackageManager pm = mPackageManager = context.getPackageManager();
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(CATEGORY);

        final List<ResolveInfo> appList = mApplications = pm.queryIntentActivities(intent, 0);
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
//        appList.add(appList.get(0));
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));
    }

    @NonNull
    @Override
    public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feature_icon, parent, false);
        return new FeatureViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
        KLog.i("position: " + position);

        final ResolveInfo info = mApplications.get(position);
        final PackageManager pm = mPackageManager;

        holder.icon.setImageDrawable(info.loadIcon(pm));
        holder.label.setText(info.loadLabel(pm).toString().toUpperCase(Locale.US));
        holder.view.setOnClickListener(v -> {
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mApplications.size();
    }

    public class FeatureViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.view)
        View view;
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.label)
        TextView label;

        public FeatureViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}
