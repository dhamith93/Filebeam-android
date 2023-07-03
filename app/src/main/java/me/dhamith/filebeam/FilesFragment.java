package me.dhamith.filebeam;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.dhamith.filebeam.adapters.FileListAdapter;

public class FilesFragment extends Fragment {
    private FileListAdapter fileListAdapter;
    private int currentItemCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        fileListAdapter = new FileListAdapter(getContext());
        RecyclerView filesView = view.findViewById(R.id.filesList);
        filesView.setLayoutManager(new LinearLayoutManager(getContext()));
        filesView.setAdapter(fileListAdapter);
        currentItemCount = fileListAdapter.getItemCount();

        return view;
    }

    public void updateList() {
        if (fileListAdapter.getItemCount() != currentItemCount) {
            currentItemCount = fileListAdapter.getItemCount();
            fileListAdapter.notifyDataSetChanged();
        }
    }

    public void updateList(int idx) {
        fileListAdapter.notifyItemInserted(idx);
    }
}