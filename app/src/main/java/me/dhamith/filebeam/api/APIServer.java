package me.dhamith.filebeam.api;

import android.content.Context;

import java.io.IOException;
import api.FileServiceGrpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.okhttp.OkHttpServerBuilder;

public class APIServer {
    private String key;
    private Server server;
    private Context context;
    private APIServer() { }

    private static final APIServer apiServer = create();

    private static APIServer create() {
        return new APIServer();
    }

    public static APIServer getApiServer(Context context) {
        apiServer.context = context;
        return apiServer;
    }

    public void start() throws IOException {
        int port = 9292; // Choose a port for your server

        server = OkHttpServerBuilder.forPort(port, InsecureServerCredentials.create())
                .addService(new FileService(context)) // Add your gRPC service implementation
                .build()
                .start();

        System.out.println("Server started on port " + port);

        // Handle server shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public boolean isRunning() {
        return server != null && !server.isShutdown() && !server.isTerminated();
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
