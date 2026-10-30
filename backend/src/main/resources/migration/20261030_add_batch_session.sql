USE lab_select;

CREATE TABLE IF NOT EXISTS batch_session (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id    BIGINT   NOT NULL COMMENT '所属批次 id',
    session_no  INT      NOT NULL COMMENT '课次序号，从1开始',
    start_time  DATETIME NOT NULL COMMENT '本次上课开始时间',
    end_time    DATETIME NOT NULL COMMENT '本次上课结束时间',
    deleted     TINYINT  NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_session (batch_id, session_no),
    KEY idx_batch_time (batch_id, start_time, end_time),
    KEY idx_session_time (start_time, end_time)
) ENGINE = InnoDB COMMENT '批次课次安排';

INSERT INTO batch_session (batch_id, session_no, start_time, end_time)
SELECT b.id, 1, b.start_time, b.end_time
FROM course_batch b
WHERE b.start_time IS NOT NULL
  AND b.end_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM batch_session s WHERE s.batch_id = b.id);

ALTER TABLE course_batch DROP COLUMN start_time, DROP COLUMN end_time;
