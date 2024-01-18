package codechicken.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import codechicken.core.launch.CodeChickenCorePlugin;
import com.google.common.base.Function;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.relauncher.FMLInjectionData;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

public class CCUpdateChecker
{
    public static void tick() {

    }

    public static void addUpdateMessage(String s) {

    }

    public static String mcVersion() {
        return (String) FMLInjectionData.data()[4];
    }

    public static void updateCheck(final String mod, final String version) {

    }

    public static void updateCheck(String mod) {

    }

    public static void updateCheck(String url, Function<String, Void> handler) {

    }
}
