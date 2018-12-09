package com.example.mchabi.horairebus_cw;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;

public class ListeBus extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView recycler_view;
    private RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_bus);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);

        ArrayList list = dbHelper.getBusRoutesFromDatabase();

        recycler_view = findViewById(R.id.recycler_view);
        recycler_view.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_view.setLayoutManager(layoutManager);
        adapter = new ListeBusAdapter(list);
        recycler_view.setAdapter(adapter);

    }

}
