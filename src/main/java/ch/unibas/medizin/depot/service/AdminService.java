package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.dto.LogRequestDto;

import java.util.List;

public interface AdminService {

    List<String> getLastLogLines(LogRequestDto logRequestDto);

}
