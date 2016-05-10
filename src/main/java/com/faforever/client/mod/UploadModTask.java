package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.io.Zipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Validator;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

public class UploadModTask extends AbstractPrioritizedTask<Void> {

  @Resource
  PreferencesService preferencesService;
  @Resource
  FafApiAccessor fafApiAccessor;

  private Path modPath;
  private Consumer<Float> progressListener;
  private CompletableFuture<Void> future;

  public UploadModTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(modPath, "modPath must not be null");
    Validator.notNull(progressListener, "progressListener must not be null");

    ResourceLocks.acquireUploadLock();
    ResourceLocks.acquireDiskLock();
    try {
      noCatch(() -> {
        Path cacheDirectory = preferencesService.getCacheDirectory();
        Files.createDirectories(cacheDirectory);
        Path tmpFile = createTempFile(cacheDirectory, "mod", ".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(newOutputStream(tmpFile)))) {
          Zipper.of(modPath)
              .to(zipOutputStream)
              .listener((written, total) -> progressListener.accept((float) (written / total)))
              .zip();
        }

        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(tmpFile))) {
          fafApiAccessor.uploadMod(inputStream);
        }
        Files.delete(tmpFile);
      });
      return null;
    } finally {
      ResourceLocks.acquireDiskLock();
      ResourceLocks.acquireUploadLock();
    }
  }

  public void setModPath(Path modPath) {
    this.modPath = modPath;
  }

  public void setProgressListener(Consumer<Float> progressListener) {
    this.progressListener = progressListener;
  }

  public CompletableFuture<Void> getFuture() {
    return future;
  }

  public void setFuture(CompletableFuture<Void> future) {
    this.future = future;
  }
}