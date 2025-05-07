package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.VersionHolder;
import ch.unibas.medizin.depot.service.AuthorizationService;
import ch.unibas.medizin.depot.service.DepotService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Controller
public class UiController {

    private final AuthorizationService authorizationService;

    private final DepotService depotService;

    private final VersionHolder versionHolder;

    private final String banner;

    public UiController(AuthorizationService authorizationService, DepotService depotService, final VersionHolder versionHolder) {
        this.authorizationService = authorizationService;
        this.depotService = depotService;
        this.versionHolder = versionHolder;

        try (final var reader = new InputStreamReader(new ClassPathResource("banner.txt").getInputStream(), StandardCharsets.UTF_8)) {
            this.banner = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping( "/")
    public String index(Model model) {
        var tokenData = authorizationService.getTokenData("default", "realm", "subject");
        var files = depotService.list("/", tokenData);

        model.addAttribute("directory", "/");
        model.addAttribute("files", files);
        return "pages/index";
    }

    @HxRequest
    @GetMapping("/cd")
    public String list(@RequestParam final String path, Model model) {
        var tokenData = authorizationService.getTokenData("default", "realm", "subject");
        var files = depotService.list(path, tokenData);
        model.addAttribute("directory", path);
        model.addAttribute("files", files);
        return "list";
    }

    @GetMapping( "/info")
    public String info(Model model) {
        model.addAttribute("banner", banner);
        model.addAttribute("version", versionHolder.getVersion());
        model.addAttribute("thread", Thread.currentThread().toString());
        return "pages/info";
    }

}
