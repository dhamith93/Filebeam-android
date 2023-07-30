package me.dhamith.filebeam;

import me.dhamith.filebeam.adapters.DeviceListAdapter;
import me.dhamith.filebeam.api.APIServer;
import me.dhamith.filebeam.databinding.ActivityMainBinding;
import me.dhamith.filebeam.helpers.Keygen;
import me.dhamith.filebeam.helpers.System;
import me.dhamith.filebeam.pojo.File;
import me.dhamith.filebeam.pojo.Transfer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
    };
    private ActivityMainBinding binding;
    final DevicesFragment devicesFragment = new DevicesFragment();
    final FilesFragment filesFragment = new FilesFragment();
    final TransfersFragment transfersFragment = new TransfersFragment();
    final FragmentManager fm = getSupportFragmentManager();
    Fragment active = devicesFragment;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.bottomNavView.setBackground(null);

        Window window = getWindow();
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);
        window.setStatusBarColor(Color.parseColor("#222127"));
        window.setNavigationBarColor(Color.parseColor("#222127"));

        setContentView(binding.getRoot());
        binding.bottomNavView.setBackground(null);

        fm.beginTransaction().add(R.id.frame_layout, transfersFragment, "3").hide(transfersFragment).commit();
        fm.beginTransaction().add(R.id.frame_layout, filesFragment, "2").hide(filesFragment).commitNow();
        fm.beginTransaction().add(R.id.frame_layout,devicesFragment, "1").commitNow();
        binding.bottomNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.devices) {
                fm.beginTransaction().hide(active).show(devicesFragment).commitNow();
                active = devicesFragment;
            }
            if (item.getItemId() == R.id.files) {
                fm.beginTransaction().hide(active).show(filesFragment).commitNow();
                active = filesFragment;
                filesFragment.updateList();
            }
            if (item.getItemId() == R.id.transfers) {
                fm.beginTransaction().hide(active).show(transfersFragment).commitNow();
                active = transfersFragment;
                transfersFragment.updateList();
            }
            return true;
        });

        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS);

        MaterialTextView txtIP = findViewById(R.id.lblIP);
        MaterialTextView txtKey = findViewById(R.id.lblKey);

        String key = Keygen.getKey();
        txtIP.setText(System.getLocalIPs().toString());
        txtKey.setText(key);

        APIServer server = APIServer.getApiServer(this, key);
        try {
            if (!server.isRunning()) {
                server.start();
            }
        } catch (IOException e) {
            Log.e("---", e.toString());
            throw new RuntimeException(e);
        }

        ExtendedFloatingActionButton fabAddFiles = findViewById(R.id.fabFileAdd);
        fabAddFiles.setOnClickListener(view -> {
            Intent data = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            data.setType("*/*");
            data.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            data = Intent.createChooser(data, "Chose files");
            filePickerResultLauncher.launch(data);
        });

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean isAnyTransferActive = false;
                for (Transfer t : Transfer.getTransfers()) {
                    if (t.isActive()) {
                        isAnyTransferActive = true;
                        break;
                    }
                }
                if (isAnyTransferActive) {
                    if (wakeLock == null || !wakeLock.isHeld()) {
                        acquireWakeLock();
                    }
                } else {
                    releaseWakeLock();
                }
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(runnable, 5000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseWakeLock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        acquireWakeLock();
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean isGranted : result.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (!allGranted) {
//                    Toast.makeText(this, "Required permissions were not granted. Filebeam requires Read/Write permissions to storage to function properly.", Toast.LENGTH_LONG).show();
//                    this.finish();
                }
            }
    );

    private final ActivityResultLauncher<Intent> filePickerResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            int count = clipData.getItemCount();
                            for (int i = 0; i < count; i++) {
                                handleSelectedFile(clipData.getItemAt(i).getUri());
                            }
                        } else {
                            handleSelectedFile(data.getData());
                        }
                    }
                }
            }
    );

    private void handleSelectedFile(Uri uri) {
        File file = File.fromUri(this, uri);
        if (!File.getSelectedFileList().contains(file)) {
            File.getSelectedFileList().add(file);
            final int idx = File.getSelectedFileList().lastIndexOf(file);
            filesFragment.updateList(idx);
        }
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Filebeam:WakeLock");
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(30*60*1000L);
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}