@import ch.unibas.medizin.depot.dto.FileDto
@import java.util.List

@param String directory
@param List<FileDto> files

@template.layout.main(content = @`
    <div id="filelist">
        <h2>${directory}</h2>
    @if(files.isEmpty())
        <p>You have no items</p>
    @else
        <ul>
            @for(FileDto file : files)
                @if(file.type().equals(FileDto.FileType.FOLDER))
                    <li><a href="" hx-get="/cd?path=${directory}/${file.name()}" hx-target="#filelist">${file.name()}</a></li>
                @else
                    <li>${file.name()}</li>
                @endif

            @endfor
        </ul>
    @endif
    </div>
`)