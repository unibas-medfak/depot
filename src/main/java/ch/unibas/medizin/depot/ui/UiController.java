package ch.unibas.medizin.depot.ui;

import ch.unibas.medizin.depot.config.VersionHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Controller
public class UiController {

    private final VersionHolder versionHolder;

    private final String banner;

    public UiController(final VersionHolder versionHolder) {
        this.versionHolder = versionHolder;

        try (final var reader = new InputStreamReader(new ClassPathResource("banner.txt").getInputStream(), StandardCharsets.UTF_8)) {
            this.banner = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ResponseBody
    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String info() {
        return banner + "\nversion " + versionHolder.getVersion() + " on " + Thread.currentThread() + " ready to serve you.";
    }

}
