package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // Firestore variables
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // Create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Firestore initialization
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Snapshot listener to keep ListView synced
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w("Firestore", "Listen failed.", error);
                return;
            }

            cityArrayList.clear();

            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    String cityName = doc.getString("city");
                    String province = doc.getString("province");
                    if (cityName != null && province != null) {
                        cityArrayList.add(new City(cityName, province));
                    }
                }
            }

            cityArrayAdapter.notifyDataSetChanged();
        });

        // Set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });

        // Long click to delete city
        cityListView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            City city = cityArrayAdapter.getItem(position);
            if (city != null) {
                deleteCity(city);
            }
            return true;
        });

    }

    @Override
    public void updateCity(City city, String title, String year) {
        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // Update Firestore: remove old document and add new
        deleteCity(city); // remove old
        addCity(city);    // add updated
    }

    @Override
    public void addCity(City city) {
        // Add to local list
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        // Add to Firestore
        Map<String, String> data = new HashMap<>();
        data.put("city", city.getName());
        data.put("province", city.getProvince());

        citiesRef.document(city.getName())
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City added: " + city.getName()))
                .addOnFailureListener(e -> Log.w("Firestore", "Error adding city", e));
    }

    private void deleteCity(City city) {
        // Remove from Firestore
        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted: " + city.getName()))
                .addOnFailureListener(e -> Log.w("Firestore", "Error deleting city", e));
    }
}