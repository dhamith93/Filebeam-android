package me.dhamith.filebeam;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import me.dhamith.filebeam.adapters.DeviceListAdapter;
import me.dhamith.filebeam.helpers.System;

public class DevicesFragment extends Fragment {
    private String port;
    private RecyclerView devicesView;
    private DeviceListAdapter deviceListAdapter;
    private List<String> devices;
    SwipeRefreshLayout swipeRefreshLayout;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        port = "9292";
        devices = new ArrayList<>();
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        deviceListAdapter = new DeviceListAdapter(getContext(), devices);
        lookForDevices();

        devicesView = view.findViewById(R.id.deviceList);
        devicesView.setLayoutManager(new LinearLayoutManager(getContext()));
        devicesView.setAdapter(deviceListAdapter);

        swipeRefreshLayout = view.findViewById(R.id.devicesSwipeLayout);
        swipeRefreshLayout.setOnRefreshListener(this::lookForDevices);

        return view;
    }

    private void lookForDevices() {
        for (List<Integer> ip : System.getLocalIPsAsInt()) {
            int finalOctet = ip.get(3);
            ip.remove(3);
            String ipStr = String.join(".", ip.stream().map(Object::toString).toArray(String[]::new));
            new Thread(() -> {
                for (int i = finalOctet - 1; i > 0; i -= 1) {
                    String host = ipStr + "." + i;
                    checkIfServiceUp(host);
                }
            }).start();
            new Thread(() -> {
                for (int i = finalOctet + 1; i < 255; i += 1) {
                    String host = ipStr + "." + i;
                    checkIfServiceUp(host);
                }
            }).start();
        }

        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void checkIfServiceUp(String host) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (System.isUp(host + ":" + port) && !devices.contains(host)) {
                devices.add(host);
                final int idx = devices.lastIndexOf(host);
                getActivity().runOnUiThread(() -> deviceListAdapter.notifyItemInserted(idx));
            }
        });
    }
}