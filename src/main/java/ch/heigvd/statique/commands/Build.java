package ch.heigvd.statique.commands;

import ch.heigvd.statique.convertors.Builder;
import ch.heigvd.statique.utils.Watcher;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/** This command builds a statique site from a provided folder name. */
@Command(name = "build", description = "Build a static site")
public class Build implements Callable<Integer> {

    /** The site to build */
    @Parameters(paramLabel = "SITE", description = "The site to build")
    public Path site;

    @Option(
            names = {"--watch"},
            description = "Keeps building the site when changes occurred")
    private boolean haveWatcher = false;

    /** Calls the builder to build the provided site. */
    @Override
    public Integer call() throws IOException {
        // Check that the site folder exists.
        if (!Files.exists(site)) {
            System.err.println("Destination does not exists");
            return -1;
        }

        // Verify that destination is a folder.
        if (!Files.isDirectory(site)) {
            System.err.println("Destination is not a folder");
            return -1;
        }

        Builder builder = new Builder(site, site.resolve("build"));
        builder.build();
        System.out.println("Build done");

        // Constantly builds the site when changes occurred
        if (haveWatcher) {
            new Thread(new Watcher(site)).start();
        }

        return 0;
    }
}
