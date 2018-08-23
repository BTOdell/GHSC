package ghsc.net.update;

import com.google.gson.*;
import ghsc.gui.Application;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a public release on GitHub.
 */
public class Release implements Comparable<Release> {

    private static final String LATEST_RELEASE_API_URL = "https://api.github.com/repos/BTOdell/GHSC/releases/latest";

    /**
     * The version of this release.
     */
    public final Version version;

    /**
     * The version compatibility information for this release.
     */
    private final List<Version> versions;

    /**
     * Gets the URL to download this release.
     */
    public final String downloadURL;

    private Release(final Version version, final List<Version> versions, final String downloadURL) {
        this.version = version;
        this.versions = versions;
        this.downloadURL = downloadURL;
    }

    /**
     * Determines if the given version is a compatible version with the current running version.
     * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
     */
    boolean isRequired() {
        return this.test(this.version, v1 -> v1.hasFlag(Version.REQUIRED));
    }

    /**
     * Determines if the given version is a compatible version with the current running version.
     * @return <tt>true</tt> if the given version is compatible, otherwise <tt>false</tt>.
     */
    boolean isForced() {
        return this.test(this.version, v1 -> v1.hasFlag(Version.FORCED));
    }

    /**
     * Tests all versions between the current version and the given target version.
     * @param targetVersion The latest version.
     * @param predicate The filter to qualify versions.
     */
    private boolean test(final Version targetVersion, final Predicate<Version> predicate) {
        if (!this.versions.contains(targetVersion) ||
            !this.versions.contains(Application.VERSION)) {
            return false;
        }
        final Version min;
        final Version max;
        if (targetVersion.compareTo(Application.VERSION) < 0) {
            min = targetVersion;
            max = Application.VERSION;
        } else {
            min = Application.VERSION;
            max = targetVersion;
        }
        boolean withinMinMax = false;
        for (final Version c : this.versions) {
            if (withinMinMax) {
                if (max.equals(c)) {
                    break;
                }
            } else if (min.equals(c)) {
                withinMinMax = true;
            }
            if (withinMinMax) {
                if (predicate.test(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(final Release o) {
        return this.version.compareTo(o.version);
    }

    @Override
    public int hashCode() {
        return this.version.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Release &&
            this.version.equals(((Release) obj).version);
    }

    @Override
    public String toString() {
        return this.version.toString();
    }

    // region Static factory functions

    /**
     * Determines if the given target version is a compatible version with the current running version.
     * @return <tt>true</tt> if the given target version is compatible, otherwise <tt>false</tt>.
     */
    public static boolean isCompatible(@Nullable final Release release, final Version targetVersion) {
        return targetVersion.equals(Application.VERSION) ||
            (release != null && !release.test(targetVersion, v1 -> v1.hasFlag(Version.COMPATIBLE)));
    }

    /**
     * Retrieves the latest release version of the internet.
     */
    static Release getLatest() throws IOException {
        // Get latest release information
        final URL latestReleaseURL = new URL(LATEST_RELEASE_API_URL);
        final JsonObject rootObject = new JsonParser()
            .parse(new InputStreamReader(latestReleaseURL.openStream(), StandardCharsets.UTF_8))
            .getAsJsonObject();
        String versionURLString = null;
        String jarURLString = null;
        for (final JsonElement assetElement : rootObject.getAsJsonArray("assets")) {
            final JsonObject assetObject = assetElement.getAsJsonObject();
            final String assetName = assetObject.getAsJsonPrimitive("name").getAsString();
            final String assetDownloadURL = assetObject.getAsJsonPrimitive("browser_download_url").getAsString();
            if ("version.txt".equalsIgnoreCase(assetName)) {
                versionURLString = assetDownloadURL;
            } else if (assetName.endsWith(".jar")) {
                jarURLString = assetDownloadURL;
            }
        }
        if (versionURLString == null) {
            throw new IOException("Latest release JSON didn't contain version file URL.");
        }
        if (jarURLString == null) {
            throw new IOException("Latest release JSON didn't contain JAR file URL.");
        }
        // Parse version compatibility information from version file
        final List<Version> versions = new ArrayList<>();
        final URL versionURL = new URL(versionURLString);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(versionURL.openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            final Version v = Version.parse(line);
            if (v == null) {
                throw new IOException("Version parse error!");
            }
            versions.add(v);
        }
        versions.sort(null);
        // Return release object
        return new Release(versions.get(0), versions, jarURLString);
    }

    // endregion

}
