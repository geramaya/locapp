<div align="center">
        <img alt="LocApp" src="https://dummyimage.com/420x100/000/fff&text=LocApp" />
  
        # LocApp
  
        Lightweight Localization Workflow CLI for Java `.properties` files
</div>

## Overview
LocApp is a modern command line application that recursively scans a source root for `*.properties` files, normalizes and stores their key/value, path, locale and version information in an embedded H2 database. It then enables an iterative translation workflow using Excel export/import, delta comparisons and integrity checks. The data model tracks lifecycle states (source, exported, translated, verified, etc.) to support controlled evolution of localized resources.

## Key Features
- **Modern Interactive CLI** with TAB completion powered by [Picocli](https://picocli.info/) and [JLine 3](https://github.com/jline/jline3)
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

## Data Model
`Localization` entity fields:
`key`, `value`, `locale`, `fileName`, `fullPath`, `version`, `creationDate`, `status` (`SRC`, `XLS`, `TRANSLATED`, `VERIFIED`, `REJECT`, `DONE`, `CSV`).
Each (key + fileName + fullPath + status + version) tuple is unique, allowing historical versions and workflow transitions.

## Typical Workflow
1. Initialize & scan: `files <ROOT>` then `import-properties`
2. Export for translation: `excel-export <DIR> [LANG] [E]`
3. Translator edits Excel (fill empty cells, adjust values)
4. Import changes: `excel-import <DIR>` or delta refine via `export-delta` / `import-delta <DIR> <VERSION>`
5. Count / assess progress: `properties-count SRC|XLS [LANG] [E]`
6. Integrity check: `check-integrity [LANG]` (verifies coverage across locales)
7. Merge latest versions: `merge-properties SRC|XLS`
8. Optional CSV operations: `csv-import` / `cscmig`
9. Housekeeping: `import-ignore-list`, `clear-ignore-list`, `clear-loc`

## Command Reference

All commands support the `--help` option to display usage information. Commands can be invoked using either the full name or the short alias.

| Shortcut | Command | Parameters | Description |
|----------|---------|------------|-------------|
| q | quit | – | Quit program |
| h | help | – | Show help text |
| f | files | `<DIR>` | Recursively scan for `.properties` files |
| ip | import-properties | – | Import discovered properties into DB (status=SRC) |
| ee | excel-export | `<DIR> [-l LANG] [-e]` | Export properties to Excel (filter by language / empty values) |
| ei | excel-import | `<DIR>` | Import Excel modifications (status=XLS) |
| ep | export-properties | `<DIR>` | Export properties to directory |
| i | init | – | Initialize configuration |
| ed | export-delta | `<DIR>` | Export only changed/empty cells to Excel |
| id | import-delta | `<DIR> <VERSION>` | Import a delta sheet merging against a chosen version |
| sdl | set-default-language | `<LANG>` | Set default language column ordering |
| pc | properties-count | `<SRC\|XLS> [-l LANG] [-e]` | Count entries (optionally per language / empty) |
| mp | merge-properties | `<SRC\|XLS>` | Merge latest versions into new dataset |
| ci | check-integrity | `[-l LANG]` | Validate all source keys covered in XLS version(s) |
| cl | clear-loc | – | Delete ALL localization entries (dangerous) |
| iil | import-ignore-list | `<FILE>` | Import file list to ignore |
| cil | clear-ignore-list | – | Remove all ignored entries |
| csvin | csv-import | `<FILE>` | Import from CSV source (status=CSV) |
| cscmig | cscmig | `<FILE>` | Specialized CSV migration command |

### Command Options
- `-l, --language` – Filter by language ISO code (e.g., `de`, `en`, `fr`)
- `-e, --empty` – Filter/export only empty properties
- `-h, --help` – Display help for any command
- `-V, --version` – Display version information

## Installation
```bash
git clone https://github.com/geramaya/locapp.git
cd locapp
mvn clean package
```

Resulting artifact (fat JAR) appears under `target/locapp-<version>-jar-with-dependencies.jar`.

## Running
```bash
java -jar target/locapp-<version>-jar-with-dependencies.jar
```
On startup LocApp:
1. Prints splash banner
2. Initializes Picocli command framework and JLine terminal
3. Starts H2 TCP server + JPA (`H2DatabaseManager`)
4. Enters interactive CLI loop with TAB completion

### Interactive Shell Features
- **TAB Completion**: Press TAB to autocomplete command names and options
- **Command History**: Use arrow keys to navigate through previous commands
- **Graceful Exit**: Use `quit`, `q`, or Ctrl+D to exit; Ctrl+C shows exit hint

### Example Session
```text
>> command: files /path/to/project/src/main/resources
>> command: import-properties
>> command: excel-export ./export -l en -e
>> command: excel-import ./export
>> command: check-integrity -l en
```

### New Command Syntax Examples
```bash
# Export Excel with language filter and empty properties only
excel-export /tmp/export --language de --empty

# Or using short options
ee /tmp/export -l de -e

# Count properties with options
properties-count SRC --language fr --empty

# Check integrity for specific language
check-integrity --language en
```

## Configuration & Storage
- Database files are stored under user home: `~/.locapp/`.
- H2 2.x is used; upgrading from older 1.4.x requires a fresh start (see migration below).
- Ignore list allows excluding non‑translatable property files.

## Excel Handling
`ExcelHandler` builds styled sheets (header + change highlighting). Changed cells are tracked and colored. Current implementation sets fixed column widths and highlights modifications via a change style.

## Migration from H2 1.4.x to 2.x
If upgrading from legacy versions:
1. Export critical data (Excel).
2. Remove old DB files: `rm -f ~/.locapp/*.db`
3. Restart LocApp – a new schema is created automatically.

## Testing
Use Maven Surefire (already configured). To run tests:
```bash
mvn test
```
Test reports: `target/surefire-reports/`.

## Development Notes
- **CLI Framework**: Commands use Picocli annotations (`@Command`, `@Parameters`, `@Option`) for declarative parsing
- **Root Command**: `LocAppCLI.java` registers all subcommands with aliases for backward compatibility
- **Interactive Shell**: `MainStart.java` integrates JLine 3 for terminal handling with `StringsCompleter` for TAB completion
- **Backward Compatibility**: Commands implement both `CommandRunnable` (legacy) and `Runnable` (Picocli) interfaces
- Legacy `CommandContext` is still available for gradual migration
- `LocalizationDao` provides filtered queries (by status, version, locale, empties, path) and ignore‑list filtering
- Status transitions are implicit through import/export commands

### Architecture Overview
```
MainStart.java          → Application entry point, JLine terminal setup
    ↓
LocAppCLI.java         → Picocli root command, registers all subcommands
    ↓
*Command.java          → Individual commands with @Command annotations
    ↓
CommandContext.java    → Legacy command registry (backward compatibility)
```

## Roadmap Ideas
- ~~Switch to `XSSFWorkbook` for `.xlsx` support~~ ✅ Done
- ~~Modern CLI with TAB completion~~ ✅ Done (Picocli + JLine 3)
- Introduce structured logging (SLF4J) & log configuration
- Add unit tests for individual command classes
- Provide JSON/REST API wrapper for CI automation
- Add progress summaries (percentage translated / verified)
- Optional encryption for local DB if handling sensitive strings

## License
See `LICENSE.md` for details.

## Contributing
PRs welcome. Please:
1. Create a feature branch
2. Add/update tests where sensible
3. Keep README and help output consistent

## Troubleshooting
- Unknown command: verify spelling (`help` to list all)
- Empty export: ensure you ran `import-properties` first
- Slow queries: large datasets may benefit from adding indexes (future enhancement)
- Excel encoding issues: ensure your editor preserves UTF‑8 without BOM

## Security Considerations
- Runs locally; no network exposure except H2 TCP (default port). Disable server if not needed.
- Validate external CSV inputs to avoid injection in property values.

---
*This README was generated to provide a comprehensive overview of the LocApp project and its functionalities.*
