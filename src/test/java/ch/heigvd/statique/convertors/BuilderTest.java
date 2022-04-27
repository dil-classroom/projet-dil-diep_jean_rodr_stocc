package ch.heigvd.statique.convertors;

import ch.heigvd.statique.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {
  Path root, build;
  private final LinkedList<Path> filesMDPath = new LinkedList<>();
  private final LinkedList<Path> filesHtmlPath = new LinkedList<>();
  private final LinkedList<Path> filesYamlPath = new LinkedList<>();
  private final LinkedList<Path> filesOtherPath = new LinkedList<>();
  private final LinkedList<String> filesMDText = new LinkedList<>();
  private final LinkedList<String> filesHtmlText = new LinkedList<>();
  private final LinkedList<String> filesYamlText = new LinkedList<>();
  private final LinkedList<Map<String, Object>> filesYamlMap = new LinkedList<>();

  /**
   * Writes inside a file
   *
   * @param filePath file path
   * @param text     file text (using \n separators)
   * @throws IOException BufferWriter exception
   */
  private void writeFile(String filePath, String text) throws IOException {
    try (BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
      out.write(text);
    }
  }

  /**
   * Creates files for test
   *
   * @throws IOException File creation exception
   */
  @BeforeEach
  void createFiles() throws IOException {
    root = Files.createTempDirectory("site");
    build = root.resolve("build");

    filesMDPath.add(Files.createFile(root.resolve("index.md")));
    filesHtmlPath.add(build.resolve("index.html"));
    filesMDText.add("# Mon premier article\n" + "## Mon sous-titre\n" + "Le contenu de mon article.\n"
        + "![Une image](./dossier/image.png)\n");

    filesHtmlText.add("<h1>Mon premier article</h1>\n" + "<h2>Mon sous-titre</h2>\n" + "<p>Le contenu de mon article.\n"
        + "<img src=\"./dossier/image.png\" alt=\"Une image\" /></p>\n");
    writeFile(filesMDPath.getLast().toString(), filesMDText.getLast());

    filesYamlPath.add(Files.createFile(root.resolve("config.yaml")));
    filesYamlText
        .add("titre: Mon premier article\n" + "auteur: Jean François\n" + "date: 2021-03-10\n" + "chiffre: 25\n");
    filesYamlMap.addLast(new HashMap<>() {
      {
        put("titre", "Mon premier article");
        put("auteur", "Jean François");
        put("date", new Yaml().loadAs("2021-03-10", Date.class));
        put("chiffre", 25);
      }
    });
    writeFile(filesYamlPath.getLast().toString(), filesYamlText.getLast());
    filesYamlPath.set(filesYamlMap.size() - 1, build.resolve("config.yaml"));

    Files.createDirectories(root.resolve("dossier"));
    filesMDPath.add(Files.createFile(root.resolve("dossier/page.md")));
    filesHtmlPath.add(build.resolve("dossier/page.html"));
    filesMDText.add("# Première page\n");
    filesHtmlText.add("<h1>Première page</h1>\n");
    writeFile(filesMDPath.getLast().toString(), filesMDText.getLast());

    filesOtherPath.add(root.resolve("dossier/image.png"));
    imageGenerator(filesOtherPath.getLast().toString());
    filesOtherPath.set(filesOtherPath.size() - 1, build.resolve("dossier/image.png"));
  }

  /**
   * Generates an image Code from :
   * https://dyclassroom.com/image-processing-project/how-to-create-a-random-pixel-image-in-java
   *
   * @param path image path with name and png extension
   * @throws IOException
   */
  public void imageGenerator(String path) throws IOException {
    // image dimension
    int width = 640;
    int height = 320;
    // create buffered image object img
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    // file object
    File f = null;
    // create random image pixel by pixel
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int a = (int) (Math.random() * 256); // alpha
        int r = (int) (Math.random() * 256); // red
        int g = (int) (Math.random() * 256); // green
        int b = (int) (Math.random() * 256); // blue

        int p = (a << 24) | (r << 16) | (g << 8) | b; // pixel

        img.setRGB(x, y, p);
      }
    }
    // write image
    try {
      f = new File(path);
      ImageIO.write(img, "png", f);
    } catch (IOException e) {
      System.out.println("Error: " + e);
    }
  }

  /**
   * Clean up temporary dir and file
   */
  @AfterEach
  void tearDown() throws IOException {
    Utils.deleteRecursive(root);
  }

  /**
   * Test build command
   */
  @Test
  void build() throws IOException {
    Builder builder = new Builder(root, build);
    builder.build();

    // An HTML file should exist with the MD content
    for (int i = 0; i < filesHtmlPath.size(); ++i) {
      assertEquals(filesHtmlText.get(i), Files.readString(filesHtmlPath.get(i), StandardCharsets.UTF_8));
    }

    // Yaml config file shouldn't be copied
    for (Path path : filesYamlPath) {
      assertFalse(Files.exists(path));
    }

    // Yaml configuration should be retrieved
    assertEquals(filesYamlMap.getLast(), builder.getConfig().toRender());

    // Other files should have been copied
    for (Path path : filesOtherPath) {
      assertTrue(Files.exists(path));
    }
  }
}