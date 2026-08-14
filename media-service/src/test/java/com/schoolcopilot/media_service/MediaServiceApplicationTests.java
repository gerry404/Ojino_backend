package com.schoolcopilot.media_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.media_service.repository.MediaAssetRepository;
import com.schoolcopilot.media_service.service.MediaService;
import com.schoolcopilot.media_service.storage.MediaStorage;
import com.schoolcopilot.media_service.web.BlobController;
import com.schoolcopilot.media_service.web.MediaController;

/**
 * Verifie le cablage complet : securite, validation des tokens, stockage,
 * controleurs.
 *
 * <p>Le {@link BlobController} doit etre present tant que le stockage local est
 * actif — c'est lui qui recoit les octets. Avec un stockage objet il disparaitra,
 * et ce test devra suivre.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration",
        "ojino.media.local.directory=${java.io.tmpdir}/ojino-media-test"
})
class MediaServiceApplicationTests {

    @MockitoBean
    MediaAssetRepository assets;

    @Autowired
    MediaService mediaService;

    @Autowired
    MediaController mediaController;

    @Autowired
    BlobController blobController;

    @Autowired
    MediaStorage storage;

    @Test
    void contextLoads() {
        assertThat(mediaService).isNotNull();
        assertThat(mediaController).isNotNull();
        assertThat(blobController).isNotNull();
        assertThat(storage).isNotNull();
    }
}
