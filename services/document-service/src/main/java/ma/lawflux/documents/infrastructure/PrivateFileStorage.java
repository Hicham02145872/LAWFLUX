package ma.lawflux.documents.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import ma.lawflux.documents.application.DocumentContentReader;
import ma.lawflux.documents.application.DocumentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class PrivateFileStorage implements DocumentStorage, DocumentContentReader {
    private final Path root;

    public PrivateFileStorage(@Value("${lawflux.documents.storage-root}") String storageRoot) {
        root = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public String store(UUID cabinetId, String originalName, InputStream content, long sizeBytes) {
        if (cabinetId == null || content == null || sizeBytes < 0) throw new IllegalArgumentException("Fichier invalide");
        String key = cabinetId + "/" + UUID.randomUUID();
        Path file = resolve(key);
        try {
            Files.createDirectories(file.getParent());
            if (Files.isSymbolicLink(root) || Files.isSymbolicLink(file.getParent())) {
                throw new IOException("Répertoire de stockage non autorisé");
            }
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                long remaining = sizeBytes;
                while (remaining > 0) {
                    int read = content.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) throw new IOException("Contenu incomplet");
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
                if (content.read() != -1) throw new IOException("Contenu plus grand que prévu");
            }
            return key;
        } catch (IOException e) {
            try { Files.deleteIfExists(file); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            throw new IllegalStateException("Stockage du document impossible", e);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        Path file = resolve(storageKey);
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Fichier introuvable");
            return Files.newInputStream(file, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du document impossible", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try { Files.deleteIfExists(resolve(storageKey)); }
        catch (IOException e) { throw new IllegalStateException("Nettoyage du document impossible", e); }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Clé de stockage invalide");
        }
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Clé de stockage invalide");
        return file;
    }
}
