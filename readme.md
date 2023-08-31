# Missing GUI app to search in indexed file tree content

> Work in progress, though it generally works.

The system scans and indexes files in parallel, therefire works very fats. It tries to index only the
files it can extract the text to, so no binaries, no archives (yet), etc. It index bash seripts adn 
shebangs, but excludes git repositories, build directories, node_moules, etc.

## How to run

Right now, clone it and use gradle

## Why reinwenting the wheel

Reason d'être is the following: For my 35 years of coding, my source archive became too big and still sometimes
I know there is something like I need to repeat, but most I can remember exactly where. Full text search was helping
me on the mac, but I miss in on the linux, so I'm implementing it as GUI which is more convenient for this task as
for me. Commercial services like clouds and like are too vulnerable to international madness, so I'm doing it as open source under appropriate license (most likely apache as I'm using lucene).

For examplem Last time I was looking in my "small" archive (last 7 years) with grem, it took 5 minutes on my superfast dell.

As I am too old to believe again in adequate humanity, I can't believe in proprietary OS and
clouds as tomorrow they will ban me, say, as I have a slavic name or a red polo shirt ;) Something like this already happened to me and enough is enough.

So I'm writing a comprehensive GUI app for linux to let me search fast in my enormous codebases and 
documents. It's, well, way too big for grep even with history ;) So I started. Hope I'll finish it and somebody else would then share my pleasure of having fast content indexed search in Linux. As a GUI too ;)

## Technical details

I need it to be self-contained but reasonably fast, and use little of my time, that's why I selected compose for desktop, kotlin, and H2. Thus, I can use lots of useful tools, either mine which I am used to, and a lot more of another open source. For example, much less problem will be to extract a text from documents like odf and pdf, etc. Also, I might use Lucene later for fine-grained search. 

To me or to whom it many concern.

Scanned directories and files are stored in the H2 database to determine which ones require re-scanning.
ScanFolder records create ScanFile records which are sort of tasks to perform in the background. I plan to load 10-15% of cores with indexing this way. Rescans could be then implementing watching fs events and browsing folders with changed mtime.

- SearchFolder is a DB-held model of folder tree structire. Used to discover changes and walk the tree.
  - it is possible to have more than one root SearchFilder to index separated trees (e.g., with mountable media)
- FileDoc is a single file record in the DB, contains its last known (processed) mtime and size and the detected suitable text extratror.
- FileScanner works in the background peeking changed/new files (provided by SeachFolder.rescan)
- UI uses compose. So far so good, but I hate the fonts.

Rescan should be started when FS change event is detected, and on start.

## License

All this will soon receive MIT license - well if they won't add clause agains red polo shirts or something worse. Right now the code is useless so no license yet. Contact me if you need something particularly strange.