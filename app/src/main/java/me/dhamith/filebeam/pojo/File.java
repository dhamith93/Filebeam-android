package me.dhamith.filebeam.pojo;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.dhamith.filebeam.helpers.GRPCClient;
import me.dhamith.filebeam.helpers.Keygen;

public class File {
    private String name;
    private String type;
    private Long size;
    private Uri uri;
    private String path;

    private String key;

    private static List<File> selectedFileList;

    public File() {}
    public File(String name, String type, Long size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    public static File fromUri(Context context, Uri uri) {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        File file = new File();
        file.setUri(uri);
        file.setPath(getAbsolutePathFromUri(context, uri));
        file.setName(returnCursor.getString(nameIndex));
        file.setSize(returnCursor.getLong(sizeIndex));
        file.setType(context.getContentResolver().getType(uri));
        return file;
    }

    public static List<File> getSelectedFileList() {
        if (selectedFileList == null) {
            selectedFileList = new ArrayList<>();
        }
        return selectedFileList;
    }

    public static String getAbsolutePathFromUri(Context context, Uri uri) {
        String filePath = null;
        Log.e("---", uri.toString());

        if (DocumentsContract.isDocumentUri(context, uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            String[] split = documentId.split(":");

            if (split.length > 1 && "primary".equalsIgnoreCase(split[0])) {
                filePath = context.getExternalFilesDir(null) + "/" + split[1];
            }
        }

        if (filePath != null) {
            filePath = filePath.replace("Android/data/me.dhamith.filebeam/files/", "");
        }
        return filePath;
    }

    public void receiveEncrypted(int transferIdx) throws IOException {
        setName(getName().replace(":", "_"));
        ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getByName("0.0.0.0"));
        int port = serverSocket.getLocalPort();
        Transfer transfer = Transfer.getTransfers().get(transferIdx);
        transfer.setFilePort(port);
        transfer.setStatus(Transfer.STARTED);
        Log.e("---", "Server listening on: " + serverSocket.getInetAddress().getHostAddress() + ":" + port);

        new Thread(() -> {
            GRPCClient client = GRPCClient.create(transfer.getHost() + ":" + transfer.getPort());
            client.sendClearToSend(this, String.valueOf(port));
        }).start();

        java.io.File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        root = new java.io.File(root, transfer.getHost());
        root.mkdir();
        if (new java.io.File(root , getName()).exists()) {
            root = new java.io.File(root , System.currentTimeMillis() / 1000L + "_" + getName());
        } else {
            root = new java.io.File(root , getName());
        }

        java.io.File finalRoot = root;
        new Thread(() -> {
            long completed = 0;
            try (Socket socket = serverSocket.accept();
                 InputStream inputStream = socket.getInputStream();
                 FileOutputStream fileOutputStream = new FileOutputStream(finalRoot)
            ) {
                byte[] nonce = new byte[16];
                int bytesRead = inputStream.read(nonce);
                SecretKey secretKey = new SecretKeySpec(Keygen.getKey().getBytes(), "AES");

                Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(nonce));

                byte[] buffer = new byte[4096];
                int decryptedBytes;

                transfer.setStartTime(System.currentTimeMillis() / 1000L);

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (transfer.getStatus().equals(Transfer.CANCELED)) {
                        transfer.setEndTime(System.currentTimeMillis() / 1000L);
                        return;
                    }
                    decryptedBytes = cipher.update(buffer, 0, bytesRead, buffer);
                    fileOutputStream.write(buffer, 0, decryptedBytes);
                    completed += bytesRead;
                    transfer.setCompletedBytes(completed);
                }

                decryptedBytes = cipher.doFinal(buffer, 0);
                fileOutputStream.write(buffer, 0, decryptedBytes);
            } catch (Exception e) {
                Log.e("---", e.getMessage());
                transfer.setStatus(Transfer.ERROR);
                transfer.setError(e);
            } finally {
                transfer.setEndTime(System.currentTimeMillis() / 1000L);
                if (completed >= getSize()) {
                    transfer.setStatus(Transfer.COMPLETED);
                } else if (!transfer.getStatus().equals(Transfer.ERROR)) {
                    transfer.setStatus(Transfer.CANCELED);
                }
            }
        }).start();
    }

    public void sendEncrypted(Context context, int transferIdx, String host, int port) throws Exception {
        Transfer transfer = null;
        byte[] keyBytes = this.key.getBytes();
        Socket socket = null;
        OutputStream outputStream = null;
        FileInputStream fileInputStream = null;
        ParcelFileDescriptor parcelFileDescriptor = null;

        try {
            socket = new Socket(host, port);
            outputStream = socket.getOutputStream();
            if (getPath() == null) {
                parcelFileDescriptor = context.getContentResolver().openFileDescriptor(getUri(), "r");
                fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            } else {
                fileInputStream = new FileInputStream(getPath());
            }

            transfer = Transfer.getTransfers().get(transferIdx);
            transfer.setStatus(Transfer.STARTED);
            transfer.setStartTime(System.currentTimeMillis() / 1000L);

            byte[] ivBytes = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(ivBytes);
            outputStream.write(ivBytes);

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                if (transfer.getStatus().equals(Transfer.CANCELED)) {
                    break;
                }
                byte[] encryptedBytes = cipher.update(buffer, 0, bytesRead);
                outputStream.write(encryptedBytes);
                transfer.setCompletedBytes(
                        transfer.getCompletedBytes() + bytesRead
                );
            }

            byte[] encryptedBytes = cipher.doFinal();
            outputStream.write(encryptedBytes);

            getSelectedFileList().remove(this);
            transfer.setStatus(Transfer.COMPLETED);
            transfer.setEndTime(System.currentTimeMillis() / 1000L);
        } catch (Exception e) {
            if (transfer != null) {
                transfer.setStatus(Transfer.ERROR);
                transfer.setError(e);
                transfer.setEndTime(System.currentTimeMillis() / 1000L);
            }
            throw e;
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }

            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        File other = (File) obj;
        return Objects.equals(name, other.name) && Objects.equals(size, other.size);
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
