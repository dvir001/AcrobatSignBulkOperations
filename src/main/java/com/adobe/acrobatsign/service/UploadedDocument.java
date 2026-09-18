package com.adobe.acrobatsign.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.web.multipart.MultipartFile;

final class UploadedDocument implements AutoCloseable {

	private final Path directory;
	private final Path path;

	private UploadedDocument(Path directory) {
		this.directory = directory;
		this.path = directory.resolve("document.pdf");
	}

	static UploadedDocument store(MultipartFile upload) throws IOException {
		// A fresh private directory prevents filename traversal and pre-planted symlinks.
		UploadedDocument document = new UploadedDocument(Files.createTempDirectory("acrobat-upload-"));
		try {
			try (InputStream input = upload.getInputStream();
					OutputStream output = Files.newOutputStream(document.path,
							StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
				input.transferTo(output);
			}
			return document;
		} catch (IOException | RuntimeException exception) {
			try {
				document.close();
			} catch (IOException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}
	}

	Path getPath() {
		return path;
	}

	@Override
	public void close() throws IOException {
		try {
			Files.deleteIfExists(path);
		} finally {
			Files.deleteIfExists(directory);
		}
	}
}
