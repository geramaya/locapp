
## 🚀 LocApp Happy Path: Full Translation Roundtrip

This scenario demonstrates the intended **Sparse Data (SRC) to Dense Matrix (XLS) Promotion** workflow, ensuring that all missing translations are captured, completed, and written back to the definitive Source files. The integrity check will validate the final consistency.

The full, verified command sequence is: `cl, f, ip, ee, <External Edit>, ei, ep, ip, ci`.

### 1. Initial State & Setup

The project initially contains **sparse** `.properties` files. The goal is to promote the localized content to a new, dense **Source Baseline**.

| Variable | Example Value |
| :--- | :--- |
| **Source Status** | **SRC V1: 10 Entries** (Sparse Data) |
| **Matrix Size** | **16 Entries** (4 Keys $\times$ 4 Languages) |

#### 1.1. Setup & Initial Import

We establish the initial state by clearing old data, scanning the project, and performing the first `ip` import.

| Step | Command (Simulation) | Description |
| :--- | :--- | :--- |
| **1.** | `cl` | Delete all existing Localization entries from the database. |
| **2.** | `f <PROJECT_ROOT>` | Scan the project directory and register all `.properties` files. |
| **3.** | `ip` | Import the raw, sparse Source files. Creates **SRC V1**. |
| **4.** | `pc src` | **Verification:** Confirms the sparse data count. |

```text
>> command: pc src
...
INFORMATION: The amount of properties type SRC is 10 [language=null, englishDefaultAmount=4, status=SRC].
```

-----

### 2. Translation & Matrix Completion (XLS V3)

The `ee` command creates the 16-entry dense matrix, which the translator completes in two fictional rounds (V2 & V3).

#### 2.1. First Export & Import (XLS V2)

```bash
# 5. Export dense matrix (ee)
>> command: ee <TEMP_DIR>
# [...] Excel file exported: 20251128-HHMMSS-export-all.xlsx

# 6. External Edit (Simulation)
# The translator fills most of the empty gaps (e.g., key.foobar in IT/FR, key.foobar2/3 in DE).
# This creates a mostly complete matrix, but 4 empty entries remain (e.g., IT/FR for key.foobar2/3).

# 7. Import partially filled matrix (ei)
>> command: ei <TEMP_DIR>/20251128-HHMMSS-export-all.xlsx
# OUTPUT: INFORMATION: Import complete: 16 total entries in XLS version 2

# 8. Check remaining empty count (pc xls -e)
# The result should be low (e.g., 4) because the final IT/FR keys are still missing translations.
>> command: pc xls -e
INFORMATION: The amount of properties type XLS is 4 [language=null, englishDefaultAmount=4, status=XLS].
```

#### 2.2. Targeted Export of Empty Values (Final XLS V3)

```bash
# 9. Targeted export of *only* empty values for a specific language (-l <LANG> -e)
>> command: ee <TEMP_DIR> -l fr -e
# This will export only the keys where the French translation is still missing (empty). The Excel file will contain only the open gaps for 'fr', making it easier for translators to focus on what needs to be done.
# [...] Excel file exported (only the empty French key-value pairs visible for translation).

# 10. External Edit (Simulation)
# The remaining 4 gaps are filled (e.g., foobar2/3 in IT/FR).

# 11. Import final completed Excel (ei)
>> command: ei <TEMP_DIR>/20251128-HHMMSS-export-all-empty.xlsx
# OUTPUT: INFORMATION: Import complete: 16 total entries in XLS version 3

# 12. Final check of empty values
>> command: pc xls -e
INFORMATION: The amount of properties type XLS is 0 [language=null, englishDefaultAmount=4, status=XLS].
```

-----

### 3. Promotion to Source Baseline & Final SUCCESS

The complete **XLS V3** matrix is promoted back to the Source structure, ensuring future consistency.

```bash
# 13. Export Properties (ep)
# This step writes the 16 fully translated entries back to the physical .properties files.
>> command: ep <PROJECT_ROOT>
INFORMATION: Export properties fileset into a directory [<PROJECT_ROOT>] in ms: [...]

# Optional: Delta Export (only changed files)
# If you only want to export files that have changed since the last SRC baseline import, use:
>> command: ep -d <PROJECT_ROOT>
INFORMATION: Delta export: Only files with modified keys are exported.
# This saves time and avoids unnecessary file changes—especially in large projects or CI/CD workflows.

# 14. Re-Import Source (ip)
# The tool now reads 16 entries from the physical files, creating the new DENSE SRC V3.
>> command: ip
INFORMATION: Iterate properties fileset and save into database in ms: [...]

# 15. Final Source Count Check
# The Source count must now equal the dense matrix size.
>> command: pc src
INFORMATION: The amount of properties type SRC is 16 [language=null, englishDefaultAmount=4, status=SRC].

# 16. Final Integrity Check (ci)
# SRC V3 (16) must equal XLS V3 (16).
>> command: ci
```

### 🏆 FINAL VERIFICATION OUTPUT

```text
>> command: ci
Nov. 28, 2025 5:40:00 PM de.aspera.locapp.cmd.CheckIntegrityCommand checkIntegrityProperties
INFORMATION: Check the integrity for language de and with src vs xls ...
INFORMATION: SUCCESS: LANGUAGE[de] The amount of src(4) vs xls(4) is equal. All src properties are provided by xls
Nov. 28, 2025 5:40:00 PM de.aspera.locapp.cmd.CheckIntegrityCommand checkIntegrityProperties
INFORMATION: Check the integrity for language en and with src vs xls ...
INFORMATION: SUCCESS: LANGUAGE[en] The amount of src(4) vs xls(4) is equal. All src properties are provided by xls
Nov. 28, 2025 5:40:00 PM de.aspera.locapp.cmd.CheckIntegrityCommand checkIntegrityProperties
INFORMATION: Check the integrity for language fr and with src vs xls ...
INFORMATION: SUCCESS: LANGUAGE[fr] The amount of src(4) vs xls(4) is equal. All src properties are provided by xls
Nov. 28, 2025 5:40:00 PM de.aspera.locapp.cmd.CheckIntegrityCommand checkIntegrityProperties
INFORMATION: Check the integrity for language it and with src vs xls ...
INFORMATION: SUCCESS: LANGUAGE[it] The amount of src(4) vs xls(4) is equal. All src properties are provided by xls

*** SUCCESS! The localization properties are complete! ***
```

---

**💡 Tip: Delta Export (`ep -d` / `--delta`)**

If you only want to export property files that have changed (instead of always exporting all), use `ep -d <DIR>`. This saves time and avoids unnecessary file changes—especially in large projects or CI/CD workflows. The tool automatically detects which files have changed since the last SRC baseline import and exports only those.

**Example:**
```bash
# Export only changed files
ep -d <PROJECT_ROOT>
```
Without `-d`, all files are always exported; with `-d`, only the actually changed ones are exported.
