package ch.unibas.medizin.depot.api;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@NullMarked
public class UiController {

    @GetMapping({"/browse", "/browse/**", "/view/**"})
    public String forward() {
        return "forward:/index.html";
    }

}
