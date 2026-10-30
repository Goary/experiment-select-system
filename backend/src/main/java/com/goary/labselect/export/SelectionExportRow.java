package com.goary.labselect.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SelectionExportRow {

    @ExcelProperty("工号/学号")
    private String employeeNo;

    @ExcelProperty("姓名")
    private String username;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("课程编号")
    private String courseCode;

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("教室")
    private String classroomName;

    @ExcelProperty("上课时间")
    private String timeRange;

    @ExcelProperty("选课时间")
    private String selectTime;
}
