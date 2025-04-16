package vn.hoidanit.jobhunter.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FileServicesTest {

    @Autowired
    private FileService fileService;

    @Value("${hoidanit.upload-file.base-uri}")
    private String baseURI;

    @TempDir
    Path tempDir;

    private String testFolder;
    private String testFileName;
    private byte[] testFileContent;

    @BeforeEach
    public void setup() throws Exception {
        testFolder = "test-uploads";
        testFileName = "test-file.txt";
        testFileContent = "Test file content".getBytes();

        // Override baseURI to use tempDir
        String testBaseURI = tempDir.toUri().toString();
        ReflectionTestUtils.setField(fileService, "baseURI", testBaseURI);

        Path testFolderPath = tempDir.resolve(testFolder);
        if (!Files.exists(testFolderPath)) {
            Files.createDirectory(testFolderPath);
        }
    }

    @AfterEach
    public void cleanup() {
        ReflectionTestUtils.setField(fileService, "baseURI", baseURI);
    }

    @Test
    public void testCreateDirectoryNew() throws Exception {
        String newFolderPath = tempDir.resolve("new-folder").toString();

        fileService.createDirectory(newFolderPath);

        File folder = new File(newFolderPath);
        assertTrue(folder.exists());
        assertTrue(folder.isDirectory());
    }

    @Test
    public void testCreateDirectoryExisting() throws Exception {
        String existingFolderPath = tempDir.resolve(testFolder).toString();

        fileService.createDirectory(existingFolderPath);

        File folder = new File(existingFolderPath);
        assertTrue(folder.exists());
        assertTrue(folder.isDirectory());
    }

    @Test
    public void testStore() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "test-file",
                testFileName,
                "text/plain",
                testFileContent);

        String storedFileName = fileService.store(file, testFolder);

        assertNotNull(storedFileName);
        assertTrue(storedFileName.endsWith(testFileName));

        Path storedFilePath = tempDir.resolve(testFolder).resolve(storedFileName);
        assertTrue(Files.exists(storedFilePath));

        byte[] storedContent = Files.readAllBytes(storedFilePath);
        assertArrayEquals(testFileContent, storedContent);
    }

    @Test
    public void testGetFileLengthExisting() throws Exception {
        Path filePath = tempDir.resolve(testFolder).resolve(testFileName);
        Files.write(filePath, testFileContent);

        long length = fileService.getFileLength(testFileName, testFolder);

        assertEquals(testFileContent.length, length);
    }

    @Test
    public void testGetFileLengthNonExisting() throws Exception {
        String nonExistingFile = "non-existing-file.txt";
        long length = fileService.getFileLength(nonExistingFile, testFolder);
        assertEquals(0, length);
    }

    @Test
    public void testGetFileLengthDirectory() throws Exception {
        String subDirName = "subdir";
        Path dirPath = tempDir.resolve(testFolder).resolve(subDirName);
        Files.createDirectory(dirPath);

        long length = fileService.getFileLength(subDirName, testFolder);

        assertEquals(0, length);
    }

    @Test
    public void testGetResourceExisting() throws Exception {
        Path filePath = tempDir.resolve(testFolder).resolve(testFileName);
        Files.write(filePath, testFileContent);

        InputStreamResource resource = fileService.getResource(testFileName, testFolder);

        assertNotNull(resource);
    }

    @Test
    public void testGetResourceNonExisting() throws Exception {
        String nonExistingFile = "non-existing-file.txt";

        assertThrows(Exception.class, () -> {
            fileService.getResource(nonExistingFile, testFolder);
        });
    }
}
