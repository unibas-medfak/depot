@param String banner
@param String version
@param String thread

@template.layout.main(content = @`
    <pre>${banner}</pre>
    <span>${version} on ${thread} ready to serve you.</span>
`)
