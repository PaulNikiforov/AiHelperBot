package com.nikiforov.aichatbot.adapter.out.storage;

import com.nikiforov.aichatbot.port.out.DocumentStoragePort;
import com.nikiforov.aichatbot.service.rag.AzureBlobStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@Slf4j
public class AzureBlobStorageAdapter implements DocumentStoragePort {

    private final AzureBlobStorageService blobService;

    @Override
    public byte[] download(String path) {
        try {
            Path tempFile = Files.createTempFile("blob-download-", ".tmp");
            tempFile.toFile().deleteOnExit();

            if (path.contains("vectorstore")) {
                blobService.downloadVectorStoreToPath(tempFile);
            } else if (path.contains("metadata")) {
                blobService.downloadMetadataToPath(tempFile);
            } else {
                blobService.downloadPdfToPath(tempFile);
            }

            return Files.readAllBytes(tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download blob: " + path, e);
        }
    }

    @Override
    public void upload(String path, byte[] data) {
        try {
            Path tempFile = Files.createTempFile("blob-upload-", ".tmp");
            tempFile.toFile().deleteOnExit();
            Files.write(tempFile, data);

            if (path.contains("vectorstore")) {
                blobService.uploadVectorStore(tempFile);
            } else if (path.contains("metadata")) {
                blobService.uploadMetadata(tempFile);
            } else {
                throw new UnsupportedOperationException("Upload not supported for path: " + path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload blob: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        if (path.contains("vectorstore")) {
            return blobService.vectorStoreExists();
        } else if (path.contains("metadata")) {
            return blobService.metadataExists();
        } else {
            return blobService.pdfExists();
        }
    }
}
