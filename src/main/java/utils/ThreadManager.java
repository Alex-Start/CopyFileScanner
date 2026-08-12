package utils;

import common.ActionHelper;
import file.FileMetadata;
import file.IFileScanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import settings.SettingsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Analyze folders and create threads to scan for the devices (according to folders)
 */
public class ThreadManager {
    private static final Logger logger = LogManager.getLogger(ThreadManager.class);

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private final MapManagerHelper mapManager = new MapManagerHelper(MAP_SERIAL_DISK_CACHE);

    private static final Map<String, DeviceFolder> MAP_SERIAL_DISK_CACHE = new HashMap<>();

    private final SettingsManager settingsManager;

    public ThreadManager(String path, boolean isWriteOperation) {
        this(new String[]{path}, isWriteOperation, null);
    }

    public ThreadManager(String[] paths, boolean isWriteOperation) {
        this(paths, isWriteOperation, null);
    }

    public ThreadManager(String[] paths, boolean isWriteOperation, SettingsManager settingsManager) {
        if(settingsManager == null) {
            //default value for settings
            settingsManager = new SettingsManager(ActionHelper.Action.SCAN);
        }
        this.settingsManager = settingsManager;
        analyzePath(paths, isWriteOperation);
    }

    public Map<String, List<FileMetadata>> createDynamicThreads(IFileScanner fileScanner) {
        Map<String, List<FileMetadata>> filesByName = new ConcurrentHashMap<>();
        Collection<DeviceFolder> devices = getMapSerialDisk().values();

        ExecutorService executor = Executors.newFixedThreadPool(getCountPoolThreads());

        for (DeviceFolder device : devices) {
            List<List<String>> partitions = splitFolders(device.getFolders(), settingsManager.getThreadCount(device.getCountThreads()));
            logger.info("Scan device: {} with count partitions: {} ", device, partitions.size());
            for (List<String> partition : partitions) {
                executor.submit(() -> {
                    for (String folder : partition) {
                        fileScanner.scanFiles(folder, filesByName);
                    }
                    return null;
                });
            }
        }

        try {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Interrupt scan files", e);
        }

        return filesByName;
    }

