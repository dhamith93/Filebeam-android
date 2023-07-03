package me.dhamith.filebeam.pojo;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class File {
    private String name;
    private String type;
    private Long size;
    private Uri uri;
    private String path;
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



    public void send(int transferIdx, String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 1000);
        OutputStream out = socket.getOutputStream();
        Transfer transfer = null;

        try {
            transfer = Transfer.getTransfers().get(transferIdx);
            transfer.setStatus(Transfer.STARTED);
            transfer.setStartTime(System.currentTimeMillis() / 1000L);
            FileInputStream fis = new FileInputStream(getPath());
            byte[] buffer = new byte[1024];

            int count = 0;
            while ((count = fis.read(buffer)) > 0) {
                if (transfer.getStatus().equals(Transfer.CANCELED)) {
                    break;
                }
                out.write(buffer, 0, count);
                transfer.setCompletedBytes(
                    transfer.getCompletedBytes() + count
                );
                transfer.setProgress((int) (transfer.getCompletedBytes() / (double) transfer.getFile().getSize()) * 100);
            }

            fis.close();

            getSelectedFileList().remove(this);
            if (transfer.getCompletedBytes() == transfer.getFile().getSize()) {
                transfer.setProgress(100);
                transfer.setStatus(Transfer.COMPLETED);
            }
        } catch (Exception e) {
            if (transfer != null) {
                transfer.setStatus(Transfer.ERROR);
                transfer.setError(e);
            }
            e.printStackTrace();
        } finally {
            if (transfer != null) {
                transfer.setEndTime(System.currentTimeMillis() / 1000L);
            }
            out.close();
            socket.close();
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
}
