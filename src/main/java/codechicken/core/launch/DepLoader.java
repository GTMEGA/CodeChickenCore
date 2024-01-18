package codechicken.core.launch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.LaunchClassLoader;
import sun.misc.URLClassPath;
import sun.net.util.URLUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * For autodownloading stuff.
 * This is really unoriginal, mostly ripped off FML, credits to cpw.
 */
public class DepLoader implements IFMLLoadingPlugin, IFMLCallHook {
    public interface IDownloadDisplay {
        void resetProgress(int sizeGuess);

        void setPokeThread(Thread currentThread);

        void updateProgress(int fullLength);

        boolean shouldStopIt();

        void updateProgressString(String string, Object... data);

        Object makeDialog();

        void showErrorDialog(String name, String url);
    }

    @SuppressWarnings("serial")
    public static class Downloader extends JOptionPane implements IDownloadDisplay {
        boolean stopIt;
        Thread pokeThread;

        private Box makeProgressPanel() {
            return null;
        }

        @Override
        public JDialog makeDialog() {
            return null;
        }

        protected void requestClose(String message) {

        }

        @Override
        public void updateProgressString(String progressUpdate, Object... data) {

        }

        @Override
        public void resetProgress(int sizeGuess) {
        }

        @Override
        public void updateProgress(int fullLength) {
        }

        @Override
        public void setPokeThread(Thread currentThread) {
            this.pokeThread = currentThread;
        }

        @Override
        public boolean shouldStopIt() {
            return stopIt;
        }

        @Override
        public void showErrorDialog(String name, String url) {

        }
    }

    public static class DummyDownloader implements IDownloadDisplay {
        @Override
        public void resetProgress(int sizeGuess) {
        }

        @Override
        public void setPokeThread(Thread currentThread) {
        }

        @Override
        public void updateProgress(int fullLength) {
        }

        @Override
        public boolean shouldStopIt() {
            return false;
        }

        @Override
        public void updateProgressString(String string, Object... data) {
        }

        @Override
        public Object makeDialog() {
            return null;
        }

        @Override
        public void showErrorDialog(String name, String url) {
        }
    }

    public static class VersionedFile
    {
        public final Pattern pattern;
        public final String filename;
        public final ComparableVersion version;
        public final String name;

        public VersionedFile(String filename, Pattern pattern) {
            this.pattern = pattern;
            this.filename = filename;
            Matcher m = pattern.matcher(filename);
            if(m.matches()) {
                name = m.group(1);
                version = new ComparableVersion(m.group(2));
            }
            else {
                name = null;
                version = null;
            }
        }

        public boolean matches() {
            return name != null;
        }
    }

    public static class Dependency
    {
        public String url;
        public VersionedFile file;

        public String existing;
        /**
         * Flag set to add this dep to the classpath immediately because it is required for a coremod.
         */
        public boolean coreLib;

        public Dependency(String url, VersionedFile file, boolean coreLib) {
            this.url = url;
            this.file = file;
            this.coreLib = coreLib;
        }
    }

    public static class DepLoadInst {
        public DepLoadInst() {

        }

        public void load() {

        }
    }

    public static void load() {

    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return getClass().getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public Void call() {
        load();

        return null;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
