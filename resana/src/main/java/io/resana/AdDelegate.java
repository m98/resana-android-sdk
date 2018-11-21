package io.resana;

public interface AdDelegate {
    void onPreparingProgram();

    void onPreparingProgramError();

    void onInstallingProgramError();
}
