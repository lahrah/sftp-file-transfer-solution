package com.example.sftpfiletransfersolution.service;

public interface JschFileTransferService {
    boolean uploadFile(String localFilePath, String remoteFilePath);

    boolean downloadFile(String localFilePath, String remoteFilePath);
}
