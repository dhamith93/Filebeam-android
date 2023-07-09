package me.dhamith.filebeam.api;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

import api.Api;
import api.FileServiceGrpc;
import api.FileServiceGrpc.FileServiceImplBase;
import io.grpc.Grpc;
import io.grpc.ServerCall;
import io.grpc.stub.StreamObserver;
import me.dhamith.filebeam.pojo.File;
import me.dhamith.filebeam.pojo.Transfer;

public class FileService extends FileServiceImplBase {
    private final String key;
    private final Context context;
    public static final io.grpc.Context.Key<String> CLIENT_IP_KEY = io.grpc.Context.key("client-ip");

    FileService(String key, Context context) {
        this.key = key;
        this.context = context;
    }
    @Override
    public void hello(Api.Void request, StreamObserver<Api.Void> responseObserver) {
        responseObserver.onNext(Api.Void.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void filePush(Api.FilePushRequest request, StreamObserver<Api.FilePushResponse> responseObserver) {
        if (!request.getKey().equals(this.key)) {
            responseObserver.onNext(Api.FilePushResponse.newBuilder().setAccepted(false).build());
            responseObserver.onCompleted();
        }
        String clientIP = FileService.CLIENT_IP_KEY.get();
        String port = request.getPort();
        int idx = Transfer.getTransfers().size();
        Transfer.getTransfers().add(
                idx,
                new Transfer(
                        request.getFile().getId(),
                        clientIP,
                        9292,
                        Integer.parseInt(port),
                        Transfer.PENDING,
                        Transfer.DOWNLOAD,
                        new File(request.getFile().getName(), request.getFile().getType(), request.getFile().getSize())
                )
        );
        responseObserver.onNext(Api.FilePushResponse.newBuilder().setAccepted(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void clearToSend(Api.FilePushResponse request, StreamObserver<Api.Void> responseObserver) {
        String host = request.getHost();
        String port = request.getPort();
        Api.File file = request.getFile();
        int idx = Transfer.getTransfers().size();
        Transfer.getTransfers().add(
                new Transfer(
                        file.getId(),
                        host,
                        9292,
                        Integer.parseInt(port),
                        Transfer.PENDING,
                        Transfer.UPLOAD,
                        File.getSelectedFileList().get(file.getId())
                )
        );
        try {
            File.getSelectedFileList().get(file.getId()).sendEncrypted(this.context, idx, host, Integer.parseInt(port));
        } catch (Exception e) {
            Transfer.getTransfers().get(idx).setStatus(Transfer.ERROR);
            Transfer.getTransfers().get(idx).setError(e);
            throw new RuntimeException(e);
        }
        responseObserver.onNext(Api.Void.newBuilder().build());
        responseObserver.onCompleted();
    }
}
