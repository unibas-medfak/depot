@import ch.unibas.medizin.depot.dto.FileDto
@import java.util.List

@param String directory
@param List<FileDto> files

@template.layout.main(content = @`
    @template.list(directory = directory, files = files)
`)
