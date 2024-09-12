package com.solo83.service;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
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
    private final static String DIRECTORY_PREFIX = "/";

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

    public void createEmptyFolder(String path) {
        if(!path.endsWith(DIRECTORY_PREFIX)) {
            path += DIRECTORY_PREFIX;
        }
        if (!isObjectExist(path)) {
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .object(path)
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

    public void renameObject(String oldPath, String newPath) {
        try {
            // Retrieve objects under oldPath
            Iterable<Result<Item>> objects = getObjects(oldPath);

            // Iterate over each object
            for (Result<Item> result : objects) {
                Item object = result.get();
                String oldObjectName = object.objectName();
                log.info("Renaming from {}", oldObjectName);

                // Compute the new object name
                String newObjectName = oldObjectName.replace(oldPath, newPath);
                log.info("Renaming to {}", newObjectName);

                // Define the source and destination for the copy operation
                CopySource copySource = CopySource.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(oldObjectName)
                        .build();

                CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(newObjectName)
                        .source(copySource)
                        .build();

                // Perform the copy operation
                minioClient.copyObject(copyObjectArgs);

                // Remove the old object after successful copy
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(env.getProperty("minio.bucket.name"))
                                .object(oldObjectName)
                                .build()
                );
            }
        } catch (Exception e) {
            // Log the exception with a message
            log.error("Error renaming object from {} to {}", oldPath, newPath, e);
            throw new RuntimeException("Error renaming objects", e);
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
