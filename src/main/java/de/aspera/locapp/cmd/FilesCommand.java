package de.aspera.locapp.cmd;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import de.aspera.locapp.dao.ConfigDao;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.FileInfoDao;
import de.aspera.locapp.dto.FileInfo;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "files",
    aliases = {"f"},
    description = "Read recursive down for properties files and save fileinfo.",
    mixinStandardHelpOptions = true
)
public class FilesCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(FilesCommand.class.getName());

    @Parameters(index = "0", description = "Directory path to scan for properties files", arity = "0..1")
    private Path path;

    @Override
    public void run() {
        if (path != null) {
            listFiles(path.toString());
        } else {
            logger.warning("No path provided. Use: files <path>");
        }
    }

    private void listFiles(String pathStr) {
        if (StringUtils.isEmpty(pathStr) || !new File(pathStr).exists()) {
            logger.warning("No path found for command: (f)iles");
            return;
        }

        long start = System.currentTimeMillis();
        ConfigDao configFacade = new ConfigDao();
        
        String[] excludedPaths = new String[] { "" };
        try {
            excludedPaths = configFacade.getValue("Excluded_Paths");
        } catch (DatabaseException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        PropertyFileReader propertyFileReader = new PropertyFileReader(pathStr + (SystemUtils.IS_OS_WINDOWS ? "\\" : "/"),
                ".properties", excludedPaths);

        FileInfoDao fileFacade = new FileInfoDao();
        List<FileInfo> files = new ArrayList<>();
        try {
            fileFacade.removeAll();
            for (File file : propertyFileReader.getResultFileNames()) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(file.getName());
                fileInfo.setFullPath(file.getAbsolutePath());
                fileInfo.setRelativePath(
                        file.getAbsolutePath().replace(pathStr, SystemUtils.IS_OS_WINDOWS ? ".\\" : "./"));
                fileInfo.setSearchPath(pathStr);

                files.add(fileInfo);
            }
            fileFacade.saveFileInfos(files);
        } catch (DatabaseException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        long end = System.currentTimeMillis() - start;
        logger.log(Level.INFO, "List files and save into database in ms: {0}", end);
    }
    
    /**
     * Sets the path programmatically for testing or legacy support.
     */
    public void setPath(Path path) {
        this.path = path;
    }
}
