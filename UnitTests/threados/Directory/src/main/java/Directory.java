import java.util.*;

public class Directory {
    private static final int MAX_CHARS = Constants.MAX_FILENAME_LENGTH;
    private static final int UNALLOCATED = -1;

    private int[] filenameSizes;
    private List<String> fileNames;

    public Directory(int maxInumber) {
        filenameSizes = new int[maxInumber];
        fileNames = new ArrayList<>(maxInumber);

        for (int i = 0; i < maxInumber; i++) {
            filenameSizes[i] = UNALLOCATED;
            fileNames.add("");
        }

        filenameSizes[0] = 1;
        fileNames.set(0, "/");
    }

    public String list() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filenameSizes.length; i++) {
            if (filenameSizes[i] != UNALLOCATED) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(fileNames.get(i));
            }
        }
        return sb.toString();
    }

    public List<FilenameLengthPair> listFormatted() {
        List<FilenameLengthPair> result = new ArrayList<>();
        for (int i = 0; i < filenameSizes.length; i++) {
            if (filenameSizes[i] != UNALLOCATED) {
                result.add(new FilenameLengthPair(fileNames.get(i), filenameSizes[i]));
            }
        }
        return result;
    }

    public void bytes2directory(byte[] data) {
        int offset = 0;

        for (int i = 0; i < filenameSizes.length; i++) {
            filenameSizes[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }

        for (int i = 0; i < filenameSizes.length; i++) {
            byte[] nameBytes = new byte[MAX_CHARS * 2];
            System.arraycopy(data, offset, nameBytes, 0, MAX_CHARS * 2);
            offset += MAX_CHARS * 2;

            String name = new String(nameBytes).trim();
            if (filenameSizes[i] == UNALLOCATED) {
                fileNames.set(i, "");
            } else {
                int actualLen = Math.max(0, Math.min(filenameSizes[i], name.length()));
                fileNames.set(i, name.substring(0, actualLen));
            }
        }
    }

    public byte[] directory2bytes() {
        int totalSize = filenameSizes.length * 4 + filenameSizes.length * MAX_CHARS * 2;
        byte[] data = new byte[totalSize];
        int offset = 0;

        for (int i = 0; i < filenameSizes.length; i++) {
            SysLib.int2bytes(filenameSizes[i], data, offset);
            offset += 4;
        }

        for (int i = 0; i < filenameSizes.length; i++) {
            byte[] nameBytes = new byte[MAX_CHARS * 2];
            String name = fileNames.get(i);
            byte[] src = name.getBytes();
            int len = Math.min(src.length, MAX_CHARS * 2);
            System.arraycopy(src, 0, nameBytes, 0, len);
            System.arraycopy(nameBytes, 0, data, offset, MAX_CHARS * 2);
            offset += MAX_CHARS * 2;
        }

        return data;
    }

    public short ialloc(String filename) {
        if (filename == null || filename.length() > MAX_CHARS) {
            return -1;
        }

        if (namei(filename) != -1) {
            return -1;
        }

        for (short i = 1; i < filenameSizes.length; i++) {
            if (filenameSizes[i] == UNALLOCATED) {
                filenameSizes[i] = filename.length();
                fileNames.set(i, filename);
                return i;
            }
        }

        return -1;
    }

    public boolean ifree(short iNodeIndex) {
        if (iNodeIndex < 0 || iNodeIndex >= filenameSizes.length) {
            return false;
        }
        if (filenameSizes[iNodeIndex] == UNALLOCATED) {
            return false;
        }

        filenameSizes[iNodeIndex] = UNALLOCATED;
        fileNames.set(iNodeIndex, "");
        return true;
    }

    public short namei(String filename) {
        if (filename == null) return -1;

        for (short i = 0; i < filenameSizes.length; i++) {
            if (filenameSizes[i] != UNALLOCATED && fileNames.get(i).equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    public static class FilenameLengthPair {
        public final String filename;
        public final int length;

        public FilenameLengthPair(String filename, int length) {
            this.filename = filename;
            this.length = length;
        }

        @Override
        public String toString() {
            return "FilenameLengthPair{filename='" + filename + "', length=" + length + "}";
        }
    }
}