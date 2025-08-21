package com.jackasher.ageiport.model.export;

/**
 * 文件路径结果类
 * @author Jackasher
 */
public class FilePaths {
    public String excelFileName;
    public String excelDirectory;
    public String outZipFileName;
    public String beforeDecodeZipFileName;
    
    public FilePaths(String excelFileName, String excelDirectory, String outZipFileName, String beforeDecodeZipFileName) {
        this.excelFileName = excelFileName;
        this.excelDirectory = excelDirectory;
        this.outZipFileName = outZipFileName;
        this.beforeDecodeZipFileName = beforeDecodeZipFileName;
    }
}
