package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DepotService {
    List<FileDto> list(String path);
    Resource get(String file);
    PutFileResponseDto put(MultipartFile file, String path, boolean hash);
}
