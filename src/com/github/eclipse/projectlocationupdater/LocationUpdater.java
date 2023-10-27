package com.github.eclipse.projectlocationupdater;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.eclipse.core.internal.localstore.ILocalStoreConstants;
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;

/**
 * A utility class to read/write the location of a project.
 *
 * "restriction" warnings are suppressed: we need do access Eclipse internal
 * constants.
 *
 * @author Max Gensthaler
 * @author Thomas Calmant
 */
@SuppressWarnings("restriction")
public abstract class LocationUpdater {
    /** URI prefix in location file. */
    private static final String URI_PREFIX = "URI//"; //$NON-NLS-1$

    /** URI prefix and file protocol prefix. */
    private static final String FILE_URI_PREFIX = URI_PREFIX + "file:"; //$NON-NLS-1$

    /** Constant path to the workspace projects locations storage. */
//    private static final IPath WORKSPACE_PROJECT_SETTINGS_RELPATH = new Path(".metadata/.plugins/org.eclipse.core.resources/.projects"); //$NON-NLS-1$
    private static final String WORKSPACE_PROJECT_SETTINGS_RELPATH = ".metadata/.plugins/org.eclipse.core.resources/.projects"; //$NON-NLS-1$

    /** <code>true</code> if we're running on Windows, else <code>false</code>. */
    private static final boolean OS_IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0; //$NON-NLS-1$  //$NON-NLS-2$

    private LocationUpdater() {
    	//
    }

	/**
     * Retrieves the path to the .location file of a project in its workspace.
	 *
     * @param project
	 *            Any project
	 * @return An IPath to its .location file
	 */
    private static Path getProjectLocationFile(final String workspaceLocation, final String projectName) {
		// Get the workspace root path
//        final IPath workspaceLocation = project.getWorkspace().getRoot().getLocation();

		// Forge the location file name
		return Paths.get(workspaceLocation, WORKSPACE_PROJECT_SETTINGS_RELPATH, projectName, ".location"); //$NON-NLS-1$
	}

	/**
     * Reads the content of a project location file.
	 *
     * @param projectLocationFile
	 *            Path to a project location file
	 * @return The value of the project location
	 * @throws IOException
	 *             Error reading the location file
	 */
    private static String readProjectLocation(final Path projectLocationFile) throws IOException {
		DataInputStream in = null;
		try {
			// Read the location file
            in = new DataInputStream(new FileInputStream(projectLocationFile.toFile()));

			// Ignore the begin chunk
			in.skipBytes(ILocalStoreConstants.BEGIN_CHUNK.length);

			// Parse the location
            String projectLocation = in.readUTF();
            if (projectLocation.startsWith(FILE_URI_PREFIX)) {
                projectLocation = projectLocation.substring(FILE_URI_PREFIX.length());
                if (OS_IS_WINDOWS && Pattern.compile("^/[a-zA-Z]:").matcher(projectLocation).find()) { //$NON-NLS-1$
					// remove trailing "/" from absolute path on windows
                    projectLocation = projectLocation.substring(1);
				}
			}

            return projectLocation;
		} finally {
			// Be nice
			if (in != null) {
				in.close();
			}
		}
	}

	/**
     * Reads the content of a project location file.
	 *
     * @param project
	 *            Project to locate
	 * @return The value of the project location
	 * @throws IOException
	 *             Error reading the location file
	 */
    public static String readProjectLocation(final String workspaceLocation, final String projectName) throws IOException {
        Path projectLocationFile = getProjectLocationFile(workspaceLocation, projectName);
        return readProjectLocation(projectLocationFile);
	}

