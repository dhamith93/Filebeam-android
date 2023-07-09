package me.dhamith.filebeam.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import me.dhamith.filebeam.R;
import me.dhamith.filebeam.helpers.GRPCClient;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
    private Context context;
    private List<String> devices;

    public DeviceListAdapter(Context context, List<String> devices) {
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DeviceViewHolder(LayoutInflater.from(context).inflate(R.layout.device_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.deviceIp.setText(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder {
        private MaterialTextView deviceIp;
        private TextInputEditText deviceKey;
        private MaterialButton sendBtn;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIp = itemView.findViewById(R.id.lblDeviceIP);
            deviceKey = itemView.findViewById(R.id.txtDeviceKey);
            sendBtn = itemView.findViewById(R.id.btnShare);
            sendBtn.setOnClickListener(view -> {
                String key = deviceKey.getText().toString();
                if (key.length() > 0) {
                    GRPCClient client = GRPCClient.create(devices.get(getAdapterPosition())+":9292");
                    client.handleFileList(key);
                }
            });
        }
    }
}
