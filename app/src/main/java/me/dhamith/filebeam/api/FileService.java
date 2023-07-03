package me.dhamith.filebeam.api;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import api.Api;
import api.FileServiceGrpc;
import api.FileServiceGrpc.FileServiceImplBase;
import io.grpc.stub.StreamObserver;
import me.dhamith.filebeam.pojo.File;
import me.dhamith.filebeam.pojo.Transfer;

public class FileService extends FileServiceImplBase {
    private Context context;

    FileService(Context context) {
        this.context = context;
    }
    @Override
    public void hello(Api.Void request, StreamObserver<Api.Void> responseObserver) {
        responseObserver.onNext(Api.Void.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void filePush(Api.FilePushRequest request, StreamObserver<Api.FilePushResponse> responseObserver) {
        responseObserver.onNext(Api.FilePushResponse.newBuilder().build());
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
            File.getSelectedFileList().get(file.getId()).send(idx, host, Integer.parseInt(port));
        } catch (IOException e) {
            Transfer.getTransfers().get(idx).setStatus(Transfer.ERROR);
            Transfer.getTransfers().get(idx).setError(e);
            throw new RuntimeException(e);
        }
        responseObserver.onNext(Api.Void.newBuilder().build());
        responseObserver.onCompleted();
    }
}
