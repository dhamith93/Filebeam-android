package me.dhamith.filebeam;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import me.dhamith.filebeam.adapters.TransferAdapter;
import me.dhamith.filebeam.pojo.Transfer;

public class TransfersFragment extends Fragment {
    private TransferAdapter transferAdapter;
    private List<Integer> idxToMonitor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);
        idxToMonitor = new ArrayList<>();
        transferAdapter = new TransferAdapter(getContext());
        RecyclerView transferView = view.findViewById(R.id.transferList);
        transferView.setLayoutManager(new LinearLayoutManager(getContext()));
        transferView.setAdapter(transferAdapter);
        setHandler();
        return view;
    }

    private void timer() {
        for(int i = 0; i < Transfer.getTransfers().size(); i++) {
            if (idxToMonitor.contains(i)) {
                transferAdapter.notifyItemChanged(i);

                if (Transfer.getTransfers().get(i).getStatus().equals(Transfer.COMPLETED)
                        || Transfer.getTransfers().get(i).getStatus().equals(Transfer.CANCELED)
                        || Transfer.getTransfers().get(i).getStatus().equals(Transfer.ERROR)) {
                    idxToMonitor.remove((Integer) i);
                }
            } else if (Transfer.getTransfers().get(i).getStatus().equals(Transfer.STARTED)) {
                idxToMonitor.add(i);
            }
        }
    }

    public void setHandler() {
        final Handler handler = new Handler();
        final int delay = 1000 ; //1000 milliseconds = 1 sec

        handler.postDelayed(new Runnable(){
            public void run(){
                timer();
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    public void updateList() {
        transferAdapter.notifyDataSetChanged();
    }
}