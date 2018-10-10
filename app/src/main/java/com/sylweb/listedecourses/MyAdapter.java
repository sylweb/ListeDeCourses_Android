package com.sylweb.listedecourses;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by sylvain on 10/10/2018.
 */

public class MyAdapter extends BaseAdapter implements View.OnClickListener{

    private Context context;
    public ArrayList<Article> data;
    private static LayoutInflater inflater = null;

    public MyAdapter(Context context, ArrayList<Article> data) {
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null) vi = inflater.inflate(R.layout.item, parent, false);

        Article a = (Article) getItem(position);

        TextView name = vi.findViewById(R.id.itemName);
        name.setText(a.name);

        TextView qty = vi.findViewById(R.id.itemQuantity);
        qty.setText(String.valueOf(a.quantity));

        ImageView i = vi.findViewById(R.id.binButton);
        i.setTag(a);
        i.setOnClickListener(this);

        return vi;
    }

    @Override
    public void onClick(View v) {
        String query = "UPDATE article SET deleted=1, modified_on=%d WHERE id LIKE '%s'";
        query = String.format(query, Calendar.getInstance().getTimeInMillis(), ((Article)v.getTag()).id);
        DBManager.executeQuery(query);
        Intent i = new Intent("AskForUpdate");
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }
}
