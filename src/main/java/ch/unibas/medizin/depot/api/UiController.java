package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.VersionHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UiController {

    private final VersionHolder versionHolder;

    public UiController(VersionHolder versionHolder) {
        this.versionHolder = versionHolder;
    }

    @GetMapping("/")
    public String info() {
        return "depot " + versionHolder.getVersion() + " ready.";
    }

}
