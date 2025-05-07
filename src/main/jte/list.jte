@import ch.unibas.medizin.depot.dto.FileDto
@import java.util.List

@param String path
@param String parent
@param List<FileDto> files

@template.layout.main(content = @`
    <div id="filelist">
    @if(path.isEmpty())
        <h2>/</h2>
    @else
        <h2>${path}</h2>
        <a href="" hx-get="/cd?path=${parent}" hx-target="#filelist">..</a>
    @endif
    @if(files.isEmpty())
        <p>You have no items</p>
    @else
        <ul>
            @for(FileDto file : files)
                @if(file.type().equals(FileDto.FileType.FOLDER))
                    <li><a href="" hx-get="/cd?path=${path}/${file.name()}" hx-target="#filelist">${file.name()}</a></li>
                @else
                    <li><a href="/fetch?file=${path}/${file.name()}" target="_blank">${file.name()}</a></li>
                @endif
            @endfor
        </ul>
    @endif
    </div>
`)