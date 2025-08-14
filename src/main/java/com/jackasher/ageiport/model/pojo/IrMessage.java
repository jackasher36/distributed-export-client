package com.jackasher.ageiport.model.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName ir_message
 */
@TableName(value ="ir_message")
@Data
public class IrMessage {
    /**
     * 
     */
    @TableId
    private String uuid;

    /**
     * 
     */
    private Date createdTime;

    /**
     * 
     */
    private String deviceType;

    /**
     * 
     */
    private String deviceNumber;

    /**
     * 
     */
    private String archiveName;

    /**
     * 
     */
    private String bucketName;

    /**
     * 
     */
    private String areaNumber;

    /**
     * 
     */
    private String areaName;

    /**
     * 
     */
    private String fileName;

    /**
     * 
     */
    private Integer fileLength;

    /**
     * 
     */
    private String diePickingFileName;

    /**
     * 
     */
    private String ddcFileName;

    /**
     * 
     */
    private String demodulationFileName;

    /**
     * 
     */
    private String decodeFileName;

    /**
     * 
     */
    private String beforeDecodeFileName;

    /**
     * 
     */
    private String obtUlFileName;

    /**
     * 
     */
    private String obtDlFileName;

    /**
     * 
     */
    private String dataSourceType;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        IrMessage other = (IrMessage) that;
        return (this.getUuid() == null ? other.getUuid() == null : this.getUuid().equals(other.getUuid()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getDeviceType() == null ? other.getDeviceType() == null : this.getDeviceType().equals(other.getDeviceType()))
            && (this.getDeviceNumber() == null ? other.getDeviceNumber() == null : this.getDeviceNumber().equals(other.getDeviceNumber()))
            && (this.getArchiveName() == null ? other.getArchiveName() == null : this.getArchiveName().equals(other.getArchiveName()))
            && (this.getBucketName() == null ? other.getBucketName() == null : this.getBucketName().equals(other.getBucketName()))
            && (this.getAreaNumber() == null ? other.getAreaNumber() == null : this.getAreaNumber().equals(other.getAreaNumber()))
            && (this.getAreaName() == null ? other.getAreaName() == null : this.getAreaName().equals(other.getAreaName()))
            && (this.getFileName() == null ? other.getFileName() == null : this.getFileName().equals(other.getFileName()))
            && (this.getFileLength() == null ? other.getFileLength() == null : this.getFileLength().equals(other.getFileLength()))
            && (this.getDiePickingFileName() == null ? other.getDiePickingFileName() == null : this.getDiePickingFileName().equals(other.getDiePickingFileName()))
            && (this.getDdcFileName() == null ? other.getDdcFileName() == null : this.getDdcFileName().equals(other.getDdcFileName()))
            && (this.getDemodulationFileName() == null ? other.getDemodulationFileName() == null : this.getDemodulationFileName().equals(other.getDemodulationFileName()))
            && (this.getDecodeFileName() == null ? other.getDecodeFileName() == null : this.getDecodeFileName().equals(other.getDecodeFileName()))
            && (this.getBeforeDecodeFileName() == null ? other.getBeforeDecodeFileName() == null : this.getBeforeDecodeFileName().equals(other.getBeforeDecodeFileName()))
            && (this.getObtUlFileName() == null ? other.getObtUlFileName() == null : this.getObtUlFileName().equals(other.getObtUlFileName()))
            && (this.getObtDlFileName() == null ? other.getObtDlFileName() == null : this.getObtDlFileName().equals(other.getObtDlFileName()))
            && (this.getDataSourceType() == null ? other.getDataSourceType() == null : this.getDataSourceType().equals(other.getDataSourceType()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getUuid() == null) ? 0 : getUuid().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getDeviceType() == null) ? 0 : getDeviceType().hashCode());
        result = prime * result + ((getDeviceNumber() == null) ? 0 : getDeviceNumber().hashCode());
        result = prime * result + ((getArchiveName() == null) ? 0 : getArchiveName().hashCode());
        result = prime * result + ((getBucketName() == null) ? 0 : getBucketName().hashCode());
        result = prime * result + ((getAreaNumber() == null) ? 0 : getAreaNumber().hashCode());
        result = prime * result + ((getAreaName() == null) ? 0 : getAreaName().hashCode());
        result = prime * result + ((getFileName() == null) ? 0 : getFileName().hashCode());
        result = prime * result + ((getFileLength() == null) ? 0 : getFileLength().hashCode());
        result = prime * result + ((getDiePickingFileName() == null) ? 0 : getDiePickingFileName().hashCode());
        result = prime * result + ((getDdcFileName() == null) ? 0 : getDdcFileName().hashCode());
        result = prime * result + ((getDemodulationFileName() == null) ? 0 : getDemodulationFileName().hashCode());
        result = prime * result + ((getDecodeFileName() == null) ? 0 : getDecodeFileName().hashCode());
        result = prime * result + ((getBeforeDecodeFileName() == null) ? 0 : getBeforeDecodeFileName().hashCode());
        result = prime * result + ((getObtUlFileName() == null) ? 0 : getObtUlFileName().hashCode());
        result = prime * result + ((getObtDlFileName() == null) ? 0 : getObtDlFileName().hashCode());
        result = prime * result + ((getDataSourceType() == null) ? 0 : getDataSourceType().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", uuid=").append(uuid);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", deviceType=").append(deviceType);
        sb.append(", deviceNumber=").append(deviceNumber);
        sb.append(", archiveName=").append(archiveName);
        sb.append(", bucketName=").append(bucketName);
        sb.append(", areaNumber=").append(areaNumber);
        sb.append(", areaName=").append(areaName);
        sb.append(", fileName=").append(fileName);
        sb.append(", fileLength=").append(fileLength);
        sb.append(", diePickingFileName=").append(diePickingFileName);
        sb.append(", ddcFileName=").append(ddcFileName);
        sb.append(", demodulationFileName=").append(demodulationFileName);
        sb.append(", decodeFileName=").append(decodeFileName);
        sb.append(", beforeDecodeFileName=").append(beforeDecodeFileName);
        sb.append(", obtUlFileName=").append(obtUlFileName);
        sb.append(", obtDlFileName=").append(obtDlFileName);
        sb.append(", dataSourceType=").append(dataSourceType);
        sb.append("]");
        return sb.toString();
    }
}