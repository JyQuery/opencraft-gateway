package science.atlarge.opencraft.opencraft.gateway.io;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import java.io.*;
import java.util.ArrayList;

public class BlobClientAzure {
    // performance
    private String yourSasToken = "?sv=2020-02-10&ss=b&srt=sco&sp=rwdlacx&se=2022-06-23T15:48:00Z&st=2021-06-23T07:48:00Z&spr=https&sig=rIwXYrhHmBjQ5z2hDo7LxS0RR3xMqicf1vhdAITNDI0%3D";
    private String blockEndpoint = "https://opencraftstoragedeh.blob.core.windows.net/";
    private String containerName = "world-container";

    private BlobServiceClient blobServiceClient;
    private Boolean exists;
    private BlobContainerClient containerClient;
    private BlobClient blobClient;

    // for async
    private BlobServiceAsyncClient blobServiceAsyncClient;
    private BlobContainerAsyncClient containerAsyncClient;
    private BlobAsyncClient blobAsyncClient;

    public static ArrayList<String> workingFiles = new ArrayList<>();

    public BlobClientAzure() {
        buildSyncClient();
    }

    private void buildSyncClient() {
        blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(blockEndpoint)
                .sasToken(yourSasToken)
                .buildClient();

        containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // if container does not exist, create one
        if (!containerClient.exists()){
            try {
                containerClient = blobServiceClient.createBlobContainer(containerName);
            } catch (BlobStorageException e) {
                // Ignore if container exists
                if (!e.getErrorCode().equals(BlobErrorCode.CONTAINER_ALREADY_EXISTS)) {
                    throw e;
                }
            }
        }
    }

    public BlobClientAzure(String fileName) {
        this();
        setBlobClient(fileName);
    }

    public BlobClientAzure(Boolean async) {
        if (async) {
            buildAsyncClient();
        } else {
            buildSyncClient();
        }
    }

    public BlobClientAzure(Boolean async, String fileName) {
        this(async);
        setBlobClientAsync(fileName);
    }

    private void buildAsyncClient() {
        blobServiceAsyncClient = new BlobServiceClientBuilder()
                .endpoint(blockEndpoint)
                .sasToken(yourSasToken)
                .buildAsyncClient();
        containerAsyncClient = blobServiceAsyncClient.getBlobContainerAsyncClient(containerName);
    }

    public BlobClientAzure(String fileName, Boolean async) {
        this(async);
        setBlobClientAsync(fileName);
    }

    public void setBlobClient(String fileName) {
        // 400 exception if blob does not exist, safe to ignore
        try {
            blobClient = containerClient.getBlobClient(fileName);
            exists = blobClient.exists();
        } catch (Exception ignored) {}
    }

    public void setBlobClientAsync(String fileName) {
        blobAsyncClient = containerAsyncClient.getBlobAsyncClient(fileName);
    }

    public void uploadOrReplaceFileAsync (String path) {
        if (workingFiles.contains(path))
            return;
        workingFiles.add(path);

        blobAsyncClient.uploadFromFile(path, true)
                .doOnError(throwable ->  uploadAsyncOnError (path, throwable))
                .doOnSuccess(success -> uploadAsyncOnSuccess(path))
                .subscribe();
    }

    private void uploadAsyncOnError(String path, Throwable throwable) {
        workingFiles.remove(path);
    }

    private void uploadAsyncOnSuccess(String path) {
        workingFiles.remove(path);
    }

    public BlobClient getBlobClient() {
        return blobClient;
    }

    public void uploadOrReplaceBytes(ByteArrayInputStream in, int size) {
        blobClient.upload(in, size, true);
    }

    public void uploadOrReplaceString(String in) {
        ByteArrayInputStream dataStream = new ByteArrayInputStream(in.getBytes());
        uploadOrReplaceBytes(dataStream, in.length());
    }

    public void uploadOrReplaceOutputStream(ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        uploadOrReplaceBytes(in, out.size());
    }

    public void uploadOrReplaceFile (String path) {
        blobClient.uploadFromFile(path, true);
    }

    public ByteArrayOutputStream downloadToStream() {
        if (!getExists())
            return null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        blobClient.download(bout);
        return bout;
    }

    public byte[] downloadToByteArray() {
        ByteArrayOutputStream res = downloadToStream();
        if (res == null)
            return null;
        return downloadToStream().toByteArray();
    }

    public ByteArrayInputStream downloadToInputStream() {
        byte [] ba = downloadToByteArray();
        if (ba == null)
            return null;
        return new ByteArrayInputStream(ba);
    }

    public String downloadToString() {
        ByteArrayOutputStream res = downloadToStream();
        if (res == null)
            return null;
        return downloadToStream().toString();
    }

    public void downloadToFile(String localPath) {
        downloadToFile(localPath, false);
    }

    public void downloadToFile(String localPath, Boolean overwrite) {
        if (workingFiles.contains(localPath))
            return;
        workingFiles.add(localPath);
        if (getExists()) {
            blobClient.downloadToFile(localPath, overwrite);
        }
        workingFiles.remove(localPath);
    }

    public BlobContainerClient getContainerClient() {
        return containerClient;
    }

    public Boolean getExists() {
        return exists;
    }

    public long getLastModified() {
        if (getExists()) {
            return blobClient.getProperties().getLastModified().toInstant().toEpochMilli();
        } else
            return 0;
    }
}