    public static List<List<String>> splitFolders(List<String> folders, int parts) {
        if (parts <= 0) parts = 1;
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < parts; i++) result.add(new ArrayList<>());
        for (int i = 0; i < folders.size(); i++) {
            result.get(i % parts).add(folders.get(i));
        }
        return result;
    }

    public Map<String, List<FileMetadata>> oneThreadPerDevice(IFileScanner fileScanner) {
        Map<String, List<FileMetadata>> filesByName = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(getCountPoolThreads());

        // Step 1: Scan files and group by name
        for (Map.Entry<String, DeviceFolder> entry : getMapSerialDisk().entrySet()) {
            executorService.submit(() -> {
                        for (String path : entry.getValue().getFolders()) {
                            if (path.isEmpty()) {
                                continue;
                            }
                            logger.info("Scan files in {}", path);
                            executorService.submit(() -> fileScanner.scanFiles(path, filesByName));
                        }
                    }
            );
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Interrupt scan files", e);
        }

        return filesByName;
    }

    public int getCountPoolThreads() {
        return mapManager.getMap().size();
    }

    public int getCountThreads() {
        if (mapManager.getMap().isEmpty()) {
            return 0;
        }
        return mapManager.getMap().values().stream().findFirst().get().getCountThreads();
    }

    public Map<String, DeviceFolder> getMapSerialDisk() {
        return mapManager.getMap();
    }

    private void analyzePath(String[] paths, boolean isWriteOperation) {
        for(String path : paths) {
            analyzePath(path, isWriteOperation);
        }
    }

    private int analyzePath(String path, boolean isWriteOperation) {
        String firstSymbol = getFirstSymbol(path);

        DeviceFolder device = mapManager.getDeviceWithPathStartWith(firstSymbol);
        // if exists disk - add path and return count threads
        if (device != null) {
            return device.addPath(path).getCountThreads();
        }

        // HDDs prefer fewer threads to avoid disk thrashing
        byte res = (byte) Math.min(CORES, 1);

        String serial;
        try {
            serial = getVolumeSerialNumber(firstSymbol);
            if (mapManager.getMap().containsKey(serial)) {
                return mapManager.get(serial).addPath(path).getCountThreads();
            }
            mapManager.put(serial, path);
        } catch (IOException e) {
            // unknown serial: return for HDD
            mapManager.put(null, path).get(null).setMediaType(DeviceFolder.HDD).setCountThreads(res);
            return res;
        }
        if (isSSD(path)) {
            // SSDs handle parallel I/O better
            res = (byte) Math.min(CORES, isWriteOperation ? 4 : 6);
            mapManager.get(serial).setMediaType(DeviceFolder.SSD).setCountThreads(res);
            return res;
        }

        mapManager.get(serial).setMediaType(DeviceFolder.HDD).setCountThreads(res);
        return res;
    }

    private static boolean isSSD(String path) {
        if(path == null || path.isEmpty()) return false;
        String firstSymbol = getFirstSymbol(path);
        boolean res = false;
        File file = new File(path);
        try {
            Path rootPath = file.toPath().toRealPath().getRoot();
            FileStore store = Files.getFileStore(rootPath);
            String desc = store.toString().toLowerCase();
            res = desc.contains("nvme") || desc.contains("ssd");
        } catch (IOException e) {
            // assume HDD on failure
        }
        logger.info("ThreadsManager::isSSD: Is SSD {} disk = {}", firstSymbol, res);
        return res;
    }

    // to compare to find different disks to have the same HDD device
    private static String getVolumeSerialNumber(String driveLetter) throws IOException {
        if (FileUtils.isWindows()) {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"cmd.exe", "/c", "wmic logicaldisk where DeviceID=\"" + driveLetter + "\" get VolumeSerialNumber"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String value = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.contains("VolumeSerialNumber")) continue;
                value = line.trim();
                logger.info("DriveLetter: {}, Volume Serial Number: {}", driveLetter, line.trim());
            }

            return value;
        } else {
            Process process = new ProcessBuilder("bash", "-c", "lsblk -o NAME,MOUNTPOINT,SERIAL,MODEL").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            return line;
        }
    }

    private static String getFirstSymbol(String str) {
        //return new File(str).toPath().getRoot().toString().replace("\\", "").replace(":", "");
        return str.toUpperCase().substring(0, 2);
    }

    public static class DeviceFolder {
        public static final String SSD = "SSD";
        public static final String HDD = "HDD";
        private final String deviceId;
        private String mediaType; //SSD, HDD
        private int countThreads = 1;
        private final Set<String> folders = new HashSet<>();

        DeviceFolder(String deviceId) {
            this.deviceId = deviceId;
        }

        DeviceFolder setMediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        DeviceFolder addPath(String path) {
            if (path != null && !path.isEmpty()) {
                folders.add(path);
            }
            return this;
        }

        DeviceFolder setCountThreads(int count) {
            countThreads = count;
            return this;
        }

        int getCountThreads() {
            return countThreads;
        }

        List<String> getFolders() {
            return new ArrayList<>(folders);
        }

        @Override
        public DeviceFolder clone() {
            return new DeviceFolder(this.deviceId).setMediaType(this.mediaType).setCountThreads(this.countThreads);
        }

        DeviceFolder merge(DeviceFolder device) {
            if (this != device) {
                if (!device.folders.isEmpty()) {
                    this.folders.addAll(device.folders);
                }
            }
            return this;
        }

        @Override
        public String toString() {
            return "DeviceFolder{" +
                    "deviceId='" + deviceId + '\'' +
                    ", mediaType='" + mediaType + '\'' +
                    ", countThreads=" + countThreads +
                    ", folders=" + folders +
                    '}';
        }
    }

    private static class MapManagerHelper {
        private final Map<String, DeviceFolder> MAP_SERIAL_DISK = new HashMap<>();
        private final Map<String, DeviceFolder> MAP_SERIAL_DISK_CACHE;

        MapManagerHelper(Map<String, DeviceFolder> cache) {
            MAP_SERIAL_DISK_CACHE = cache;
        }

        Map<String, DeviceFolder> getMap() {
            return MAP_SERIAL_DISK;
        }

        MapManagerHelper put(String key, String path) {
            DeviceFolder deviceFolder;
            if (MAP_SERIAL_DISK.containsKey(key)) {
                deviceFolder = get(key);
            } else {
                deviceFolder = new DeviceFolder(key).addPath(path);
            }
            put(key, deviceFolder);
            return this;
        }

        MapManagerHelper put(String key, DeviceFolder device) {
            MAP_SERIAL_DISK.put(key, device);
            DeviceFolder devForMerge = MAP_SERIAL_DISK_CACHE.getOrDefault(key, device.clone());
            MAP_SERIAL_DISK_CACHE.put(key, devForMerge.merge(device));
            return this;
        }

        DeviceFolder get(String key) {
            return MAP_SERIAL_DISK.get(key);
        }

        DeviceFolder getDeviceWithPathStartWith(String firstSymbol) {
            DeviceFolder device = getMap().values().stream()
                    .filter(dev -> dev.getFolders().stream()
                            .anyMatch(f -> f.toUpperCase().startsWith(firstSymbol))
                    ).findFirst().orElse(null);
            if (device != null) {
                return device;
            }
            Map.Entry<String, DeviceFolder> map = MAP_SERIAL_DISK_CACHE.entrySet().stream()
                    .filter(entry -> entry.getValue().getFolders().stream()
                    .anyMatch(f -> f.toUpperCase().startsWith(firstSymbol))
                    ).findFirst().orElse(null);
            if (map != null) {
                put(map.getKey(), map.getValue().clone());
                return get(map.getKey());
            }
            return null;
        }
    }
}
