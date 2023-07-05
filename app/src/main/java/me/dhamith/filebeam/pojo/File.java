package me.dhamith.filebeam.pojo;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class File {
    private String name;
    private String type;
    private Long size;
    private Uri uri;
    private String path;

    private String key;

    private static List<File> selectedFileList;

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

    public void sendEncrypted(int transferIdx, String host, int port) throws Exception {
        Transfer transfer = null;

        byte[] keyBytes = this.key.getBytes();

        try (Socket socket = new Socket(host, port);
             OutputStream outputStream = socket.getOutputStream();
             FileInputStream fileInputStream = new FileInputStream(getPath())) {

            transfer = Transfer.getTransfers().get(transferIdx);
            transfer.setStatus(Transfer.STARTED);
            transfer.setStartTime(System.currentTimeMillis() / 1000L);

            byte[] ivBytes = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(ivBytes);
            outputStream.write(ivBytes);

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/CFB/PKCS5Padding");
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
