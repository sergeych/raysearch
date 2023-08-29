# Missing GUI to search in indexed file content

> just started

Reason d'être: For my 35 years of coding my sources archive is unreally big and still sometimes
I know there is something like I need, but most I can remember is some key words or class names
or so. The same, though much better, is with documents that before GoogleDoc era were, and after
the last world are again as plain files, no more indexed by the cloud.

As I am too old to believe again in adequate humanity, I can't believe in proprietary OS and
clouds as tomorrow they will ban me, say, as I have a slavic name or a red polo shirt ;) Something like this already happened to me and enough is enough.

So I'm writing a comprehensive GUI app for linux to let me search fast in my enormous codebases and 
documents. It's, well, way too big for grep even with history ;) So I started. Hope I'll finish it and somebody else would then share my pleasure of having fast content indexed search in Linux. As a GUI too ;)

## Technical details

I need it to be self-contained but reasonably fast, and use little of my time, that's why I selected compose for desktop, kotlin, and H2. Thus, I can use lots of useful tools, either mine which I am used to, and a lot more of another open source. For example, much less problem will be to extract a text from documents like odf and pdf, etc. Also, I might use Lucene later for fine-grained search. 

To me or to whom it many concern.

Scanned directories and files are stored in the H2 database to determine which ones require re-scanning.
ScanFolder records create ScanFile records which are sort of tasks to perform in the background. I plan to load 10-15% of cores with indexing this way. Rescans could be then implementing watching fs events and browsing folders with changed mtime.

So, we perform directory structire scan more or less fast and create massive "job" in a database as FileScan records with empty checkedMtime field, which will small by small be performed without eating too many cores. It will also let us display progress in a plausible manner.

## Still Gitgub?

Well I was a fan of it, now I'll move active development to my own server anyway. I presume the weird effect of mass mental disorder is just starting. Wll the most interesting is just "incooominnng" :))))

## License

All this will soon receive MIT license - well if they won't add clause agains red polo shirts or something worse. Right now the code is useless so no license yet. Contact me if you need something particularly strange.