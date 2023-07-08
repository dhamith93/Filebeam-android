package me.dhamith.filebeam.api;

import android.content.Context;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

import api.FileServiceGrpc;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
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

    public static APIServer getApiServer(Context context, String key) {
        apiServer.context = context;
        apiServer.key = key;
        return apiServer;
    }

    public void start() throws IOException {
        int port = 9292;

        server = OkHttpServerBuilder.forPort(port, InsecureServerCredentials.create())
                .addService(new FileService(this.key))
                .intercept(new IPInterceptor())
                .build()
                .start();

        System.out.println("Server started on port " + port);

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

    static class IPInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            // Extract client IP from the remote address
            String clientIP = ((InetSocketAddress) Objects.requireNonNull(call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR))).getHostString();
            io.grpc.Context ctx = io.grpc.Context.current().withValue(FileService.CLIENT_IP_KEY, clientIP); // Store client IP in the context
            return Contexts.interceptCall(ctx, call, headers, next);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
