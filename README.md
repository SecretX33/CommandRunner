# CommandRunner
CommandRunner is a multiplatform CLI tool that watch a folder tory and react to file changes by running the command(s) you specify. Some common use cases are:

- Recompiling a project automatically whenever you change, paste or delete a file.
- Re-running a script whenever you save a file to a folder or edit a file from that folder.

And above all, the possibilities are endless since you can basically use this with any other CLI tool available on your OS. Speaking of OS, this tool supports all 3 major OSs (Windows, Linux and MacOS), please file an Issue if you encounter a bug.

## Options

- `-?`, `--help`: display a list of all program flags with a basic description
- `-c`, `--command`: specifies a command that will run whenever a file event occur [may be used more than once to run more than one command at a time]
- `-p`, `--path`: changes the watched folder, by default it watches the current directory
- `-d`, `--delay`: delay the execution of the command by the specified time. Time can be specified with or without a time unit (e.g. `25` [25 millis] or `5s` [5 seconds]); the default time unit is **milliseconds**
  - Available time units: `d` days, `h` hours, `m` minutes, `s` seconds, `ms` millis, `us` micros, `ns` nanos
- `-dr`, `--disable-recursive`: by default children folders are also monitored, this option disables that and makes CommandRunner only watch files exactly under the watched folder
- `-f`, `--filter`: allows further customization of what files will trigger the commands, with this option, you can provide filters in an Ant-styled path matchers ([Quick Guide](https://confluence.atlassian.com/fisheye/pattern-matching-guide-960155410.html)) to select what files may trigger the commands [may be used more than once to add multiple file filters that will behave as **_OR_**]
- `-ig`, `--ignore-case`: by default `--filter` is case-sensitive, this option makes it case-insensitive
- `--min-size`: set a minimum size that a file must have. File size can be specified with or without a size unit (e.g. `10` [10 kilobytes] or `40mb` [40 megabytes]); the default size unit is **kilobytes**
  - Available size units: `t` `tb` terabytes, `g` `gb` gigabytes, `m` `mb` megabytes, `k` `kb` kilobytes, `b` bytes
- `--max-size`: set a maximum size that a file must have. Uses the same size format and units as the `--min-size`
- `-fc`: run commands on file **creation**; if none of the `-fX` flags are used, defaults to `-fc -fm`
- `-fm`: run commands on file **modification**; if none of the `-fX` flags are used, defaults to `-fc -fm`
- `-fd`: run commands on file **deletion**; if none of the `-fX` flags are used, defaults to `-fc -fm`
  - **Attention:** This option is disabled by default for a reason, it makes the commands trigger not only for _file_ deletion, but also _folder_ deletions too. This is an issue without a solution because since it is a deletion and Java do not provide the necessary information to determine if the deleted path is a file or a folder, there is no way of making the commands trigger only for file deletion, **you have been warned**


## Placeholders

Additionally, there are many placeholders that can be used in commands. Placeholders are strings that you can use in commands that will be replaced in runtime by their equivalent counterpart in the context of the file event. They are super simple to use, and to make it even clearer, let's not only describe it, but also use an example, assume the following:

**Watched folder:** `/project`

**File relative path:** `src/purple/File.txt`

Then, here's the list with all placeholders and what they would provide:

- `{filename}`: `File.txt`; the file name with extension
- `{filenamenoextension}`: `File`; the file name without the extension
- `{fileextension}`: `txt`; only the file extension, or an empty string if the file doesn't have any
- `{filepath}`: `/project/src/purple/File.txt`; the absolute path to the file, including the file name
- `{path}`: `/project/src/purple`; the absolute path to the file
- `{path+}`: `/project/src/purple/`; the absolute path to the file, adding a `pathseparator` at the end if the path is not empty
- `{relativefilepath}`: `src/purple/File.txt`; the relative path to the file, including the file name
- `{relativepath}`: `src/purple`; the relative path to the file
- `{relativepath+}`: `src/purple/`; the relative path to the file, adding a `pathseparator` at the end if the path is not empty
- `{pathseparator}`: `/`; the path separator of the current OS, may be useful to create some command that runs on any OS without needing to adapt anything

# Examples
Since CommandRunner offer many options, it can be hard to grasp how to properly use them, so here are some examples.

### Simplest possible command
```shell
java -jar CommandRunner.jar -p "~/Documents/MyProject" -c "echo I just created/modified file '{filename}'"
```

**Result**
```
I just created/modified file 'examplefile.txt'
```

### Multiple commands
```shell
java -jar CommandRunner.jar -p "~/Documents/MyProject" \
   -c "echo I just created/modified file '{filename}'" \
   -c "echo and I love command chains"
```

**Result**
```
I just created/modified file 'examplefile.txt'
and I love command chains
```

## Filtering
There are multiple filters available for you to use, remember that you can **combine** these filters to attend different needs.

### By file extension

This command will only trigger commands for `.txt` files.

```shell
java -jar CommandRunner.jar -p "~/Documents/MyProject" \
   -f "**.txt" \   # filtering by extension here
   -c "..."
```

### By multiple file extension

This command will only trigger commands for `.mov` and `.mp4` files. Multiple file filters will act like an **OR** filter (in this example, the file extension has to be `.mov` OR `.mp4`)

```shell
java -jar CommandRunner.jar -p "~/Documents/MyProject" \
   -f "**.mov" -f "**.mp4" \   # filtering by extension here
   -c "..."
```

### Only files in a certain folder

To only trigger commands for files that are inside a folder called `loop`.

```shell
java -jar CommandRunner.jar -p "~/Documents/MyProject" \
   -f "loop/*" \  # will filter files inside folder 'loop'; this folder can only on the root of the watched folder
   -f "**/loop/*" \   # will filter files inside folder 'loop'; this folder can be on the root or nested inside the watched folder
   -c "..."
```

### By file size

File size filters may be used alone (e.g. use only `--min-size` or `--max-size`), or used together like in this example.

```shell
java -jar CommandRunner.jar -p "~/Documents/MyProject" \
   --min-size "10mb" \    # only files that have at least 10 megabytes
   --max-size "1gb" \     # and at most 1 gigabyte
   -c "..."
```

# Building

CommandRunner uses Gradle to handle dependencies & building.

#### Requirements
* Java 17 JDK or newer

### Compiling from source

```shell
cd CommandRunner       # whenever you extracted the source code
./gradlew shadowJar    # the result Jar will be located at build/libs/CommandRunner.jar
```

# License
CommandRunner is licensed under **AGLP 3.0** license. Please see [`LICENSE`](LICENSE) for more info.
