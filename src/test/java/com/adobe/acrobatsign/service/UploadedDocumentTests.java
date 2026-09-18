package com.adobe.acrobatsign.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.atomic.AtomicReference;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import com.adobe.acrobatsign.model.SendAgreementVO;

class UploadedDocumentTests {

	@TempDir
	Path temporaryDirectory;

	@ParameterizedTest
	@ValueSource(strings = {"../../application.bat", "/tmp/application.bat",
			"C:\\Windows\\application.bat", "..\\application.bat", "", "document.pdf"})
	void ignoresClientFilenameAndRemovesTemporaryDocument(String filename) throws Exception {
		Path uploadedPath;
		try (UploadedDocument document = UploadedDocument.store(upload(filename))) {
			uploadedPath = document.getPath();
			assertEquals("document.pdf", uploadedPath.getFileName().toString());
			assertTrue(uploadedPath.getParent().getFileName().toString().startsWith("acrobat-upload-"));
			assertEquals("test document", Files.readString(uploadedPath));
			assertFalse(Files.isSymbolicLink(uploadedPath));
		}
		assertFalse(Files.exists(uploadedPath));
		assertFalse(Files.exists(uploadedPath.getParent()));
	}

	@Test
	void doesNotOverwriteExistingFileNamedByClient() throws Exception {
		Path existing = temporaryDirectory.resolve("application.bat");
		Files.writeString(existing, "original");
		try (UploadedDocument document = UploadedDocument.store(upload(existing.toString()))) {
			assertNotEquals(existing, document.getPath());
			assertEquals("original", Files.readString(existing));
		}
	}

	@Test
	void repeatedFilenamesUseSeparateDirectories() throws Exception {
		try (UploadedDocument first = UploadedDocument.store(upload("document.pdf"));
				UploadedDocument second = UploadedDocument.store(upload("document.pdf"))) {
			assertNotEquals(first.getPath().getParent(), second.getPath().getParent());
		}
	}

	@Test
	void doesNotFollowClientNamedSymlink() throws Exception {
		Path target = temporaryDirectory.resolve("target.txt");
		Files.writeString(target, "original");
		Path link = temporaryDirectory.resolve("document.pdf");
		try {
			Files.createSymbolicLink(link, target);
		} catch (UnsupportedOperationException | IOException exception) {
			Assumptions.assumeTrue(false, "Symbolic links are not available on this platform");
		}
		try (UploadedDocument document = UploadedDocument.store(upload(link.toString()))) {
			assertNotEquals(link, document.getPath());
			assertEquals("original", Files.readString(target));
			assertTrue(Files.isSymbolicLink(link));
		}
	}

	@Test
	void temporaryDirectoryIsPrivateOnPosix() throws Exception {
		try (UploadedDocument document = UploadedDocument.store(upload("document.pdf"))) {
			Assumptions.assumeTrue(Files.getFileStore(document.getPath()).supportsFileAttributeView("posix"));
			var permissions = Files.getPosixFilePermissions(document.getPath().getParent());
			assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
			assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
			assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
			assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
			assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
			assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void bothSendMethodsCleanUpAfterAdobeFailure(boolean contract) throws Exception {
		AdobeSignService service = new AdobeSignService();
		service.restApiAgreements = mock(RestApiAgreements.class);
		AtomicReference<Path> sentPath = new AtomicReference<>();
		when(service.restApiAgreements.postTransientDocument(anyString(), anyString(), anyString(), anyString()))
				.thenAnswer(invocation -> {
					Path path = Path.of(invocation.getArgument(2, String.class));
					sentPath.set(path);
					assertEquals("test document", Files.readString(path));
					assertEquals("document.pdf", invocation.getArgument(3));
					throw new IOException("Simulated Adobe failure");
				});

		String result = contract
				? service.sendContract(new SendAgreementVO(), upload("../../application.bat"))
				: service.sendAgreement(new org.json.JSONArray(), upload("../../application.bat"));

		assertNull(result);
		assertNotNull(sentPath.get());
		assertFalse(Files.exists(sentPath.get()));
		assertFalse(Files.exists(sentPath.get().getParent()));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void bothSendMethodsCleanUpAfterSuccess(boolean contract) throws Exception {
		AdobeSignService service = new AdobeSignService();
		service.restApiAgreements = mock(RestApiAgreements.class);
		AtomicReference<Path> sentPath = new AtomicReference<>();
		JSONObject uploaded = new JSONObject();
		uploaded.put("transientDocumentId", "transient-id");
		JSONObject sent = new JSONObject();
		sent.put("id", "agreement-id");
		when(service.restApiAgreements.postTransientDocument(anyString(), anyString(), anyString(), anyString()))
				.thenAnswer(invocation -> {
					sentPath.set(Path.of(invocation.getArgument(2, String.class)));
					assertTrue(Files.isRegularFile(sentPath.get()));
					return uploaded;
				});
		when(service.restApiAgreements.sendAgreement(anyString(), any(JSONObject.class), eq("transient-id"), any()))
				.thenReturn(sent);

		String result = contract
				? service.sendContract(new SendAgreementVO(), upload("document.pdf"))
				: service.sendAgreement(new org.json.JSONArray(), upload("document.pdf"));

		assertEquals("agreement-id", result);
		assertFalse(Files.exists(sentPath.get()));
		assertFalse(Files.exists(sentPath.get().getParent()));
	}

	private MockMultipartFile upload(String filename) {
		return new MockMultipartFile("file", filename, "application/pdf", "test document".getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
