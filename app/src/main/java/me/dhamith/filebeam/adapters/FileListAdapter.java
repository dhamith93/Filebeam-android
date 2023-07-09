package me.dhamith.filebeam.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import me.dhamith.filebeam.R;
import me.dhamith.filebeam.helpers.System;
import me.dhamith.filebeam.pojo.File;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
    private Context context;

    public FileListAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileListAdapter.FileViewHolder(LayoutInflater.from(context).inflate(R.layout.file_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = File.getSelectedFileList().get(position);
        holder.fileName.setText(file.getName());
        holder.fileType.setText(file.getType());
        holder.fileSize.setText(System.getSimplifiedFileSize(file.getSize()));
        holder.removeFile.setBackgroundResource(R.drawable.outline_remove_circle_outline_20);
        if (file.getType().startsWith("image")) {
            holder.icon.setBackgroundResource(R.drawable.outline_photo_48);
        } else if (file.getType().startsWith("video")) {
            holder.icon.setBackgroundResource(R.drawable.baseline_video_library_48);
        } else {
            holder.icon.setBackgroundResource(R.drawable.baseline_file_present_48);
        }
    }

    @Override
    public int getItemCount() {
        return File.getSelectedFileList().size();
    }

    public class FileViewHolder extends RecyclerView.ViewHolder {
        private MaterialTextView fileName;
        private MaterialTextView fileType;
        private MaterialTextView fileSize;
        private MaterialButton removeFile;
        private ImageView icon;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.lblFileName);
            fileType = itemView.findViewById(R.id.lblFileType);
            fileSize = itemView.findViewById(R.id.lblFileSize);
            removeFile = itemView.findViewById(R.id.btnRemoveFile);
            icon = itemView.findViewById(R.id.imgFileIcn);

            removeFile.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                File.getSelectedFileList().remove(pos);
                notifyItemRemoved(pos);
            });
        }
    }
}
