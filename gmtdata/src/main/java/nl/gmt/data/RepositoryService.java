package nl.gmt.data;

import org.apache.commons.lang3.Validate;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RepositoryService {
    private final Map<Class<? extends Repository>, Class<? extends Repository>> classMap = new HashMap<>();

    public RepositoryService(URL... urls) {
        Validate.notNull(urls, "urls");

        Set<Class<? extends Repository>> classes = new HashSet<>();
        Set<Class<? extends Repository>> interfaces = new HashSet<>();

        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setScanners(new SubTypesScanner())
            .setUrls(urls)
        );

        for (Class<? extends Repository> repositoryClass : reflections.getSubTypesOf(Repository.class)) {
            if (repositoryClass.isInterface()) {
                interfaces.add(repositoryClass);
            } else if (!Modifier.isAbstract(repositoryClass.getModifiers())) {
                classes.add(repositoryClass);
            }
        }

        for (Class<? extends Repository> repositoryInterface : interfaces) {
            Class<? extends Repository> implementation = null;

            for (Class<? extends Repository> repositoryClass : classes) {
                if (repositoryInterface.isAssignableFrom(repositoryClass)) {
                    implementation = repositoryClass;
                    break;
                }
            }

            if (implementation == null) {
                throw new IllegalStateException(String.format("Cannot find implementation for '%s'", repositoryInterface));
            }

            classes.remove(implementation);

            classMap.put(repositoryInterface, implementation);
        }

        if (classes.size() > 0) {
            StringBuilder sb = new StringBuilder();

            for (Class<? extends Repository> repositoryClass : classes) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }

                sb.append('\'');
                sb.append(repositoryClass.getName());
                sb.append('\'');
            }

            throw new IllegalStateException(String.format("The following classes could not be mapped to an interface: '%s'", sb));
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public <T extends Repository> T getRepository(Class<T> repositoryClass) {
        Class<T> implementation = (Class<T>)classMap.get(repositoryClass);

        Validate.isTrue(implementation != null, String.format("Repository class '%s' is not mapped", repositoryClass));

        try {
            return implementation.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
