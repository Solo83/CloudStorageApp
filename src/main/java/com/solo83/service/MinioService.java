package com.solo83.service;

import com.solo83.exception.MinioServiceException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final Environment env;
    private final static String DIRECTORY_SUFFIX = "/";

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
                log.info("Bucket Created - {}", bucketName);
            }
            log.info("Bucket already exists");
        } catch (Exception e) {
            log.error("Error creating DefaultBucket {}", e.getMessage());
            throw new MinioServiceException("Error creating DefaultBucket, " +e.getMessage(), e);
        }
    }

    public void createEmptyFolder(String path) {
        if(!path.endsWith(DIRECTORY_SUFFIX)) {
            path += DIRECTORY_SUFFIX;
        }
        String[] split = path.split(DIRECTORY_SUFFIX);
        if(split.length > 0) {
            StringBuilder pathToObject = new StringBuilder();
            for (String s : split) {
                pathToObject.append(s).append(DIRECTORY_SUFFIX);
                if (!isObjectExist(pathToObject.toString())) {
                    try {
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .object(pathToObject.toString())
                                        .bucket(env.getProperty("minio.bucket.name"))
                                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                                        .build());
                        log.info("Empty folder created successfully - {}", pathToObject);
                    } catch (Exception e) {
                        log.error("Error creating empty folder {} - {}", path, e.getMessage());
                        throw new MinioServiceException("Error creating empty folder, " + e.getMessage(), e);
                    }
                }
                log.info("Folder is already exists - {}", pathToObject);
            }
        }
    }

    public void uploadFile(String objectName, String path) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(env.getProperty("minio.bucket.name"))
                            .object(objectName)
                            .filename(path)
                            .build());
            log.info("{}/{} is successfully uploaded as object {} ", path, objectName,objectName);
        } catch (Exception e) {
            log.error("Error while uploading object {} - {}",path, e.getMessage());
            throw new MinioServiceException("Error while uploading, " +e.getMessage(), e);
        }
    }

    public void removeObject(String path) {
        if (!isObjectExist(path)) {
            log.warn("Object at path {} does not exist and cannot be removed.", path);
            return;
        }
        List<String> errorMessages = new ArrayList<>();
        Iterable<Result<Item>> objects = getObjects(path, true);
        for (Result<Item> object : objects) {
            try {
                String pathToRemove = object.get().objectName();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(env.getProperty("minio.bucket.name"))
                                .object(pathToRemove)
                                .build());
                log.info("Successfully removed object at path {}", pathToRemove);
            } catch (Exception e) {
                String errorMessage = String.format("Error removing object %s: %s", path, e.getMessage());
                log.error(errorMessage);
                errorMessages.add(errorMessage);
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new MinioServiceException("Errors occurred while removing objects: " + String.join(", ", errorMessages));
        }
    }

    public void renameObject(String oldPath, String newPath) {
        try {
            Iterable<Result<Item>> objects = getObjects(oldPath,true);

            for (Result<Item> result : objects) {
                Item object = result.get();
                String oldObjectName = object.objectName();
                String newObjectName = oldObjectName.replace(oldPath, newPath);

                CopySource copySource = CopySource.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(oldObjectName)
                        .build();

                CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(newObjectName)
                        .source(copySource)
                        .build();

                minioClient.copyObject(copyObjectArgs);

                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(env.getProperty("minio.bucket.name"))
                                .object(oldObjectName)
                                .build()
                );

                log.info("Sucessfully renamed from {} to {}", oldPath, newPath);
            }
        } catch (Exception e) {
            log.error("Error renaming object from {} to {}", oldPath, newPath, e);
            throw new MinioServiceException("Error renaming object, " +e.getMessage(), e);
        }
    }

    public Iterable<Result<Item>> getObjects(String path, boolean recursive) {
        try {
            return minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(env.getProperty("minio.bucket.name"))
                            .prefix(path)
                            .recursive(recursive)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while getObjects from path: {} - {}",path, e.getMessage());
            throw new MinioServiceException("Error while getObjects from path, " +e.getMessage(), e);
        }
    }

    private boolean isObjectExist(String path) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(env.getProperty("minio.bucket.name"))
                    .object(path)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            log.warn("Object not found in Minio for path {}: {}", path, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("An error occurred while checking existence of object {} from Minio: {}", path, e.getMessage());
            return false;
        }
    }

    public boolean checkAllChainsIsExists(String path) {
        String[] split = path.split(DIRECTORY_SUFFIX);
        if(split.length > 0) {
            StringBuilder pathToObject = new StringBuilder();
            for (String s : split) {
                pathToObject.append(s).append(DIRECTORY_SUFFIX);
                if (isObjectExist(pathToObject.toString())) {
                    log.info("Object {} exists - {}", s, pathToObject);
                }
            }
            return true;
        }
        return false;
    }
}
