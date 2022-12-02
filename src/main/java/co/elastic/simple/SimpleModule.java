package co.elastic.simple;

import jdk.crac.CheckpointException;
import jdk.crac.Core;
import jdk.crac.RestoreException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SimpleModule {
    public static void main(String[] args) throws RestoreException, CheckpointException, IOException {
        final URLClassLoader classLoader = buildClassLoader();
        final Supplier<?> console = buildConsoleLoader(classLoader);
        classLoader.close();
        Core.checkpointRestore();
        System.out.println("Hello modules!");
        System.out.println(console.get().toString());
    }

    static Supplier<?> buildConsoleLoader(ClassLoader classLoader) {
        try {
            final Class<? extends Supplier<?>> cls = (Class<? extends Supplier<?>>) classLoader.loadClass("org.elasticsearch.io.ansi.AnsiConsoleLoader");
            final Constructor<? extends Supplier<?>> constructor = cls.getConstructor();
            final Supplier<?> supplier = constructor.newInstance();
            return supplier;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to load ANSI console", e);
        }
    }

    private static URLClassLoader buildClassLoader() {
        final Path libDir = Path.of(".").resolve("lib").resolve("ansi-console");

        try {
            final URL[] urls = Files.list(libDir)
                    .filter(each -> each.getFileName().toString().endsWith(".jar"))
                    .map(SimpleModule::pathToURL)
                    .toArray(URL[]::new);

            return URLClassLoader.newInstance(urls, SimpleModule.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list jars in [" + libDir + "]: " + e.getMessage(), e);
        }
    }

    private static URL pathToURL(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            // Shouldn't happen, but have to handle the exception
            throw new RuntimeException("Failed to convert path [" + path + "] to URL", e);
        }
    }
}
