package ch.heigvd.statique.commands;

import org.apache.commons.io.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class Utils {

  /**
   * Delete given directory.
   *
   * @param directory Directory path to clean.
   */
  public static void deleteRecursive(Path directory) throws IOException {
    FileUtils.deleteDirectory(directory.toFile());
  }

  /**
   * Copy a file from resources to a destination.
   *
   * @param resourceName Resource name.
   * @param destination  Destination path.
   */
  public static void copyFileFromResources(String resourceName, Path destination) throws IOException {
    InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream(resourceName);
    FileUtils.copyInputStreamToFile(inputStream, destination.toFile());
  }
}
