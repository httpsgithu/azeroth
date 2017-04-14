package cn.com.warlock.filesystem.provider.fdfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;

import cn.com.warlock.filesystem.FileItem;
import cn.com.warlock.filesystem.FileType;
import cn.com.warlock.filesystem.provider.AbstractProvider;
import cn.com.warlock.filesystem.provider.FSOperErrorException;
import cn.com.warlock.filesystem.sdk.fdfs.FastdfsClient;
import cn.com.warlock.filesystem.sdk.fdfs.FileId;
import cn.com.warlock.filesystem.sdk.fdfs.FastdfsClient.Builder;
import cn.com.warlock.filesystem.utils.HttpDownloader;

public class FdfsProvider extends AbstractProvider {

    public static final String NAME = "fastDFS";
    private FastdfsClient      client;

    public FdfsProvider(String urlprefix, String bucketName, String[] servers, long connectTimeout,
                        int maxThreads) {
        this.urlprefix = urlprefix.endsWith(DIR_SPLITER) ? urlprefix : urlprefix + DIR_SPLITER;
        this.bucketName = bucketName;
        Builder builder = FastdfsClient.newBuilder().connectTimeout(connectTimeout)
            .readTimeout(connectTimeout).maxThreads(maxThreads);

        String[] tmpArray;
        for (String s : servers) {
            tmpArray = s.split(":");
            builder.tracker(tmpArray[0], Integer.parseInt(tmpArray[1]));
        }
        client = builder.build();
    }

    @Override
    public String upload(String catalog, String fileName, File file) {
        CompletableFuture<FileId> upload = client.upload(bucketName, file);
        try {
            return getFullPath(upload.get().toString());
        } catch (Exception e) {
            processException(e);
        }

        return null;

    }

    @Override
    public String upload(String catalog, String fileName, byte[] data, FileType fileType) {
        CompletableFuture<FileId> upload = client.upload(bucketName, fileName, data);
        try {
            return getFullPath(upload.get().toString());
        } catch (Exception e) {
            processException(e);
        }

        return null;

    }

    @Override
    public String upload(String catalog, String fileName, InputStream in, FileType fileType) {
        try {
            byte[] bs = IOUtils.toByteArray(in);
            return upload(bucketName, fileName, bs, fileType);
        } catch (IOException e) {
            throw new FSOperErrorException(name(), e);
        }
    }

    @Override
    public String upload(String catalog, String fileName, String origUrl) {
        try {
            FileItem fileItem = HttpDownloader.read(origUrl);
            return upload(bucketName, fileName, fileItem.getDatas(), fileItem.getFileType());
        } catch (IOException e) {
            throw new FSOperErrorException(name(), e);
        }
    }

    @Override
    public boolean delete(String fileName) {
        try {
            if (fileName.contains(DIR_SPLITER))
                fileName = fileName.replace(urlprefix, "");
            FileId path = FileId.fromString(fileName);
            client.delete(path).get();
            return true;
        } catch (Exception e) {
            processException(e);
        }
        return false;
    }

    @Override
    public String createUploadToken(String... fileNames) {
        return null;
    }

    @Override
    public String name() {
        return NAME;
    }

    private void processException(Exception e) {
        throw new FSOperErrorException(name(), e);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
