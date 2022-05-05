package codechicken.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ResourceUtil {
    public static InputStream getResourceFromJar(String resourcePath, Class<?> referenceClass) {
        URL classFile = referenceClass.getResource('/' + referenceClass.getName().replace('.', '/') + ".class");
        lookup:
        {
            if (classFile == null)
                break lookup;
            String file = classFile.getFile();
            int id = file.indexOf("!");
            if (!classFile.getProtocol().equals("jar") || id < 0)
                break lookup;
            //Loading from a jar
            try {
                URL resource = new URL("jar:" + file.substring(0, id) + "!" + resourcePath);
                return resource.openStream();
            } catch (IOException e) {
                System.err.println("Failed to load resource " + resourcePath + " from jar " + file.substring(0, id));
                e.printStackTrace();
            }
        }
        //Fallback logic
        System.out.println("Using fallback resource loading logic for " + resourcePath + " with reference to " + referenceClass.getName());
        return referenceClass.getResourceAsStream(resourcePath);
    }
}
