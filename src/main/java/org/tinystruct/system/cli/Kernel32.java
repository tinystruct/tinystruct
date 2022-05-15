package org.tinystruct.system.cli;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32 extends StdCallLibrary {
    int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 7;
    /**
     * The instance.
     */
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
    boolean SetConsoleMode(Pointer pointer, int mode);
    Pointer GetStdHandle(int i);
}
