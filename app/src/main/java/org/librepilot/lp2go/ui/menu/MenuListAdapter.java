/*
 * @file   MenuListAdapter.java
 * @author The LibrePilot Project, http://www.librepilot.org Copyright (C) 2016.
 * @see    The GNU Public License (GPL) Version 3
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.librepilot.lp2go.ui.menu;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.librepilot.lp2go.R;
import org.librepilot.lp2go.helper.H;

import java.util.ArrayList;

public class MenuListAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<MenuItem> menuItems;

    public MenuListAdapter(@NonNull Context context, @NonNull ArrayList<MenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems;
    }

    @Override
    public int getCount() {
        return menuItems.size();
    }

    @Override
    public Object getItem(int position) {
        return menuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View retview = convertView;
        if (retview == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            retview = mInflater.inflate(R.layout.drawer_list_item, null);
        }

        ImageView imgIcon = (ImageView) retview.findViewById(R.id.icon);
        TextView txtTitle = (TextView) retview.findViewById(R.id.title);

        final Bitmap b = H.drawableToBitmap(menuItems.get(position).getIcon(), 25, 25, context);

        imgIcon.setImageBitmap(b);

        txtTitle.setText(menuItems.get(position).getTitle());

        return retview;
    }
}