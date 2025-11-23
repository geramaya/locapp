<div align="center">
        <img alt="LocApp" src="https://dummyimage.com/420x100/000/fff&text=LocApp" />
  
        # LocApp
  
        Lightweight Localization Workflow CLI for Java `.properties` files
</div>

## Overview
LocApp is a command line application that recursively scans a source root for `*.properties` files, normalizes and stores their key/value, path, locale and version information in an embedded H2 database. It then enables an iterative translation workflow using Excel export/import, delta comparisons and integrity checks. The data model tracks lifecycle states (source, exported, translated, verified, etc.) to support controlled evolution of localized resources.

## Key Features
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
- H2 Database (TCP server started locally for inspection if needed)
- JPA (Jakarta Persistence) for entity management
- Apache POI (currently HSSF / `.xls` format)
- Apache Commons CSV / IO / Lang3 utilities

> Note: The current Excel implementation uses `HSSFWorkbook` (legacy `.xls`). Migrating to `XSSFWorkbook` would enable larger sheets and modern Excel features.

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
| Shortcut | Command | Parameters | Description |
|----------|---------|------------|-------------|
| q | quit | – | Quit program |
| h | help | – | Show help text |
| f | files | `DIR` | Recursively scan for `.properties` files |
| ip | import-properties | – | Import discovered properties into DB (status=SRC) |
| ee | excel-export | `DIR [LANG] [E]` | Export properties to Excel (filter by language / empty values) |
| ei | excel-import | `DIR` | Import Excel modifications (status=XLS) |
| ed | export-delta | `DIR` | Export only changed/empty cells to Excel |
| id | import-delta | `DIR VERSION` | Import a delta sheet merging against a chosen version |
| sdl | set-default-language | `LANG` | Set default language column ordering |
| pc | properties-count | `SRC|XLS [LANG] [E]` | Count entries (optionally per language / empty) |
| mp | merge-properties | `SRC|XLS` | Merge latest versions into new dataset |
| ci | check-integrity | `[LANG]` | Validate all source keys covered in XLS version(s) |
| cl | clear-loc | – | Delete ALL localization entries (dangerous) |
| iil | import-ignore-list | – | Import file list to ignore |
| cil | clear-ignore-list | – | Remove all ignored entries |
| csvin | csv-import | – | Import from CSV source (status=CSV) |
| cscmig | cscmig | – | Specialized CSV migration command |

Parameters without brackets are mandatory; within brackets optional.

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
2. Registers commands (`CommandContext`) and runs initial setup (`init`)
3. Starts H2 TCP server + JPA (`H2DatabaseManager`)
4. Enters recursive CLI loop

### Example Session
```text
>> command: files /path/to/project/src/main/resources
>> command: import-properties
>> command: excel-export ./export en 1
>> command: excel-import ./export
>> command: check-integrity en
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
- Commands are registered centrally in `CommandContext` (shortcut + full name).
- Lifecycle is driven by `MainStart` (splash → help → init → CLI loop).
- `LocalizationDao` provides filtered queries (by status, version, locale, empties, path) and ignore‑list filtering.
- Status transitions are implicit through import/export commands.

## Roadmap Ideas
- Switch to `XSSFWorkbook` for `.xlsx` support
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
