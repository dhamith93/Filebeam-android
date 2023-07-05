package me.dhamith.filebeam.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import me.dhamith.filebeam.R;
import me.dhamith.filebeam.helpers.System;
import me.dhamith.filebeam.pojo.File;
import me.dhamith.filebeam.pojo.Transfer;

public class TransferAdapter extends RecyclerView.Adapter<TransferAdapter.TransferViewHolder> {

    private Context context;

    public TransferAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public TransferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TransferAdapter.TransferViewHolder(LayoutInflater.from(context).inflate(R.layout.transfer_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TransferViewHolder holder, int position) {
        Transfer transfer = Transfer.getTransfers().get(position);
        String size = System.getSimplifiedFileSize(transfer.getFile().getSize());
        holder.host.setText(transfer.getHost());
        holder.fileName.setText(transfer.getFile().getName());
        holder.fileType.setText(transfer.getFile().getType());
        holder.fileSize.setText(size);
        holder.status.setText(transfer.getStatus());
        if (transfer.getStatus().equals(Transfer.STARTED) || transfer.getStatus().equals(Transfer.COMPLETED)) {
            long endTime = transfer.getEndTime() != -1 ? transfer.getEndTime() : (java.lang.System.currentTimeMillis() / 1000L);
            float speed = System.getSpeed(transfer.getStartTime(), endTime, transfer.getCompletedBytes());
            holder.time.setText(System.handleTimeString(transfer.getFile().getSize(), transfer.getCompletedBytes(), transfer.getStartTime(), endTime, speed));
            String fileSizeStr = System.getSimplifiedFileSize(transfer.getCompletedBytes()) + " / " + size + "  " + System.getSimplifiedFileSize((long)speed) + "/s";
            holder.fileSize.setText(fileSizeStr);
        }
        if (transfer.getStatus().equals(Transfer.COMPLETED)) {
            holder.progress.setProgress(100);
        }
        holder.cancelTransfer.setBackgroundResource(R.drawable.outline_remove_circle_outline_20);
        if (transfer.getType().equals(Transfer.DOWNLOAD)) {
            holder.icon.setBackgroundResource(R.drawable.outline_file_download_48);
        } else {
            holder.icon.setBackgroundResource(R.drawable.outline_file_upload_48);
        }
        if (transfer.getStatus().equals(Transfer.STARTED) || transfer.getStatus().equals(Transfer.ERROR)) {
            holder.progress.setVisibility(View.VISIBLE);
            holder.progress.setProgress((int) ((transfer.getCompletedBytes() / (double) transfer.getFile().getSize()) * 100), true);
        }
    }

    @Override
    public int getItemCount() {
        return Transfer.getTransfers().size();
    }

    public class TransferViewHolder extends RecyclerView.ViewHolder {
        private TextView fileName;
        private TextView fileType;
        private TextView fileSize;
        private TextView host;
        private TextView time;
        private TextView status;
        private ProgressBar progress;
        private Button cancelTransfer;
        private ImageView icon;

        public TransferViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.lblTxFileName);
            fileType = itemView.findViewById(R.id.lblTxFileType);
            fileSize = itemView.findViewById(R.id.lblTxFileSize);
            host = itemView.findViewById(R.id.lblTxHost);
            time = itemView.findViewById(R.id.lblTxTime);
            status = itemView.findViewById(R.id.lblStatus);
            cancelTransfer = itemView.findViewById(R.id.btnCancelTransfer);
            progress = itemView.findViewById(R.id.prgTxProgress);
            icon = itemView.findViewById(R.id.imgTransferIcon);

            progress.setMax(100);

            cancelTransfer.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                if (Transfer.getTransfers().get(pos).getStatus().equals(Transfer.COMPLETED)
                        || Transfer.getTransfers().get(pos).getStatus().equals(Transfer.CANCELED)
                        || Transfer.getTransfers().get(pos).getStatus().equals(Transfer.ERROR)) {
                    return;
                }
                Transfer.getTransfers().get(pos).setStatus(Transfer.CANCELED);
                notifyItemChanged(pos);
            });
        }
    }
}