	/**
     * Updates a substring of the location.
	 *
     * @param project
	 *            Project to be updated
     * @param previousPrefix
	 *            Prefix path to be replaced
     * @param newPrefix
	 *            Replacement path
	 * @throws IOException
	 *             Error reading or writing the project location file
	 */
    public static void updateLocationSubstring(final String workspaceLocation, final String projectName, String previousPrefix, String newPrefix) throws IOException {
		// Read the previous location
        Path projectLocationFile = getProjectLocationFile(workspaceLocation, projectName);
		final String previousLocation = readProjectLocation(projectLocationFile);

		// Normalize paths
		previousPrefix = previousPrefix.replace('\\', '/');
		newPrefix = newPrefix.replace('\\', '/');
		if (previousPrefix.endsWith("/") && !newPrefix.endsWith("/")) { //$NON-NLS-1$ //$NON-NLS-2$
			newPrefix = newPrefix + "/"; //$NON-NLS-1$
		}

		// Replace the substring
		final String newLocationStr = previousLocation.replace(previousPrefix, newPrefix);

        writeProjectLocation(projectLocationFile.toFile(), Paths.get(newLocationStr).toFile());
	}

	/**
     * Sets a project location by writing the content of a project location
     * file.
	 *
     * @param project
     *            The project to change the location of
     * @param newLocation
     *            The path of the new location
	 * @throws IOException
	 *             Error reading or writing the location file
	 */
    public static void writeProjectLocation(final String workspaceLocation, final String projectName, final File newLocation) throws IOException {
        File projectLocationFile = getProjectLocationFile(workspaceLocation, projectName).toFile();

        writeProjectLocation(projectLocationFile, newLocation);
    }

    private static void writeProjectLocation(final File projectLocationFile, final File newLocation) throws FileNotFoundException, IOException {
		// Keep existing data
		String[] referenceNames;

		// Read the existing file
		DataInputStream in = null;
		try {
            in = new DataInputStream(new FileInputStream(projectLocationFile));

			// Ignore the begin chunk
			in.skipBytes(ILocalStoreConstants.BEGIN_CHUNK.length);

			// Get the previous project location
			final String projectLocationStr = in.readUTF();
			assert projectLocationStr.startsWith(URI_PREFIX);

			// Store references
			final int numRefs = in.readInt();
			referenceNames = new String[numRefs];
			for (int i = 0; i < numRefs; i++) {
				referenceNames[i] = in.readUTF();
			}

			// Ignore the end chunk
			in.skipBytes(ILocalStoreConstants.END_CHUNK.length);
		} finally {
			// Always close the file
			if (in != null) {
				in.close();
			}
		}

        if (projectLocationFile.isHidden() && OS_IS_WINDOWS) {
            // On windows the locationFile might be hidden
            Runtime.getRuntime().exec("attrib -H \"" + projectLocationFile.getAbsolutePath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        }

		// Write the new content
		DataOutputStream out = null;
		try {
            out = new DataOutputStream(new FileOutputStream(projectLocationFile));

			// Write the begin chunk
			out.write(ILocalStoreConstants.BEGIN_CHUNK);

			// Write the new location
            out.writeUTF(URI_PREFIX + newLocation.toURI().toString());

			// Write references
			out.writeInt(referenceNames.length);
			for (final String referenceName : referenceNames) {
				out.writeUTF(referenceName);
			}

			// Write the end chunk
			out.write(ILocalStoreConstants.END_CHUNK);
			out.flush();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		String workspaceLocation = "";
		workspaceLocation = new File(workspaceLocation).getAbsolutePath();
		
//		String projectName = "acme-api";
//		writeProjectLocation(workspaceLocation, projectName, new File("D:\\frameworks\\pan", projectName));
		
		String oldWorkspaceLocation = args[0];
		System.out.println(oldWorkspaceLocation);
		for (int i = 1; i < args.length; i++) {
			try {
				String projectName = args[i];
				String oldLocation = readProjectLocation(workspaceLocation, projectName);
				System.out.println(projectName + "~|~" + oldLocation + " ~~ " + oldWorkspaceLocation + " -> " + workspaceLocation);
				updateLocationSubstring(workspaceLocation, projectName, oldWorkspaceLocation, workspaceLocation);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}