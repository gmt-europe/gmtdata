package nl.gmt.data;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class UUIDTypeInsertingMetadataProvider extends JPAMetadataProvider {
    private final Map<AnnotatedElement, AnnotationReader> cache = new HashMap<>();
    private final MetadataProvider delegate;

    public UUIDTypeInsertingMetadataProvider(MetadataBuildingOptions metadataBuildingOptions, MetadataProvider delegate) {
        super(metadataBuildingOptions);

        this.delegate = delegate;
    }

    @Override
    public Map<Object, Object> getDefaults() {
        return delegate.getDefaults();
    }

    @Override
    public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
        // This method is called a lot of times on the same element, so annotation
        // readers are cached. We only cache our readers because the provider
        // we delegate to also caches them.

        AnnotationReader reader = cache.get(annotatedElement);
        if (reader != null) {
            return reader;
        }

        reader = delegate.getAnnotationReader(annotatedElement);

        // If this element is a method that returns a UUID, or a field of type UUID,
        // wrap the returned reader in a new reader that inserts the "pg-uuid" Type
        // annotation.

        boolean isUuid = false;
        if (annotatedElement instanceof Method) {
            isUuid = ((Method)annotatedElement).getReturnType() == UUID.class;
        } else if (annotatedElement instanceof Field) {
            isUuid = ((Field)annotatedElement).getType() == UUID.class;
        }

        if (isUuid) {
            reader = new UUIDTypeInserter(reader);
            cache.put(annotatedElement, reader);
        }

        return reader;
    }

    private static class UUIDTypeInserter implements AnnotationReader {
        private static final Type INSTANCE = new Type() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Type.class;
            }

            @Override
            public String type() {
                return "pg-uuid";
            }

            @Override
            public Parameter[] parameters() {
                return new Parameter[0];
            }
        };

        private final AnnotationReader delegate;

        public UUIDTypeInserter(AnnotationReader delegate) {
            this.delegate = delegate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            if (annotationType == Type.class) {
                return (T)INSTANCE;
            }

            return delegate.getAnnotation(annotationType);
        }

        @Override
        public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
            return annotationType == Type.class || delegate.isAnnotationPresent(annotationType);
        }

        @Override
        public Annotation[] getAnnotations() {
            Annotation[] annotations = delegate.getAnnotations();
            Annotation[] result = Arrays.copyOf(annotations, annotations.length + 1);
            result[result.length - 1] = INSTANCE;
            return result;
        }
    }
}
