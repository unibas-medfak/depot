@import ch.unibas.medizin.depot.dto.FileDto
@import java.util.List

@param String path
@param String parent
@param List<FileDto> files

@template.layout.main(content = @`
    @template.list(path = path, parent = parent, files = files)
`)
