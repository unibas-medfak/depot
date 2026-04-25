package ch.unibas.medizin.depot.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class TikaRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("org/apache/tika/mime/tika-mimetypes.xml");
        hints.resources().registerPattern("org/apache/tika/mime/custom-mimetypes.xml");
    }

}
