<div align="center">
        <img alt="LocApp" src="https://dummyimage.com/420x100/000/fff&text=LocApp" />

        # LocApp
  
        Lightweight Localization Workflow CLI for Java `.properties` files
</div>

## Overview
LocApp is a modern command line application that recursively scans a source root for `.properties` files, normalizes and stores their key/value, path, locale and version information in an embedded H2 database. It then enables an iterative translation workflow using Excel export/import, delta comparisons and integrity checks. The data model tracks lifecycle states (source, exported, translated, verified, etc.) to support controlled evolution of localized resources.

## Key Features
- **Modern Interactive CLI** with TAB completion powered by Picocli and JLine 3
- Recursive discovery of property files (per language: `messages_de.properties`, `messages_en.properties`, ...)
- Versioned storage of entries in embedded H2 (no external DB required)
- Excel round‑trip: export for translators, reimport updates, highlight changes
- Delta export/import to minimize translator workload
- Integrity and completeness checks per language
- Ignore list support to exclude files from translation scope
- CSV migration/import tooling (legacy or external sources)
- Configurable default language for column ordering

## Technology Stack
- Java 21+
- Apache Maven (build)
- **Picocli 4.7.6** – Modern CLI framework with annotation-based command parsing
- **JLine 3.26.3** – Interactive terminal with TAB completion and command history
- H2 Database (TCP server started locally for inspection if needed)
- JPA (Jakarta Persistence) for entity management
- Apache POI 5.3.0 (XSSF / `.xlsx` format)
- Apache Commons CSV / IO / Lang3 utilities

## 💡 The LocApp Process Model (SRC vs. XLS)

The core logic of LocApp relies on separating source data from the translation working copy. This ensures that missing translations become transparent without directly manipulating the source files.

| Status | Data Structure | Command | Purpose |
| :--- | :--- | :--- | :--- |
| **SRC** | **Sparse Data (Thin)** | `ip` | Reflects the exact content of the physical `.properties` files in the code repository. Counts only existing keys. |
| **XLS** | **Dense Matrix (Thick)** | `ee` / `ei` | Creates a complete translation matrix, based on all keys of the main language, multiplied by all available target languages. This exposes missing translations (gaps). |

The typical workflow involves updating the **XLS Matrix** and consolidating the **SRC Baseline** through a final roundtrip.

## Typical Workflow
1. **Initialize & Scan:** `files <ROOT>` followed by `import-properties` (`ip`). This stores the initial **Sparse Data** set in **SRC** status.
2. **Export for Translation:** `excel-export <DIR> [LANG] [E]` (`ee`). Creates the **Dense Matrix** in Excel format.

   > **💡 Tip: Export only empty values for a specific language**
   >
   > If you want to export only the keys that are missing translations for a specific language (e.g., French), you can combine the `-l` and `-e` flags:
   >
   > ```bash
   > ee <DIR> -l fr -e
   > ```
   >
   > This will generate an Excel file containing only the keys where the French translation is still empty. This is ideal for sending translators just the "open gaps" for their language, instead of the full matrix.

3. **Translator Work:** Translator edits Excel (fill empty cells, adjust values).
4. **Import & Matrix-Update:** `excel-import <DIR>` (`ei`). Updates the **Dense Matrix** in **XLS** status.
5. **Integrity Check:** `check-integrity [LANG]` (`ci`). Verifies the coverage of all Source keys in the XLS matrix.
6. **Promotion to Source Baseline:** After successful check, perform the steps **`ep` $\rightarrow$ Filesystem Update $\rightarrow$ `ip` (new SRC Baseline)**.

   > **💡 Tip: Delta Export (`ep -d` / `--delta`)**
   >
   > If you only want to export property files that have changed (instead of always exporting all), use `ep -d <DIR>`. This saves time and avoids unnecessary file changes—especially in large projects or CI/CD workflows. The tool automatically detects which files have changed since the last SRC baseline import and exports only those.
   >
   > **Example:**
   > ```bash
   > # Export only changed files
   > ep -d <PROJECT_ROOT>
   > ```
   >
   > Without `-d`, all files are always exported; with `-d`, only the actually changed ones are exported.

7. **Consolidation:** `merge-properties SRC|XLS` (`mp`). Consolidates the latest version of SRC or XLS.
8. Optional: `csv-import` / `cscmig` and Housekeeping (`iil`, `cil`, `cl`).

## Command Reference

All commands follow modern Picocli CLI conventions, meaning that extensive help and command-specific options are available by adding the **`-h`** or **`--help`** flag to the command (`cmd -h`).

| Shortcut | Command | Parameters | Description |
|----------|---------|------------|-------------|
| q | quit | – | Quit the program. |
| h | help | – | Display help information about the specified command. |
| f | files | `<DIR>` | Read recursive down for properties files and save fileinfo. |
| ip | import-properties | – | Iterate known properties and save into database. |
| ee | excel-export | `<DIR> [-l LANG] [-e]` | Export properties into an excel file (all or by language ISOCODE, search for empty values). |
| ei | excel-import | `<DIR>` | Import properties from an excel file using MERGE strategy (inherits from previous XLS version). |
| ep | export-properties | `<DIR>` | Iterate known properties and save into directory. Use `--delta` to export only modified files. |
| i | init | – | Initialize the application with default configuration. |
| ed | export-delta | `<DIR>` | Export delta (properties vs. excel) into an excel file. |
| id | import-delta | `<DIR> <VERSION>` | Import delta and merge with selected version. |
| sdl | set-default-language | `<LANG>` | Set default language for excel export (LANG=[en, de...]). |
| pc | properties-count | `<SRC\|XLS> [-l LANG] [-e]` | Count the amount of properties (all or by language ISOCODE, search for empty values). |
| mp | merge-properties | `<SRC\|XLS>` | All known properties will be merged with their latest version to a new data set. |
| ci | check-integrity | `[-l LANG]` | Check if all SRC properties provided by XLS properties with all or specified languages. |
| cl | clear-loc | – | Delete all(!) entries for Localization! |
| iil | import-ignore-list | `<FILE>` | Import list of files that are to be excluded from the translation process. |
| cil | clear-ignore-list | – | Clear list with ignored files. |
| csvin | csv-import | `<FILE>` | Import properties as CSV formatted file. |
| s, find | search | `<QUERY>` | Search for translations by key or value. |

### Command Options
- `-l, --language` – Filter by language ISO code (e.g., `de`, `en`, `fr`)
- `-e, --empty` – Filter/export only empty properties
- `-h, --help` – Display help for any command
- `-V, --version` – Display version information

## Installation
```bash
    git clone [https://github.com/geramaya/locapp.git](https://github.com/geramaya/locapp.git)
    cd locapp
    mvn clean package
```