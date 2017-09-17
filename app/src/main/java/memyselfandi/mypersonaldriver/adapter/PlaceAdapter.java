package memyselfandi.mypersonaldriver.adapter;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import memyselfandi.mypersonaldriver.R;
import memyselfandi.mypersonaldriver.data.Place;
import memyselfandi.mypersonaldriver.utils.OnRecyclerItemClickListener;
import memyselfandi.mypersonaldriver.utils.PlaceHelper;
import timber.log.Timber;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class PlaceAdapter extends RecyclerView.Adapter<PlaceHolder> implements View.OnClickListener {

    private Place[] places = null;
    private LayoutInflater layoutInflater = null;
    private OnRecyclerItemClickListener recyclerItemClickListener;

    public PlaceAdapter(@NonNull Context ctx, OnRecyclerItemClickListener listener) {
        places = PlaceHelper.getInstance().getPlaces();
        layoutInflater = LayoutInflater.from(ctx);
        recyclerItemClickListener = listener;
    }

    @Override
    public PlaceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlaceHolder(layoutInflater.inflate(R.layout.cell_place,parent,false));
    }

    @Override
    public void onBindViewHolder(PlaceHolder holder, int position) {
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(this);
        holder.textView.setText(places[position].getDescription());
    }

    @Override
    public int getItemCount() {
        return places == null ? 0 : places.length;
    }

    @Override
    public void onClick(View v) {
        if(v == null) return;

        int position = (int) v.getTag();
        if(recyclerItemClickListener != null) {
            recyclerItemClickListener.onItemClickListener(v,position);
        }
    }

    public Place getItem(@IntRange(from=0) int position) {
        try {
            return places[position];
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    public void updatePlaces() {
        places = PlaceHelper.getInstance().getPlaces();
        notifyDataSetChanged();
    }
}
