package me.dhamith.filebeam.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import api.Api;
import api.FileServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractBlockingStub;
import kotlinx.coroutines.CoroutineScope;
import me.dhamith.filebeam.api.FileService;
import me.dhamith.filebeam.pojo.File;

public class GRPCClient {
    public ManagedChannel channel;
    public FileServiceGrpc.FileServiceBlockingStub client;
    public String endpoint;

    @NonNull
    public static GRPCClient create(String endpoint) {
        GRPCClient grpcClient = new GRPCClient();
        grpcClient.endpoint = endpoint;
        grpcClient.channel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext().build();
        grpcClient.client = FileServiceGrpc.newBlockingStub(grpcClient.channel);
        return grpcClient;
    }

    public void close() {
        try {
            channel.shutdown().awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e("---", e.toString());
        }
    }

    public boolean hello() {
        boolean isSuccessful = false;
        try {
            Api.Void resp = client.hello(Api.Void.newBuilder().build());
            isSuccessful = true;
        } catch (Exception e) {
            // exception ignored as this is expected if service is not running on host
        } finally {
            close();
        }
        return isSuccessful;
    }

    public void pushFile(String key, Api.File file) {
        try {
            Api.FilePushRequest req = Api.FilePushRequest.newBuilder()
                    .setFile(file)
                    .setHost(endpoint)
                    .setKey(key)
                    .setPort(endpoint.split(":")[1])
                    .build();
            Api.FilePushResponse resp =  client.filePush(req);
        } catch (Exception e) {
            Log.e("---", e.toString());
        }
    }

    public void handleFileList(String key) {
        int count = 0;
        for (File f : File.getSelectedFileList()) {
            f.setKey(key);
            Api.File file = Api.File.newBuilder()
                    .setId(count)
                    .setName(f.getName())
                    .setType(f.getType())
                    .setSize(f.getSize())
                    .build();
            this.pushFile(key, file);
            count += 1;
        }
    }
}
