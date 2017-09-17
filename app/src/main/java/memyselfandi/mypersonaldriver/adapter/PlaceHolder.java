package memyselfandi.mypersonaldriver.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import memyselfandi.mypersonaldriver.R;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class PlaceHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.cell_place_textview)
    TextView textView;

    public PlaceHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this,itemView);
    }
}
