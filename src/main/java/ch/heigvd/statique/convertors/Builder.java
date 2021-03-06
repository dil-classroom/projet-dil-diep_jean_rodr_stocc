package ch.heigvd.statique.convertors;

import ch.heigvd.statique.utils.Config;
import ch.heigvd.statique.utils.Page;
import ch.heigvd.statique.utils.Utils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** This class is used to convert a Markdown "site" to a static website and apply a template. */
public class Builder {
    /** The path of the source directory */
    private final Path source;
    /** The path of the destination directory */
    private final Path destination;
    /** The configuration file of the website */
    private Config config;
    /** The name of the template file */
    private final String templateName;
    /** The template */
    private Template template;
    /** The folder containing the template */
    private String templateFolder = "template";

    /**
     * Builder constructor.
     *
     * @param source source directory
     * @param destination destination directory
     */
    public Builder(Path source, Path destination) {
        this.source = source;
        this.destination = destination;
        this.templateName = "layout";
    }

    /**
     * Gets the configuration of the website.
     *
     * @return the configuration of the website
     */
    public Config getConfig() {
        return config;
    }

    /** Builds the website. */
    public void build() throws IOException {
        Path configFile;

        // Delete directory of exists
        if (Files.exists(destination)) {
            Utils.deleteRecursive(destination);
        }

        Files.createDirectories(destination);

        // Get the config file path
        if (Files.exists(source.resolve("config.yml"))) {
            configFile = source.resolve("config.yml");
        } else if (Files.exists(source.resolve("config.yaml"))) {
            configFile = source.resolve("config.yaml");
        } else {
            configFile = null;
        }

        // Read the config file
        if (configFile != null) {
            config = YamlConvertor.fromFile(configFile.toFile());
        } else {
            config = new Config();
        }

        FileTemplateLoader loader = new FileTemplateLoader(source.resolve("template").toString());
        Handlebars handlebars = new Handlebars(loader);
        template = handlebars.compile(templateName);

        exploreAndBuild(source.toFile(), destination);
    }

    /**
     * Explores a directory and creates a build directory. When the explorer sees a directory, it
     * recursively explores the directory and copies it. When the explorer sees a file, it converts
     * the file or copy it in the build directory.
     *
     * @param rootDirectory root directory
     * @param buildDestination build directory
     */
    private void exploreAndBuild(File rootDirectory, Path buildDestination) throws IOException {
        // Build the files
        if (rootDirectory.isFile()) {
            fileBuilding(rootDirectory, buildDestination);
            return;
        }

        // Must be a directory
        if (!rootDirectory.isDirectory()) {
            return;
        }

        // Must not be the build directory
        if (rootDirectory.toPath().equals(destination)) {
            return;
        }

        if (rootDirectory.getName().equals(templateFolder)) {
            return;
        }

        Path buildDirectory = buildDestination;

        // Must not be the destination directory
        if (!rootDirectory.toPath().equals(source)) {
            // Create the subdirectory in build directory
            buildDirectory = buildDestination.resolve(rootDirectory.getName());
            Files.createDirectories(buildDirectory);
        }

        // Call recursively for each subdirectory and files
        File[] files = rootDirectory.listFiles();
        for (int i = 0; i < Objects.requireNonNull(files).length; ++i) {
            exploreAndBuild(files[i], buildDirectory);
        }
    }

    /**
     * Builds the given file
     *
     * @param file file to build
     * @param destination directory inside build folder
     */
    private void fileBuilding(File file, Path destination) throws IOException {
        if (file.getName().endsWith(".md")) {
            // Convert the markdown file
            Path htmlFile = destination.resolve(file.getName().replaceFirst("\\.md$", ".html"));
            Page page = new Page(file.toPath(), htmlFile, template);
            page.render(config);

        } else if (!file.getName().endsWith(".yaml") && !file.getName().endsWith(".yml")) {
            // Copy path
            Files.copy(file.toPath(), destination.resolve(file.getName()));
        }
    }
}
