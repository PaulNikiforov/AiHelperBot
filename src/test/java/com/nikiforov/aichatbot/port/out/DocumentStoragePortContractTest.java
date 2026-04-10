package com.nikiforov.aichatbot.port.out;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class DocumentStoragePortContractTest {

    public abstract DocumentStoragePort createPort();

    @Test
    void download_returnsNonNullBytes() {
        DocumentStoragePort port = createPort();

        byte[] data = port.download("test-path.txt");

        assertThat(data).isNotNull();
    }

    @Test
    void upload_thenExists_returnsTrue() {
        DocumentStoragePort port = createPort();

        String path = "contract-test-" + System.currentTimeMillis() + ".bin";
        port.upload(path, "test-data".getBytes());

        assertThat(port.exists(path)).isTrue();
    }
}
