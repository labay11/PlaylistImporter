package com.alm.playlistimporter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by A. Labay on 05/01/17.
 * As part of the project PlaylistImporter.
 */

public abstract class BaseAdapter<T, VH extends BaseAdapter.BaseHolder<T>> extends RecyclerView.Adapter<VH> {

    private ArrayList<T> mItems = new ArrayList<>();

    protected LayoutInflater mInflater;

    private OnItemClickListener<T> mItemClickListener;

    public BaseAdapter(Context context, OnItemClickListener<T> itemClickListener) {
        mInflater = LayoutInflater.from(context);
        mItemClickListener = itemClickListener;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(mItems.get(position), mItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void add(@NonNull T item) {
        mItems.add(item);
        notifyItemInserted(mItems.size());
    }

    public void add(int pos, @NonNull T item) {
        if (!checkBounds(pos)) {
            pos = 0; // or throw exception?
        }
        mItems.add(pos, item);
        notifyItemInserted(pos);
    }

    public void addAll(@NonNull Collection<T> items) {
        mItems.addAll(items);
        notifyItemRangeInserted(mItems.size() - items.size(), mItems.size());
    }

    public void set(@NonNull T items) {
        mItems.clear();
        mItems.add(items);
        notifyDataSetChanged();
    }

    public void set(@NonNull Collection<T> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void move(int from, int to) {
        if (!checkBounds(from) || !checkBounds(to))
            throw new IndexOutOfBoundsException();

        T temp = mItems.get(from);
        mItems.set(from, mItems.get(to));
        mItems.set(to, temp);
        notifyItemMoved(from, to);
    }

    public void remove(int pos) {
        if (!checkBounds(pos))
            throw new IndexOutOfBoundsException();

        mItems.remove(pos);
        notifyItemRemoved(pos);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    private boolean checkBounds(int i) {
        return i >= 0 && i < mItems.size();
    }

    public static abstract class BaseHolder<T> extends RecyclerView.ViewHolder {

        public BaseHolder(View itemView) {
            super(itemView);
        }

        public abstract void bind(T item, OnItemClickListener<T> itemClickListener);
    }

    public interface OnItemClickListener<T> {

        void onItemClick(View itemView, int pos, T item);
    }
}
