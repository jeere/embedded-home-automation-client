package fi.oulu.tol.esde_2016_014.ohapclientesde_2016_014;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.opimobi.ohap.Container;
import com.opimobi.ohap.EventSource;
import com.opimobi.ohap.Item;

import java.util.List;

public class ContainerListAdapter extends DataSetObservable implements android.widget.ListAdapter, EventSource.Listener<Container,Item>{

    private Container container;
    private DataSetObservable dataSetObservable = new DataSetObservable();
    private static final String TAG = "ContainerListAdapter";

    public ContainerListAdapter(Container container) {
        Log.d(TAG, "Constructor");
        this.container = container;
        container.itemAddedEventSource.addListener(this);
        container.itemRemovedEventSource.addListener(this);
        Log.d(TAG, "Added container add & remove listeners");
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        Log.d(TAG, "registering observer");
        this.dataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        Log.d(TAG, "Unregistering observer");
        dataSetObservable.unregisterObserver(observer);
    }

    @Override
    public int getCount() {
        return container.getItemCount();
    }

    @Override
    public Object getItem(int position) {
        return container.getItemByIndex(position);
    }

    @Override
    public long getItemId(int position) {
        return container.getItemByIndex(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.rowTextView = (TextView)convertView.findViewById(R.id.rowTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.rowTextView.setText(container.getItemByIndex(position).getName());

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void onEvent(Container container, Item item) {
        Log.d("ContainerListAdapter", "RECEIVED Container -- :"+container);
        Log.d("ContainerListAdapter", "RECEIVED Item -- :"+item);
        dataSetObservable.notifyChanged();
    }

    private static class ViewHolder {
        public TextView rowTextView;
    }
}
