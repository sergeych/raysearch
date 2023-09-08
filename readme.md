# Missing Linux GUI app to full-text search files

> See current release in the releases, now 1.0 is published.
> the [release 1.0 .deb](https://github.com/sergeych/raysearch/releases/download/v1.0.4/raysearch_1.0.4-1_amd64.deb).

__It indexes:__

- program sources, plain text documents in UTF8, ASCII, ISO/IEC-88591 - autodetecting contents, markdows, etc.
- Libre Office or other ODF text documents and spreadsheets.
- The text conent of the .pdf. Note that often pdf has no text.

# Search all your texts easily!

| search string           | meaning                                                                                |
|-------------------------|----------------------------------------------------------------------------------------|
| `foo`                   | all files where with `foo`                                                             |
| `foo bar`               | all files with foo AND bar                                                             |
| `foo bar buzz`          | all files with foo AND bar AND buzz                                                    |
| `foo bar *.txt`         | all files with extension .txt with foo AND bar                                         |
| `foo* bar`              | ...with anyting starting with foo and bar                                              |
| `f*bar`                 | could be anything between, fubar, foobar, fearbar                                      |
| `f?bar`                 | could be any single character between, fubar, febar, but _not foorbar_ (2 chars)       |
| `MIT license readme.md` | all readme.md files that cope with MIT license                                         |
| `f:myscript rendering`  | any files named `myscript` containig the `rendering` word. IT supports shebank scripts |

![](screenshots/home1.png)

Raysearch scans the while user home directory for text files, and indexes them all. IT is written to walk FS in parallel
with indexing data in already discovered files. It stores file tree metadata and reacts to changes. It is intended to be
left in the background to update indexes as the FS changes and have it ready at hand.

Results are live: whenever it scans more data, or filesystem changes, the results _might_ change. Results allow opening
containing folders and files using the system defaults (using gio).

## Fast start

The application is ready to search immediately - even while scanning. IT will automatically add more entries to your
search as it scans more. For example, if you enter search, then copy some files from say USB, it will soon appear in the
result list (if the copied files will match). The same, when you edit/delete matching files, it will dissapear from
search.

The app build indexes my home folder in 26s - and refresh in 6. Still you can just autostart it minimized:

## Scanning in the background

_Just minimize the app and leave it open!_ It will catch file changes and update databases on the fly not eating too
many resources. It is limited to two cores/threads while scanning and one thread while indexing by purpose. It is fast
enough, so it does not want to affect your activities.

## Scanning details

On start, it rescans user home (a delta from last start) for all texts it can find, avoiding non-reasonable things like

- node moules where it detects npm/yarn usage
- build directories in yarn projects
- executable binaries (shebang scripts are ok)
- binary files that are unlikely to be text: it detects valid latin-1 (ISO/IEC 8859-1)/ASCII and UTF-8, and ignores
  other files.
- system, hidden, dot-starting giles.
- archives, multimedia, any known binary data formats

Meanwhile, it detects and indexes ODF documents (LibreOffice).

I _will_ also index pdf texts (where there are), I'll add it soon. With the rest - I'd appreciate some help. Don't have
that much time. And sincerely I don't want to deal with proprietary, non-free formats, especially now.

## How to run

Install from .deb file available in releases (see the right panel). Start with the most recent!

## Note on the cryptography

This software does not implement or use any cryptograpy. Thus, it can't index encrypted files. It does not check ignature on signed files. While some cryptographic functions are included as a part of some libraries this software uses, it does not use it.

## Why reinwenting the wheel?

Reason d'être: I didn't find anything like, fast and neat, and grep on my software only archives takes minutes. This
thing runs in split second most often. And I don't trust clouds and googles for my work and private life: I was born in
the USSR and I am a true supporter of common sense and open source. This is enough to be banned from US/EU services
nowdays ;)

## Other stuff

8 RAYS DEV is my software development company, far enough from the crazy world, in quiet, nice and hot Tunis ;)
Welcome https://8-rays.dev. Unix rules!

## License

I will license it under MIT real soon. Just trying to comply with apache licensed libraries.

All this will soon receive MIT license - well, if they won't add clause agains red polo shirts or something worse. Right
now the code is useless so no license yet. Contact me if you need something particularly strange. I need some advice
whether I can publish it under MIT while it uses Lucene, which is Apache.
