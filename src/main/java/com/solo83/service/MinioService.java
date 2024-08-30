package com.solo83.service;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final Environment env;

    public MinioService(MinioClient minioClient, Environment env) {
        this.minioClient = minioClient;
        this.env = env;
        createDefaultBucket();
    }

    private void createDefaultBucket() {
        String bucketName = env.getProperty("minio.bucket.name");
        MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build();
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
        try {
            if (!minioClient.bucketExists(bucketExistsArgs)) {
                minioClient.makeBucket(makeBucketArgs);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createEmptyFolder(String pathToFolder) {
        String DIRECTORY_PREFIX = "/";
        String newFolder = pathToFolder + DIRECTORY_PREFIX;
        if (!isObjectExist(newFolder)) {
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .object(newFolder)
                                .bucket(env.getProperty("minio.bucket.name"))
                                .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                                .build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void uploadFile(String objectName, String pathToObject) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(env.getProperty("minio.bucket.name"))
                            .object(objectName)
                            .filename(pathToObject)
                            .build());
            log.info("{}/{} is successfully uploaded as object {} ", pathToObject, objectName,objectName);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void removeObject(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(env.getProperty("minio.bucket.name")).object(path).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public Iterable<Result<Item>> getObjects(String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .prefix(path)
                        .recursive(true)
                        .build());
    }

    private boolean isObjectExist(String pathToObject) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(env.getProperty("minio.bucket.name"))
                    .object(pathToObject).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
