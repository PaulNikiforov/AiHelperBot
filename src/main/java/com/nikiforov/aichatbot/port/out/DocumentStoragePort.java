package com.nikiforov.aichatbot.port.out;

public interface DocumentStoragePort {

    byte[] download(String path);

    void upload(String path, byte[] data);

    boolean exists(String path);
}
