# Tools considerations

We need to repack .deb: the default packer creates incomplete metadata.

unpack single file:

    dpkg --fsys-tarfile raysearch_1.0.2-1_amd64.deb | tar xOf - ./p^Ch/to/file

replace single file?
