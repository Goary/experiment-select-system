package com.goary.labselect.batch;

import com.goary.labselect.batch.entity.BatchSession;
import com.goary.labselect.batch.entity.CourseBatch;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BatchVO extends CourseBatch {

    private String classroomName;
    private String classroomLocation;
    private List<Long> teacherIds;
    private List<String> teacherNames;
    private List<BatchSession> sessions;
    private Integer remaining;
}
