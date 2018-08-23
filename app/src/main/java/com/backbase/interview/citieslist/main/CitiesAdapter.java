package com.backbase.interview.citieslist.main;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.models.entities.City;
import java.util.LinkedList;
import java.util.List;

public class CitiesAdapter extends RecyclerView.Adapter<CitiesAdapter.ViewHolder> implements
    Filterable {

  private final List<City> data = new LinkedList<>();
  private final List<City> dataCopy = new LinkedList<>();

  @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ViewHolder viewHolder = new ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city, parent, false));
    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {

      }
    });
    return viewHolder;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(data.get(position));
  }

  public void addAll(final List<City> cities) {
    final int lastIndex = getItemCount();
    data.addAll(cities);
    notifyItemRangeInserted(lastIndex, cities.size());

    dataCopy.clear();
    dataCopy.addAll(data);
  }


  public void filter(String text) {
    data.clear();
    if(text.isEmpty()){
      data.addAll(dataCopy);
    } else{
      text = text.toLowerCase();
      for(City item: dataCopy){
        if(item.name.toLowerCase().startsWith(text)){
          data.add(item);
        }
      }
    }
    notifyDataSetChanged();
  }

  @Override public int getItemCount() {
    return data.size();
  }

  @Override public Filter getFilter() {
    return new Filter() {
      @Override protected FilterResults performFiltering(CharSequence constraint) {
        return null;
      }

      @Override protected void publishResults(CharSequence constraint, FilterResults results) {

      }
    };
  }

  static class ViewHolder extends RecyclerView.ViewHolder{

    final TextView mNameTextView;
    final TextView mCountryTextView;

    ViewHolder(View itemView) {
      super(itemView);
      mNameTextView = itemView.findViewById(R.id.city_item_name_tv);
      mCountryTextView = itemView.findViewById(R.id.city_item_country_tv);

    }

    void bind(City city) {
      mNameTextView.setText(city.name);
      mCountryTextView.setText(city.country);
    }
  }
}
